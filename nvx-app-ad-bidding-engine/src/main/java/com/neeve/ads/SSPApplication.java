/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import com.neeve.ads.messages.AdRequestMessage;
import com.neeve.ads.messages.AdResponseMessage;
import com.neeve.ads.messages.ClientAdRequestMessage;
import com.neeve.ads.messages.ClientAdResponseMessage;
import com.neeve.ads.messages.IAdRequestMessage;
import com.neeve.ads.messages.IClientAdResponseMessage;
import com.neeve.ads.state.SSPRepository;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepEngineDescriptor;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.IAepBusManagerStats;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.lang.XLinkedHashMap;
import com.neeve.root.RootConfig;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.server.app.annotations.AppStateFactoryAccessor;
import com.neeve.sma.MessageBusBinding;
import com.neeve.sma.MessageView;
import com.neeve.sma.MessageLatencyManager.MessagingDirection;
import com.neeve.sma.MessageLatencyManager.UpdateListener;
import com.neeve.sma.event.UnhandledMessageEvent;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;

/**
 * Represents SSP component of the Ad Serving System 
 */
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class SSPApplication {

    /**
     * Calculates ad serve latencies by hooking into the platform's messaging statistics. 
     */
    final private class ResponseLatencyCalculator implements UpdateListener {
        @Override
        public void onUpdate(MessageBusBinding binding, MessageView view, MessagingDirection direction) {
            if (direction == MessagingDirection.Outbound) {
                if (view instanceof ClientAdResponseMessage) {
                    ClientAdResponseMessage response = (ClientAdResponseMessage)view;
                    // time that platform wrote the message to the wire
                    long sendingWireTime = response.getPreWireTs();
                    if (sendingWireTime != -1) {
                        Long receiptWireTime = requestReceiptWireTimes.remove(response.getAdRequestId());
                        if (receiptWireTime != null) {
                            adResponseLatencies.add(sendingWireTime - receiptWireTime);
                        }
                        long reponseReceiptTime = response.getPostWireTs();
                        if (reponseReceiptTime != -1) {
                            adResponseForwardingLatencies.add(sendingWireTime - reponseReceiptTime);
                        }
                    }
                }
                else if (view instanceof AdRequestMessage) {
                    AdRequestMessage request = (AdRequestMessage)view;
                    // time that platform wrote the message to the wire
                    long sendingWireTime = request.getPreWireTs();
                    if (sendingWireTime != -1) {
                        Long receiptWireTime = request.getPostWireTs();
                        if (receiptWireTime != null) {
                            adRequestForwardingLatencies.add(sendingWireTime - receiptWireTime);
                        }
                    }

                }
            }
        }
    }

    private final XLinkedHashMap<String, Long> requestReceiptWireTimes = new XLinkedHashMap<String, Long>().shared();

    private AepMessageSender _messageSender;
    private AepEngine _aepEngine;

    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("ssp"));

    @AppStat
    private final Counter clientAdRequestCount = StatsFactory.createCounterStat("ClientAdRequest Received Count");

    @AppStat
    private final Counter clientAdResponseCount = StatsFactory.createCounterStat("ClientAdResponse Sent Count");

    @AppStat
    private final Counter adRequestCount = StatsFactory.createCounterStat("AdRequest Sent Count");

    @AppStat
    private final Counter adResponseCount = StatsFactory.createCounterStat("AdResponse Received Count");

    @AppStat
    private final Latencies adResponseLatencies = StatsFactory.createLatencyStat("ClientAdResponse Latencies");

    @AppStat
    private final Latencies adRequestForwardingLatencies = StatsFactory.createLatencyStat("Request Forwarding Latencies");

    @AppStat
    private final Latencies adResponseForwardingLatencies = StatsFactory.createLatencyStat("Response Forwarding Latencies");

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public SSPRepository createState(MessageView view) {
                return SSPRepository.create();
            }
        };
    }

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    @AppInjectionPoint
    final public void setMessageSender(AepEngine aepEngine) {
        _aepEngine = aepEngine;
    }

    @AppInjectionPoint
    final public void prepEngineDescriptor(final AepEngineDescriptor engineDescriptor) throws Exception {
        tracer.log("Setting messaging fail policy", Level.INFO);
        engineDescriptor.setMessagingStartFailPolicy(AepEngine.MessagingStartFailPolicy.NeverFail);
        engineDescriptor.setMessageBusBindingFailPolicy(AepEngine.MessageBusBindingFailPolicy.Reconnect);
    }

    // TODO next phase SSP product should keep track of responses and calculate publisher earning and house rake.
    // it should maintain a collection of publisher accounts

    /**
     * Receive ad request from publisher and create ad request for ad exchange.
     */
    @EventHandler
    final public void onClientAdRequest(ClientAdRequestMessage message, SSPRepository repository) {

        clientAdRequestCount.increment();

        IAdRequestMessage adRequestMessage = AdRequestMessage.create();
        adRequestMessage.setAdStartTs(message.getAdStartTs());
        adRequestMessage.setAdRequestIdFrom(message.getAdRequestIdUnsafe());
        adRequestMessage.setVisitorIdFrom(message.getVisitorIdUnsafe());
        adRequestMessage.setKeywords(message.getKeywords());
        adRequestMessage.setUrlFrom(message.getUrlUnsafe());
        adRequestMessage.setPostWireTs(message.getPostWireTs());
        //Save ad receipt time to calculate response time when AdResponse is written later...
        if (message.getPostWireTs() != -1) {
            requestReceiptWireTimes.put(message.getAdRequestId(), message.getPostWireTs());
        }
        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.AD_REQUESTS,
                                   adRequestMessage);
        adRequestCount.increment();
    }

    /**
     * Process ad response from ad exchange, pass the ad to publisher.
     */
    @EventHandler
    final public void onAdResponseMessage(AdResponseMessage message, SSPRepository repository) {

        adResponseCount.increment();

        IClientAdResponseMessage clientAdResponseMessage = ClientAdResponseMessage.create();
        clientAdResponseMessage.setAdStartTs(message.getAdStartTs());
        clientAdResponseMessage.setAdRequestIdFrom(message.getAdRequestIdUnsafe());
        clientAdResponseMessage.setBannerUrlFrom(message.getBannerUrlUnsafe());
        clientAdResponseMessage.setPostWireTs(message.getPostWireTs());
        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.CLIENT_AD_RESPONSES,
                                   clientAdResponseMessage);
        clientAdResponseCount.increment();
    }

    /**
     * Dead letter handler
     */
    @EventHandler
    public void onUnhandledMessage(UnhandledMessageEvent event) {
        tracer.log("UNHANDLED MESSAGE", Level.SEVERE);
        // Mark the event for auto acknowledgement, the 
        // engine will acknowledge it.
        event.setAutoAck(true);
    }

    /**
     * Hook into message startup to add a platform statistics update listener.
     */
    @EventHandler
    public void onMessagingStarted(AepMessagingStartedEvent event) {
        // Add latency response calculator
        ResponseLatencyCalculator latencyCalculator = new ResponseLatencyCalculator();
        for (IAepBusManagerStats abms : _aepEngine.getStats().getBusManagerStats()) {
            if (abms.getLatencyManager() != null) {
                abms.getLatencyManager().setUpdateListener(latencyCalculator);
            }
        }
    }
}
