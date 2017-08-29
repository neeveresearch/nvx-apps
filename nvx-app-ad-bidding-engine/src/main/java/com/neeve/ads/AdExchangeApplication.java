/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import com.neeve.ads.messages.AdRequestMessage;
import com.neeve.ads.messages.AdResponseMessage;
import com.neeve.ads.messages.BidRequestMessage;
import com.neeve.ads.messages.BidResponseMessage;
import com.neeve.ads.messages.IAdResponseMessage;
import com.neeve.ads.messages.IBidRequestMessage;
import com.neeve.ads.messages.IVisitorLookupRequestMessage;
import com.neeve.ads.messages.IWinningBidNotificationMessage;
import com.neeve.ads.messages.VisitorLookupRequestMessage;
import com.neeve.ads.messages.VisitorLookupResponseMessage;
import com.neeve.ads.messages.WinningBidNotificationMessage;
import com.neeve.ads.state.AdExchangeRepository;
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
 * Represents Ad Exchange component of the ad serving system
 */
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class AdExchangeApplication {

    /**
     * Calculates ad serve latencies by hooking into the platform's messaging statistics. 
     */
    final private class ResponseLatencyCalculator implements UpdateListener {
        @Override
        public void onUpdate(MessageBusBinding binding, MessageView view, MessagingDirection direction) {
            if (direction == MessagingDirection.Outbound) {

                if (view instanceof AdResponseMessage) {
                    AdResponseMessage response = (AdResponseMessage)view;
                    // time that platform wrote the message to the wire
                    long sendingWireTime = response.getPreWireTs();
                    if (sendingWireTime != -1) {
                        Long adReceiptWireTime = requestReceiptWireTimes.remove(response.getAdRequestId());
                        if (adReceiptWireTime != null) {
                            adResponseLatencies.add(sendingWireTime - adReceiptWireTime.longValue());
                        }

                        long receiptWireTime = view.getPostWireTs();
                        if (receiptWireTime != -1) {
                            sspForwardingLatencies.add(sendingWireTime - receiptWireTime);
                        }
                    }
                }
                else if (view instanceof VisitorLookupRequestMessage) {
                    VisitorLookupRequestMessage request = (VisitorLookupRequestMessage)view;
                    // time that platform wrote the message to the wire
                    long sendingWireTime = request.getPreWireTs();
                    if (sendingWireTime != -1) {
                        long receiptWireTime = request.getPostWireTs();
                        if (receiptWireTime != -1) {
                            dmpForwardingLatencies.add(sendingWireTime - receiptWireTime);
                        }
                    }
                }
                else if (view instanceof BidRequestMessage) {
                    BidRequestMessage request = (BidRequestMessage)view;
                    // time that platform wrote the message to the wire
                    long sendingWireTime = request.getPreWireTs();
                    if (sendingWireTime != -1) {
                        long receiptWireTime = request.getPostWireTs();
                        if (receiptWireTime != -1) {
                            dspForwardingLatencies.add(sendingWireTime - receiptWireTime);
                        }
                    }
                }
            }
        }
    }

    private AepMessageSender _messageSender;
    private AepEngine _aepEngine;

    private final XLinkedHashMap<String, Long> requestReceiptWireTimes = new XLinkedHashMap<String, Long>().shared();

    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("ad-exchange"));

    @AppStat
    private final Counter adRequestCount = StatsFactory.createCounterStat("AdRequest Received Count");

    @AppStat
    private final Counter adResponseCount = StatsFactory.createCounterStat("AdResponse Sent Count");

    @AppStat
    private final Counter visitorLookupRequestCount = StatsFactory.createCounterStat("VisitorLookupRequest Sent Count");

    @AppStat
    private final Counter visitorLookupResponseCount = StatsFactory.createCounterStat("VisitorLookupResponse Received Count");

    @AppStat
    private final Counter bidRequestCount = StatsFactory.createCounterStat("BidRequest Sent Count");

    @AppStat
    private final Counter bidResponseCount = StatsFactory.createCounterStat("BidResponse Received Count");

    @AppStat
    private final Counter winningBidNotificationCount = StatsFactory.createCounterStat("WinningBidNotificationCount Sent Count");

    @AppStat
    private final Latencies adResponseLatencies = StatsFactory.createLatencyStat("AdResponse Latencies");

    @AppStat
    private final Latencies dmpForwardingLatencies = StatsFactory.createLatencyStat("DMP Forwarding Latencies");

    @AppStat
    private final Latencies dspForwardingLatencies = StatsFactory.createLatencyStat("DSP Forwarding Latencies");

    @AppStat
    private final Latencies sspForwardingLatencies = StatsFactory.createLatencyStat("SSP Forwarding Latencies");

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public AdExchangeRepository createState(MessageView view) {
                return AdExchangeRepository.create();
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
     * Receives ad requests from SSP
     */
    @EventHandler
    final public void onAdRequest(AdRequestMessage message, AdExchangeRepository repository) {

        adRequestCount.increment();

        // lookup visitor data on DMP
        IVisitorLookupRequestMessage visitorLookupRequestMessage = VisitorLookupRequestMessage.create();
        visitorLookupRequestMessage.setAdStartTs(message.getAdStartTs());
        visitorLookupRequestMessage.setVisitorIdFrom(message.getVisitorIdUnsafe());
        visitorLookupRequestMessage.setAdRequestIdFrom(message.getAdRequestIdUnsafe());
        visitorLookupRequestMessage.setKeywords(message.getKeywords());
        visitorLookupRequestMessage.setPostWireTs(message.getPostWireTs());
        //Save ad receipt time to calculate response time when AdResponse is written later...
        if (message.getPostWireTs() != -1) {
            requestReceiptWireTimes.put(message.getAdRequestId(), message.getPostWireTs());
        }

        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.VISITOR_LOOKUP_REQUESTS,
                                   visitorLookupRequestMessage);
        visitorLookupRequestCount.increment();

        // TODO next phase: For now DSP is doing all the work for us. If Ad Exchange is separated it 
        // should track bid state and use message injection to conclude the auction when time expires. 
    }

    /**
     * Receives response on visitor data lookup, continues to create bid request and sends it to DSP.
     */
    @EventHandler
    final public void onVisitorLookupResponse(VisitorLookupResponseMessage message, AdExchangeRepository repository) {

        visitorLookupResponseCount.increment();

        IBidRequestMessage bidRequestMessage = BidRequestMessage.create();
        bidRequestMessage.setAdStartTs(message.getAdStartTs());
        bidRequestMessage.setAdRequestId(message.getAdRequestId());
        bidRequestMessage.setBidRequestId(message.getAdRequestId());
        bidRequestMessage.setVisitorId(message.getVisitorId());
        bidRequestMessage.setKeywords(message.getKeywords());
        bidRequestMessage.setUrl(message.getUrl());
        bidRequestMessage.setPostWireTs(message.getPostWireTs());
        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.BID_REQUESTS,
                                   bidRequestMessage);
        bidRequestCount.increment();
    }

    /**
     * Receives bids from DSP and sends winning ad to SSP
     */
    @EventHandler
    final public void onBidResponse(BidResponseMessage message, AdExchangeRepository repository) {

        bidResponseCount.increment();

        // send ad response to SSP.
        // we always have a winning bid because DSP did all the work for us. We can just forward.
        IAdResponseMessage adResponseMessage = AdResponseMessage.create();
        adResponseMessage.setAdStartTs(message.getAdStartTs());
        adResponseMessage.setAdRequestId(message.getAdRequestId());
        adResponseMessage.setBannerUrl(message.getBannerUrl());
        adResponseMessage.setPostWireTs(message.getPostWireTs());
        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.AD_RESPONSES,
                                   adResponseMessage);
        adResponseCount.increment();

        if (message.hasBannerUrl() && !message.getBannerUrlUnsafe().isNull()) {
            // send win notification to DSP if there was a valid bid
            IWinningBidNotificationMessage winningBidNotificationMessage = WinningBidNotificationMessage.create();
            winningBidNotificationMessage.setAdStartTs(message.getAdStartTs());
            winningBidNotificationMessage.setBidRequestId(message.getBidRequestId());
            winningBidNotificationMessage.setBidValue(message.getBidValue());
            _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                       Constants.Channels.WIN_BID_NOTIFIY_REQUESTS,
                                       winningBidNotificationMessage);
            winningBidNotificationCount.increment();
        }
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
