/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import com.neeve.ads.messages.BidRequestMessage;
import com.neeve.ads.messages.BidResponseMessage;
import com.neeve.ads.messages.ClearCampaignsMessage;
import com.neeve.ads.messages.IBidResponseMessage;
import com.neeve.ads.messages.NewCampaignMessage;
import com.neeve.ads.messages.WinningBidNotificationMessage;
import com.neeve.ads.state.Campaign;
import com.neeve.ads.state.DSPRepository;
import com.neeve.ads.state.PendingCampaign;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepEngineDescriptor;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.IAepBusManagerStats;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.lang.XString;
import com.neeve.lang.XStringIterator;
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
import com.neeve.util.UtlTime;

/**
 * Represents DSP component of the ad serving system.
 */
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class DSPApplication {

    /**
     * Calculates ad serve latencies by hooking into the platform's messaging statistics. 
     */
    final private class ResponseLatencyCalculator implements UpdateListener {
        @Override
        public void onUpdate(MessageBusBinding binding, MessageView view, MessagingDirection direction) {
            if (direction == MessagingDirection.Outbound && view instanceof BidResponseMessage) {
                BidResponseMessage response = (BidResponseMessage)view;
                // time that platform wrote the message to the wire
                long sendingWireTime = response.getPreWireTs();
                if (sendingWireTime != -1) {
                    long receiptWireTime = response.getPostWireTs();
                    if (receiptWireTime != -1) {
                        bidResponseLatencies.add(sendingWireTime - receiptWireTime);
                    }
                }

            }
        }
    }

    private AepMessageSender _messageSender;
    private AepEngine _aepEngine;

    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("dsp"));

    @AppStat
    private final Counter bidRequestCount = StatsFactory.createCounterStat("BidRequest Received Count");

    @AppStat
    private final Counter bidResponseCount = StatsFactory.createCounterStat("BidResponse Sent Count");

    @AppStat
    private final Counter winningBidNotificationCount = StatsFactory.createCounterStat("WinningBidNotification Received Count");

    @AppStat
    private final Counter newCampaignsCount = StatsFactory.createCounterStat("New Campaigns Created");

    @AppStat
    private final Latencies campaignSearchLatencies = StatsFactory.createLatencyStat("Campaign Search Time");

    @AppStat
    private final Latencies bidResponseLatencies = StatsFactory.createLatencyStat("BidResponse Latencies");

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public DSPRepository createState(MessageView view) {
                return DSPRepository.create();
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

    // TODO next there should be scheduled timed event that searches for expired 
    // items in app state and makes needed updates

    // TODO how to best model currency

    /**
     * Receives bid requests from ad exchange, creates a bid and sends it to ad exchange
     */
    @EventHandler
    final public void onBidRequest(BidRequestMessage message, DSPRepository repository) {

        if (repository.getCampaigns() == null || repository.getCampaigns().count == 0) {
            throw new IllegalStateException("Campaigns not yet initialized.");
        }

        bidRequestCount.increment();

        long startTs = UtlTime.now();
        /*
         * Search for best bid in campaigns.
         * Simplified algorithm selects winner like this:
         * - if there is enough money left to make a bid
         * - if it is highest offered bid per impression
         * - if at least one category matches requested keywords 
         * - if offers are equal, more matching keywords, higher the score
         * - set bid value as second best bid + MIN_BID_INCREMENT
         * - when only one qualified campaign, set bid to MIN_BID_VALUE
         */
        Campaign bestBidCampaign = null;
        Campaign runnerUpBidCampaign = null;
        double bestBidValue = 0;
        int bestCatScore = 0;
        for (Campaign campaign : repository.getCampaigns().values()) {
            if (campaign.getBalance() < campaign.getBidPerImpression()) {
                continue;
            }
            int catScore = intersectionScore(campaign.getCategoriesIterator(), message.getKeywordsIterator());
            if (catScore == 0) {
                continue;
            }
            if (bestBidCampaign == null) {
                bestBidCampaign = campaign;
                runnerUpBidCampaign = campaign;
                bestCatScore = catScore;
            }

            if (campaign.getBidPerImpression() > bestBidCampaign.getBidPerImpression()
                    || campaign.getBidPerImpression() == bestBidCampaign.getBidPerImpression()
                    && catScore > bestCatScore) {
                runnerUpBidCampaign = bestBidCampaign;
                bestBidCampaign = campaign;
                bestCatScore = catScore;
            }

            // if winner is also the only available candidate, give it a discount
            if (runnerUpBidCampaign == bestBidCampaign) {
                bestBidValue = Constants.MIN_BID_VALUE;
            }
            else {
                bestBidValue = runnerUpBidCampaign.getBidPerImpression() + Constants.MIN_BID_INCREMENT;
            }
        }

        campaignSearchLatencies.add(UtlTime.now() - startTs);

        // send bid response
        IBidResponseMessage bidResponseMessage = BidResponseMessage.create();

        bidResponseMessage.setAdStartTs(message.getAdStartTs());
        bidResponseMessage.setAdRequestId(message.getAdRequestId());
        if (bestBidCampaign != null) {
            bidResponseMessage.setBidRequestIdFrom(message.getBidRequestIdUnsafe());
            bidResponseMessage.setBidValue(bestBidValue);
            bidResponseMessage.setBannerUrlFrom(bestBidCampaign.getUrlUnsafe());
            PendingCampaign pendingCampaign = PendingCampaign.create();
            pendingCampaign.setCampaignIdFrom(bestBidCampaign.getCampaignIdUnsafe());
            repository.getPendingCampaigns().put(message.getBidRequestId(), pendingCampaign);
        }
        bidResponseMessage.setPostWireTs(message.getPostWireTs());

        _messageSender.sendMessage(Constants.Buses.DEFAULT,
                                   Constants.Channels.BID_RESPONSES,
                                   bidResponseMessage);
        bidResponseCount.increment();

        // TODO next phase once balance is depleted, remove campaign from active pool and notify someone 
    }

    /**
     * Receives notification from ad exchange on the winning bid.
     */
    @EventHandler
    final public void onWinningBidNotification(WinningBidNotificationMessage message, DSPRepository repository) {
        winningBidNotificationCount.increment();

        PendingCampaign pendingCampaign = repository.getPendingCampaigns().get(message.getBidRequestId());
        if (pendingCampaign != null) {
            Campaign winningCampaign = repository.getCampaigns().get(pendingCampaign.getCampaignId());
            winningCampaign.setBalance(winningCampaign.getBalance() - message.getBidValue());
            repository.getPendingCampaigns().remove(message.getBidRequestId());
        }
        else {
            // TODO how do we best handle errors?
            throw new IllegalStateException("Winning campaign not found: " + message.getBidRequestId());
        }
    }

    // campaign management
    /**
     * Adds new campaign to active campaigns.
     */
    @EventHandler
    final public void onNewCampaign(NewCampaignMessage message, DSPRepository repository) {

        Campaign newCampaign = Campaign.create();
        newCampaign.setCampaignId(message.getCampaignId());
        newCampaign.setCategories(message.getCategories());
        newCampaign.setBalance(message.getBudget());
        newCampaign.setBidPerImpression(message.getBidPerImpression());
        newCampaign.setUrl(message.getUrl());
        repository.getCampaigns().put(newCampaign.getCampaignId(), newCampaign);
        newCampaignsCount.increment();
    }

    @EventHandler
    final public void onClearCampaigns(ClearCampaignsMessage message, DSPRepository repository) {
        repository.getCampaigns().clear();
        newCampaignsCount.reset();
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

    private final XString compA = XString.create(64, true, false);
    private final XString compB = XString.create(64, true, false);

    /**
     * Cardinality of intersection of two sorted string arrays.
     * <p>
     * Note this method is not thread safe and can only be called from a
     * message handler. 
     * 
     * @return Number of elements in intersection.
     */
    private final int intersectionScore(XStringIterator listA, XStringIterator listB) {
        int retVal = 0;

        if (!listA.hasNext() || !listB.hasNext()) {
            return 0;
        }

        listA.nextInto(compA);
        listB.nextInto(compB);
        while (true) {
            int compare = compA.compareTo(compB);
            if (compare < 0) {
                if (!listA.hasNext()) {
                    break;
                }
                else {
                    listA.nextInto(compA);
                }
            }
            else if (compare > 0) {
                if (!listA.hasNext()) {
                    break;
                }
                else {
                    listB.nextInto(compB);
                }
            }
            else {
                retVal++;
                if (!listA.hasNext() || !listB.hasNext()) {
                    break;
                }
                listA.nextInto(compA);
                listB.nextInto(compB);
            }
        }
        return retVal;
    }
}
