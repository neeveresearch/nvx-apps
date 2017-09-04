package com.neeve.tick2trade.driver;

import java.io.File;
import java.net.URL;
import java.util.Random;
import java.util.Set;

import com.neeve.aep.AepBusManager;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepEngine.HAPolicy;
import com.neeve.aep.AepEngineDescriptor;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepChannelUpEvent;
import com.neeve.ci.XRuntime;
import com.neeve.cli.annotations.Argument;
import com.neeve.cli.annotations.Command;
import com.neeve.root.RootConfig;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.server.app.annotations.AppVersion;
import com.neeve.sma.MessageChannel;
import com.neeve.sma.MessageChannel.Qos;
import com.neeve.sma.SmaException;
import com.neeve.tick2trade.acl.EMSNewOrderSinglePopulator;
import com.neeve.tick2trade.messages.EMSNewOrderSingle;
import com.neeve.toa.DefaultServiceDefinitionLocator;
import com.neeve.toa.TopicOrientedApplication;
import com.neeve.toa.service.ToaService;
import com.neeve.toa.service.ToaServiceChannel;
import com.neeve.toa.spi.AbstractServiceDefinitionLocator;
import com.neeve.toa.spi.ServiceDefinitionLocator;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;
import com.neeve.util.UtlThread;
import com.neeve.util.UtlThrowable;
import com.neeve.util.UtlTime;

@AppVersion(1)
@AppHAPolicy(HAPolicy.EventSourcing)
final public class Client extends TopicOrientedApplication {

    /**
     * Locates the services for this application. 
     * 
     * The client only needs the EMS service (it doesn't talk to market directly)
     */
    private final class ServiceLoader extends AbstractServiceDefinitionLocator {

        @Override
        public void locateServices(Set<URL> urls) throws Exception {
            urls.add(new File(XRuntime.getRootDirectory(), "conf/services/emsService.xml").toURI().toURL());
        }
    }

    final private static Qos qos = Qos.valueOf(XRuntime.getValue("simulator.qos", "Guaranteed"));
    final public static long senderAffinity = UtlThread.parseAffinityMask(XRuntime.getValue("simulator.client.sendAffinity", "0"));
    final public int sendCount = XRuntime.getValue("simulator.sendCount", 0);
    final public int sendRate = XRuntime.getValue("simulator.sendRate", 1000);

    // private members
    final private static Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("client"));
    private int orderId = 1;
    private MessageChannel ordersChannel;

    // stats
    @AppStat(name = "Client Orders Sent")
    private volatile long ordersSent;
    @AppStat(name = "Client Trades Received")
    private volatile long rcvdTradeCount;
    @AppStat(name = "Client OrderNews Received")
    private volatile long rcvdOrderNew;
    @AppStat(name = "Client Messages Received")
    private volatile long rcvdMessageCount;

    ///////////////////////////////////////////////////////////////////////////////
    // Runtime Configuration                                                     //
    //                                                                           //
    // Hornet's TopicOrientApplication class has many hooks to allow config      //
    // to be augmented programmatically at runtime.                              //
    ///////////////////////////////////////////////////////////////////////////////

    @Override
    final public void onEngineDescriptorInjected(final AepEngineDescriptor engineDescriptor) throws Exception {
        tracer.log("[Client] Engine Descriptor injected", Level.INFO);
        // allow starting market before ems is up ... will cause it to retry the bus connnection:
        engineDescriptor.setMessagingStartFailPolicy(AepEngine.MessagingStartFailPolicy.NeverFail);
        engineDescriptor.setMessageBusBindingFailPolicy(AepEngine.MessageBusBindingFailPolicy.Reconnect);
        _tracer.log("[Client] Removing the default client bus...", Level.INFO);
        engineDescriptor.removeBus("client");
        engineDescriptor.clearBusManagerProperties("client");
    }

    /**
     * We use a custom service definition locator so that we can switch between
     * multiple and single bus configurations.
     * <p>
     * This is not typically necessary ... applications usually package their
     * service definitions on the classpath and use the
     * {@link DefaultServiceDefinitionLocator}.
     */
    @Override
    public ServiceDefinitionLocator getServiceDefinitionLocator() {
        return new ServiceLoader();
    }

    /**
     * Progagates configured channel Qos. 
     */
    @Override
    public final Qos getChannelQos(final ToaService service, final ToaServiceChannel channel) {
        return qos;
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
    final public void onOrderNew(final com.neeve.tick2trade.messages.EMSOrderNew message) {
        rcvdOrderNew++;
        rcvdMessageCount++;
    }

    @EventHandler
    final public void onTrade(final com.neeve.tick2trade.messages.EMSTrade message) {
        rcvdTradeCount++;
        rcvdMessageCount++;
    }

    @EventHandler(source = "orders@ems")
    final public void onOrdersChannelUp(AepChannelUpEvent event) {
        synchronized (this) {
            // wait for order channel to come up before sending
            ordersChannel = event.getMessageChannel();
            notifyAll();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // App Main                                                                  //
    //                                                                           //
    // The application main entry point.                                         //
    ///////////////////////////////////////////////////////////////////////////////

    @AppMain
    final public void appMain(String[] args) throws Exception {
        UtlThread.setCPUAffinityMask(senderAffinity);

        tracer.log("[Client] ...sendCount=" + sendCount, Level.INFO);
        tracer.log("[Client] ...sendRate=" + sendRate, Level.INFO);

        // wait for the sending channel to come up:
        synchronized (this) {
            while (ordersChannel == null) {
                System.out.println("Waiting for orders channel to come up...");
                wait();
            }
        }

        try {
            // send NOS
            tracer.log("[Client] Sending " + sendCount + " orders with target rate of " + sendRate + " per second...", Level.INFO);
            final Random random = new java.util.Random(System.currentTimeMillis());

            int i = 0;
            final long nanosPerMsg = sendRate > 0 ? (1000000000l / sendRate) : 0;
            long next = System.nanoTime() + nanosPerMsg;
            while (i < sendCount) {
                if (System.nanoTime() >= next) {
                    // create and populate a new order
                    EMSNewOrderSingle nos = EMSNewOrderSinglePopulator.populate(EMSNewOrderSingle.create(), orderId++, random);

                    // send and transact time
                    final long nowMS = System.currentTimeMillis();
                    nos.setTransactTimeAsTimestamp(nowMS);
                    nos.setSendingTimeAsTimestamp(nowMS);

                    // orderTs
                    final long now = UtlTime.now();
                    nos.setOrderTs(now);

                    // compliance id
                    nos.setComplianceIDFrom(now);

                    sendMessage(nos);
                    ordersSent++;
                    next += nanosPerMsg;
                    i++;
                }
            }
            flush();
            tracer.log("[Client] sent " + sendCount + " orders.", Level.INFO);
        }
        catch (Exception e) {
            tracer.log("[Client] Error sending orders: " + e.getLocalizedMessage() + " - " + UtlThrowable.prepareStackTrace(e), Level.SEVERE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////
    // COMMAND HANDLERS                                                          //
    //                                                                           //
    // Command handlers can be invoked remotely via management tools such as     //
    // Robin or locally via unit tests.                                          //
    ///////////////////////////////////////////////////////////////////////////////

    @Command(description = "Waits for trades and returns the number received.")
    final public long waitForTrades(@Argument(name = "seconds", position = 1, required = false, defaultValue = "30", description = "The number of seconds to wait.") long seconds) throws Exception {
        long timeout = System.currentTimeMillis() + seconds * 1000;
        while (rcvdTradeCount < sendCount && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }
        return rcvdTradeCount;
    }

    /**
     * Flushes all message bus binding.
     * <p>
     * This method allows flushing messages that have been sent during warmup to
     * ensure that they are reflected in post warmup results.
     */
    final private void flush() {
        for (AepBusManager busManager : getEngine().getBusManagers()) {
            try {
                busManager.getBusBinding().flush(null);
            }
            catch (SmaException e) {
                e.printStackTrace();
            }
        }
    }
}
