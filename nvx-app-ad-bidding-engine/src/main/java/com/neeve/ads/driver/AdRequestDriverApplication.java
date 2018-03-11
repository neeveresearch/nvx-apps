/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads.driver;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.neeve.ads.Constants;
import com.neeve.ads.common.GenderEnumeration;
import com.neeve.ads.messages.ClearCampaignsMessage;
import com.neeve.ads.messages.ClearVisitorsMessage;
import com.neeve.ads.messages.ClientAdRequestMessage;
import com.neeve.ads.messages.ClientAdResponseMessage;
import com.neeve.ads.messages.IClearCampaignsMessage;
import com.neeve.ads.messages.IClearVisitorsMessage;
import com.neeve.ads.messages.IClientAdRequestMessage;
import com.neeve.ads.messages.INewCampaignMessage;
import com.neeve.ads.messages.INewVisitorMessage;
import com.neeve.ads.messages.NewCampaignMessage;
import com.neeve.ads.messages.NewVisitorMessage;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepEngine.HAPolicy;
import com.neeve.aep.AepEngineDescriptor;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.cli.annotations.Option;
import com.neeve.lang.XString;
import com.neeve.root.RootConfig;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.sma.event.UnhandledMessageEvent;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlThread;
import com.neeve.util.UtlTime;

/**
 * A test driver app for the ad serving system.
 */
@AppHAPolicy(HAPolicy.StateReplication)
public class AdRequestDriverApplication {
    private volatile AepMessageSender messageSender;

    private final class SendRunner {
        private final AtomicBoolean running = new AtomicBoolean();
        private volatile boolean stopRequested = false;

        SendRunner() {
            running.set(false);
        }

        public final boolean isRunning() {
            return running.get();
        }

        public synchronized final void stop() throws InterruptedException {
            while (isRunning()) {
                stopRequested = true;
                wait();
            }
            stopRequested = false;
        }

        public void send(final int count, final int rate, boolean async) {
            if (isRunning()) {
                throw new IllegalStateException("Sender is already running");
            }
            if (async) {
                new Thread(new Runnable() {
                    public void run() {
                        UtlThread.setCPUAffinityMask(sendCpuAffinityMask);
                        send(count, rate, false);
                    }
                }, "Driver Send Thread").start();
            }
            else {
                if (!running.compareAndSet(false, true)) {
                    throw new IllegalStateException("Sender is already running");
                }
                try {
                    UtlGovernor governer = new UtlGovernor(rate);
                    int numSent = 0;
                    while (numSent++ < count && !stopRequested) {
                        governer.blockToNext();
                        IClientAdRequestMessage message = ClientAdRequestMessage.create();
                        String id = UUID.randomUUID().toString();
                        message.setAdStartTs(UtlTime.now());
                        message.setAdRequestId(id);
                        message.setVisitorIdFrom(random.nextInt(visitorIds.size()));
                        message.setUrl("https://" + id);

                        // TODO this will keep growing DMP as intended, but we can also use recurring visitor
                        message.setKeywords(new String[] { Constants.CONTENT_CATEGORIES[0] });
                        adRequestCount.increment();
                        messageSender.sendMessage(Constants.Buses.DEFAULT,
                                                  Constants.Channels.CLIENT_AD_REQUESTS,
                                                  message);
                    }
                }
                finally {
                    running.set(false);
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        }
    }

    @Configured(property = "driver.sendCount")
    private int sendCount;
    @Configured(property = "driver.sendRate")
    private int sendRate;
    @Configured(property = "driver.sendCpuAffinityMask", defaultValue = "0")
    private String sendCpuAffinityMask;
    @Configured(property = "driver.autoStart", defaultValue = "true")
    private boolean autoStart;

    @AppStat
    private final Counter adRequestCount = StatsFactory.createCounterStat("ClientAdRequest Sent Count");

    @AppStat
    private final Counter adsServedCount = StatsFactory.createCounterStat("ClientAdResponse Received Count");

    @AppStat
    private final Latencies adServeLatencies = StatsFactory.createLatencyStat("Ad Serve Time");

    final private Tracer tracer = RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("driver"));
    final private SendRunner sendRunner = new SendRunner();
    final private ArrayList<XString> visitorIds = new ArrayList<XString>();
    final private Random random = new Random();

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @AppInjectionPoint
    final public void prepEngineDescriptor(final AepEngineDescriptor engineDescriptor) throws Exception {
        tracer.log("Setting messaging fail policy", Level.INFO);
        engineDescriptor.setMessagingStartFailPolicy(AepEngine.MessagingStartFailPolicy.NeverFail);
        engineDescriptor.setMessageBusBindingFailPolicy(AepEngine.MessageBusBindingFailPolicy.Reconnect);
    }

    @EventHandler
    final public void onClientAdResponseMessage(ClientAdResponseMessage message) {
        adsServedCount.increment();
        // Calculate total processing time
        adServeLatencies.add(UtlTime.now() - message.getAdStartTs());
    }

    /**
     * Dead letter handler
     */
    @EventHandler
    public void onUnhandledMessage(UnhandledMessageEvent event) {
        // TODO provide more information on what message was not handled and where it comes from
        tracer.log("UNHANDLED MESSAGE ", Level.SEVERE);
        // Mark the event for auto acknowledgement, the 
        // engine will acknowledge it.
        event.setAutoAck(true);
    }

    @Command(displayName = "Send Ad Requests", description = "Drives Ad Request traffic")
    public final void sendAdRequests(@Option(shortForm = 'c', longForm = "count", defaultValue = "10000", description = "The rate at which to send requests") int count,
                                     @Option(shortForm = 'r', longForm = "rate", defaultValue = "1000", description = "The rate at which to send requests") int rate,
                                     @Option(shortForm = 'a', longForm = "async", defaultValue = "true", description = "Whether or not to spin up a background thread to do the sends") boolean async) {
        try {
            sendRunner.stop();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Couldn't stop current send in progress (interupted)");
        }
        if (visitorIds.isEmpty()) {
            throw new IllegalStateException("Can't run sender without first seeding visitor ids");
        }
        sendRunner.send(count, rate, async);
    }

    @Command(displayName = "Seed Campaigns", description = "Seeds campaigns with the Ad Exchange")
    public final void addCampaigns(@Option(shortForm = 'c', longForm = "count", defaultValue = "10", description = "The number of campaigns to send") int count,
                                   @Option(shortForm = 'r', longForm = "rate", defaultValue = "10000", description = "The rate at which to send in campaigns") int rate) {
        tracer.log("Adding Campaigns", Level.INFO);
        if (sendRunner.isRunning()) {
            throw new IllegalStateException("Can't add campaigns while ad sender is running!");
        }

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                INewCampaignMessage message = NewCampaignMessage.create();
                String campaignId = UUID.randomUUID().toString();
                message.setCampaignId(campaignId);
                message.setCategories(new String[] { Constants.CONTENT_CATEGORIES[0] });
                message.setBudget(1000000);
                message.setBidPerImpression(0.01 + random.nextDouble() * 0.5);
                message.setUrl("https://" + campaignId);
                messageSender.sendMessage(Constants.Buses.DEFAULT,
                                          Constants.Channels.MANAGE_CAMPAIGNS_REQUESTS,
                                          message);
            }
        });
    }

    @Command(displayName = "Clear Campaigns", description = "Deregisters campains with the Ad Exchange")
    public void clearCampaigns() {
        if (sendRunner.isRunning()) {
            throw new IllegalStateException("Can't clear campaigns while ad sender is running!");
        }
        IClearCampaignsMessage message = ClearCampaignsMessage.create();
        messageSender.sendMessage(Constants.Buses.DEFAULT,
                                  Constants.Channels.MANAGE_CAMPAIGNS_REQUESTS,
                                  message);
    }

    @Command(displayName = "Seed Visitors", description = "Seeds visitor tracking data with the Ad Exchange")
    public void addVisitors(@Option(shortForm = 'c', longForm = "count", defaultValue = "10", description = "The number of visitor details to add") int count,
                            @Option(shortForm = 'r', longForm = "rate", defaultValue = "10000", description = "The rate at which to send in visitor details") int rate) {

        tracer.log("Adding new Tracking Data", Level.INFO);
        if (sendRunner.isRunning()) {
            throw new IllegalStateException("Can't add visitors while ad sender is running!");
        }

        visitorIds.clear();

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                INewVisitorMessage message = NewVisitorMessage.create();
                XString id = XString.create(UUID.randomUUID().toString());
                message.setVisitorIdFrom(id);
                visitorIds.add(id);
                message.setGender(GenderEnumeration.Male);
                message.setYearOfBirth((short)2000);
                message.setCategories(new String[] { Constants.CONTENT_CATEGORIES[0] });
                messageSender.sendMessage(Constants.Buses.DEFAULT,
                                          Constants.Channels.MANAGE_VISITORS_REQUESTS,
                                          message);
            }
        });
    }

    @Command(displayName = "Clear Visitors", description = "Clears visitors")
    private void clearVisitors() {
        if (sendRunner.isRunning()) {
            throw new IllegalStateException("Can't clear visitors while ad sender is running!");
        }
        IClearVisitorsMessage message = ClearVisitorsMessage.create();
        messageSender.sendMessage(Constants.Buses.DEFAULT,
                                  Constants.Channels.MANAGE_VISITORS_REQUESTS,
                                  message);
    }

    @Command(displayName = "Get Ad Request Count", description = "Gets the number of ads requested")
    public long getAdRequestCount() {
        return adRequestCount.getCount();
    }

    @Command(displayName = "Get Ad Response Count", description = "Gets the number of ads received")
    public long getAdResponsesCount() {
        return adsServedCount.getCount();
    }

    @Command(displayName = "Stop Sending", description = "Halts Ad Serve Requests")
    public void stopAdSender() throws InterruptedException {
        sendRunner.stop();
    }
}
