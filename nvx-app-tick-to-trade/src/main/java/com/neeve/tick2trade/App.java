package com.neeve.tick2trade;

import java.util.Set;

import com.neeve.ci.*;
import com.neeve.rog.IRogMessage;
import com.neeve.root.*;
import com.neeve.toa.TopicOrientedApplication;
import com.neeve.toa.service.ToaService;
import com.neeve.toa.service.ToaServiceChannel;
import com.neeve.trace.*;
import com.neeve.trace.Tracer.Level;
import com.neeve.xbuf.*;
import com.neeve.aep.*;
import com.neeve.aep.AepEngine.HAPolicy;
import com.neeve.aep.annotations.*;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.server.app.annotations.*;
import com.neeve.sma.MessageBusBinding;
import com.neeve.sma.MessageChannel.Qos;
import com.neeve.sma.MessageLatencyManager.MessagingDirection;
import com.neeve.sma.MessageLatencyManager.UpdateListener;
import com.neeve.sma.MessageView;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.tick2trade.messages.*;

/**
 * The application entry point for the execution management system.
 * <p>
 * <h2>Overview</h2>
 * In all this application has 4 main components:
 * <ul>
 * <li>The EMS (execution management system)
 * <li>The SOR (smart order router)
 * <li>The Client (serves as a driver for this App simulating client orders)
 * <li>The Market (servers as a driver for this App by simulating an exchange)
 * </ul>
 * The EMS is responsible for tracking state related to new order requests from
 * clients and brokering the fulfillment of the order between the executing
 * trader (Client) and liquidity venues (Market).
 * <p>
 * Order routing is done by a Smart Order Router (SOR) which determines how a
 * particular order should be sliced between different liquidity venues for
 * fulfillment. In this application the SOR is embedded in the same process as
 * the {@link Ems} for the lowest possible latency, but it would also be
 * possible for the SOR to be located in another process.
 * <p>
 * The Client and Market apps in the driver package drive the trading flow and
 * allow capture of end to end stats. Because they capture end to end latency
 * they should be run on the same host.
 * <p>
 * <h2>Metrics of Interest</h2>
 * <ul>
 * <li><b>Tick To Trade (ttt)</b>: Tick To Trade measures the latency from when
 * the timestamp of the tick upon which the {@link Sor} makes a decision based
 * on market data to the time that the trade is sent on the wire to the
 * exchange.
 * <p>
 * This metric is important for trading algorithms in terms of indicating how
 * quickly the trading system to capitalize on an advertised price.
 * <p>
 * Because this application doesn't use real market data, TTT is approximated by
 * taking the time that the {@link Sor} creates its first slice to the time at
 * which the slice is written the wire (the time at which it is received by the
 * market can't be computed because the Ems and Market may be running on
 * different system which will have clock skew). A final note on TTT: the
 * platform does have plugins for market data providers that can provide
 * extremely low (e.g. single digit microsecond) latency from ticker plant to
 * process, but to maximize the portability of this application this has not
 * been incoroporated here.
 * <li><b>Time To First Slice (ttfs)</b>: Measures the time from when the trader
 * decided to make a trade to the time at which the first slice from the
 * {@link Sor} made it to the exchange.
 * <p>
 * TTFS, is important from the trader's perspective in terms of measuring how
 * quickly the EMS can route its order to an exchange.
 * <p>
 * In this application the TTFS is measured in 2 ways: In process and End To End.
 * <p>
 * The In Process TTFS is measured in this class as the time from when the packet
 * containing the client order is received by the EMS process to the time when
 * the first slice has been serialized and written to the market process.
 * <p>
 * The End to End TTFS can only be reliably measured by collocating the client
 * and market applications on the same host and computing the timestamp based
 * off of a timestamp tunneled through the application in the
 * {@link EMSNewOrderSingle}s' and {@link MarketNewOrderSingle}s' orderTs
 * fields.
 * <p>
 * Between In Process and End To End TTFS, In Process is a better measure of
 * application performance as it isn't skewed by network latency which can be
 * considered a constant.
 * </ul>
 */
@AppHAPolicy(HAPolicy.EventSourcing)
final public class App extends TopicOrientedApplication {
    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("ems"));

    /**
     * Hooks into platform latency statistics to calculate Time To First Slice
     * and Tick To Trade Latencies.
     */
    final private class TickToTradeCalculator implements UpdateListener {
        @Override
        public void onUpdate(MessageBusBinding binding, MessageView view, MessagingDirection direction) {
            if (direction == MessagingDirection.Outbound && view instanceof MarketNewOrderSingle) {
                MarketNewOrderSingle mnos = (MarketNewOrderSingle)view;
                // when the MarketNewOrderSingle is sent, the handler copies the post wire
                // timestamp of the EMSNewOrderSingle onto it which allows calculation of the
                // time spent in process before the slice is sent out:
                timeToFirstSliceLatencies.add(mnos.getPreWireTs() - mnos.getPostWireTs());
                // The tick2trade time is the time from the tick that triggered the MarketNewOrderSingle
                // to the time it was written to the wire:
                tickToTradeLatencies.add(mnos.getPreWireTs() - mnos.getTickTs());
            }
        }
    }

    static {
        // Enable framing during deserialization. This is an optimization that should be enabled for 
        // latency critical message types working with domains that copy information from inbound
        // messages to the domain state. 
        //
        // Note: In future releases, it will be possible to set the desync policy via configuration
        EMSNewOrderSingle.setDesyncPolicy(XbufDesyncPolicy.FrameFields);
    }

    // what messaging QOS to use for this application
    final private static Qos qos = Qos.valueOf(XRuntime.getValue("simulator.qos", "Guaranteed"));

    // the EMS and SOR 
    final private Ems ems = new Ems(this, tracer);
    final private Sor sor = new Sor(this, tracer);

    // stats (see class description above for details)
    @AppStat
    final Latencies tickToTradeLatencies = StatsFactory.createLatencyStat("In Proc Tick To Trade");
    @AppStat
    final Latencies timeToFirstSliceLatencies = StatsFactory.createLatencyStat("In Proc Time To First Slice");
    final TickToTradeCalculator tickToTradeListener = new TickToTradeCalculator();

    ////////////////////////////////////////////////////////////////////////////////
    // Runtime Configuration                                                      //
    //                                                                            //
    // Hornet's TopicOrientApplication class has many hooks to allow config       //
    // to be augmented programmatically at runtime.                               //
    //                                                                            //
    // See http://build.neeveresearch.com/hornet/javadoc/LATEST for a description //
    // of the below methods
    ////////////////////////////////////////////////////////////////////////////////

    @Override
    final public void onEngineDescriptorInjected(final AepEngineDescriptor engineDescriptor) throws Exception {
        engineDescriptor.setMessagingStartFailPolicy(AepEngine.MessagingStartFailPolicy.FailIfOneBindingFails);
        engineDescriptor.setMessageBusBindingFailPolicy(AepEngine.MessageBusBindingFailPolicy.Reconnect);
    }

    @Override
    public final Qos getChannelQos(final ToaService service, final ToaServiceChannel channel) {
        return qos;
    }

    @Override
    final protected void onAppInitialized() throws Exception {
        tracer.log("Parameters", Level.INFO);
        ems.configure();
        sor.configure();
    }

    @Override
    final protected void addHandlerContainers(Set<Object> containers) {
        containers.add(ems);
        containers.add(sor);
    }

    @Override
    final protected void addAppStatContainers(Set<Object> containers) {
        containers.add(ems);
    }

    /**
     * Send interceptor to dispatch SOR traffic in the current
     * transaction.
     * 
     * @param message The message.
     */
    final public void send(final IRogMessage message) {
        switch (message.getType()) {
            case MessageFactorySOR.ID_SORNewOrderSingle:
            case MessageFactoryEMS.ID_EMSSliceCommand:
                // for commands from Sor dispatch locally:
                getEngine().getEventDispatcher().dispatchToEventHandlers(message);
                message.dispose();
                break;
            default:
                sendMessage(message);
                break;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // EVENT & MESSAGE HANDLERS                                                  //
    //                                                                           //
    // Event handlers are called by the underlying applications AepEngine.       //
    //                                                                           //
    // NOTE: An Event Sourcing applicaton must be able to identically            //
    // recover its state and generate the *same* outbound messages via replay    //
    // of the its input events at a later time or on a different system.         //
    // Thus, for an application using Event Sourcing, it is crucial that the     //
    // app not make any changes to its state that are based on the local system  //
    // such as System.currentTimeMillis() or interacting with the file system.   //
    //                                                                           //
    // Event handlers are not called concurrently so synchronization is not      //
    // needed.                                                                   // 
    ///////////////////////////////////////////////////////////////////////////////

    @EventHandler
    public void onMessagingStarted(AepMessagingStartedEvent event) {
        // To capture tick to trade statistic  
        // ... this is only needed for performance measurement. 
        for (IAepBusManagerStats abms : getEngine().getStats().getBusManagerStats()) {
            if (abms.getLatencyManager() != null) {
                abms.getLatencyManager().setUpdateListener(tickToTradeListener);
            }
        }
    }

    final Ems getEms() {
        return ems;
    }
}
