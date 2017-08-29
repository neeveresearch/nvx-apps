/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import com.neeve.ads.messages.ClearVisitorsMessage;
import com.neeve.ads.messages.IVisitorLookupResponseMessage;
import com.neeve.ads.messages.NewVisitorMessage;
import com.neeve.ads.messages.VisitorLookupRequestMessage;
import com.neeve.ads.messages.VisitorLookupResponseMessage;
import com.neeve.ads.state.DMPRepository;
import com.neeve.ads.state.Visitor;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepEngineDescriptor;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.IAepBusManagerStats;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.root.RootConfig;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.server.app.annotations.AppStateFactoryAccessor;
import com.neeve.sma.MessageBusBinding;
import com.neeve.sma.MessageLatencyManager.MessagingDirection;
import com.neeve.sma.MessageLatencyManager.UpdateListener;
import com.neeve.sma.MessageView;
import com.neeve.sma.event.UnhandledMessageEvent;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;

/**
 * Represents DMP component of the ad serving system.
 */
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class DMPApplication {

    /**
     * Calculates ad serve latencies by hooking into the platform's messaging statistics. 
     */
    final private class ResponseLatencyCalculator implements UpdateListener {
        @Override
        public void onUpdate(MessageBusBinding binding, MessageView view, MessagingDirection direction) {
            if (direction == MessagingDirection.Outbound && view instanceof VisitorLookupResponseMessage) {
                VisitorLookupResponseMessage response = (VisitorLookupResponseMessage)view;
                // time that platform wrote the message to the wire
                long sendingWireTime = response.getPreWireTs();
                if (sendingWireTime != -1) {
                    long receiptWireTime = response.getPostWireTs();
                    if (receiptWireTime != -1) {
                        visitorLookupResponseLatencies.add(sendingWireTime - receiptWireTime);
                    }
                }

            }
        }
    }

    private AepMessageSender _messageSender;
    private AepEngine _aepEngine;

    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("dmp"));

    @AppStat
    private final Counter visitorLookupRequestCount = StatsFactory.createCounterStat("VisitorLookupRequest Received Count");

    @AppStat
    private final Counter visitorLookupResponseCount = StatsFactory.createCounterStat("VisitorLookupResponse Sent Count");

    @AppStat
    private final Counter visitorsTrackedCount = StatsFactory.createCounterStat("Visitors Tracked Count");

    @AppStat
    private final Latencies visitorProcessLatencies = StatsFactory.createLatencyStat("Visitor Data Processing Time");

    @AppStat
    private final Latencies visitorLookupResponseLatencies = StatsFactory.createLatencyStat("VisitorLookupResponse Latencies");

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public DMPRepository createState(MessageView view) {
                return DMPRepository.create();
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

    /**
     * Receives visitor lookup requests from Ad Exchange
     * Updates or creates visitor data and returns lookup result.
     */
    @EventHandler
    final public void onVisitorLookupRequest(VisitorLookupRequestMessage message, DMPRepository repository) {
        // update stats
        visitorLookupRequestCount.increment();

        // update state
        Visitor visitor = repository.getVisitors().get(message.getVisitorId());
        if (visitor == null) {
            visitor = Visitor.create();
            visitor.setVisitorId(message.getVisitorId());
            visitor.setCategories(message.getKeywords());
            repository.getVisitors().put(visitor.getVisitorId(), visitor);
            visitorsTrackedCount.increment();
        }

        // TODO next phase merge keywords to categories if new ones are encountered and track frequency

        // TODO next phase how to efficiently merge sorted String arrays?

        IVisitorLookupResponseMessage visitorLookupResponseMessage = VisitorLookupResponseMessage.create();
        visitorLookupResponseMessage.setAdStartTs(message.getAdStartTs());
        visitorLookupResponseMessage.setAdRequestIdFrom(message.getAdRequestIdUnsafe());
        visitorLookupResponseMessage.setVisitorIdFrom(message.getVisitorIdUnsafe());
        visitorLookupResponseMessage.setKeywordsFrom(visitor.getCategoriesIterator());
        visitorLookupResponseMessage.setUrlFrom(message.getUrlUnsafe());
        visitorLookupResponseMessage.setPostWireTs(message.getPostWireTs());

        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.VISITOR_LOOKUP_RESPONSES,
                                   visitorLookupResponseMessage);

        // update stats
        visitorLookupResponseCount.increment();
    }

    // campaign management
    /**
     * Adds new visitor directly to data management platform.
     */
    @EventHandler
    final public void onNewVisitorData(NewVisitorMessage message, DMPRepository repository) {
        Visitor newItem = Visitor.create();

        newItem.setVisitorId(message.getVisitorId());
        newItem.setGender(message.getGender());
        newItem.setYearOfBirth(message.getYearOfBirth());
        newItem.setCategories(message.getCategories());

        repository.getVisitors().put(newItem.getVisitorId(), newItem);
        visitorsTrackedCount.increment();
    }

    @EventHandler
    final public void onClearVisitors(ClearVisitorsMessage message, DMPRepository repository) {
        repository.getVisitors().clear();
        visitorsTrackedCount.increment();
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
