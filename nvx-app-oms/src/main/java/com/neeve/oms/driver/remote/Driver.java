package com.neeve.oms.driver.remote;

import java.util.concurrent.atomic.AtomicBoolean;

import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.rog.IRogMessage;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.StatsFactory;
import com.neeve.stats.Stats;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlTime;

import com.neeve.fix.*;

import com.neeve.oms.driver.NewOrderMessagePopulator;
import com.neeve.oms.messages.NewOrderMessage;
import com.neeve.oms.messages.OrderEvent;

/**
 * A driver app for the Application.
 */
public class Driver {
    @Configured(property = "oms.driver.sendCount")
    private int sendCount;
    @Configured(property = "oms.driver.sendRate")
    private int sendRate;
    @Configured(property = "oms.driver.sendAffinity")
    private String sendThreadAffinity;
    @Configured(property = "oms.driver.autoStart", defaultValue = "true")
    private boolean autoStart;
    @Configured(property = "oms.driver.useFix", defaultValue = "true")
    private boolean useFix;
    @AppStat
    final private Counter sentCount = StatsFactory.createCounterStat("NumSent");
    @AppStat
    final private Counter receivedCount = StatsFactory.createCounterStat("NumReceived");
    @AppStat(name = "c2m")
    final private Stats.LatencyManager c2m = new Stats.LatencyManager("c2m");
    private volatile AepMessageSender messageSender;
    private AtomicBoolean running = new AtomicBoolean(false);
    private long sendTs;
    private AtomicBoolean receivedResponse = new AtomicBoolean(false);
    private int complete;

    final private IRogMessage createNewOrderMessage(final boolean useFix) {
        if (useFix) {
            final FixMessage message = FixMessage.create();
            NewOrderMessagePopulator.populate(message);
            return message;
        }
        else {
            final NewOrderMessage message = NewOrderMessage.create();
            NewOrderMessagePopulator.populate(message);
            return message;
        }
    }

    final private void onReceive() {
        final long receiveTs = UtlTime.now();
        final long sendTs = this.sendTs;
        receivedResponse.set(true);
        receivedCount.increment();
        final long latency = receiveTs - sendTs;
        c2m.add(latency);
        if (++complete % 10000 == 0) {
            c2m.compute();
            StringBuilder sb = new StringBuilder();
            c2m.get(sb);
            System.out.print(sb.toString());
        }
    }

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @EventHandler
    public final void onOrderEvent(OrderEvent event) {
        onReceive();
    }

    @EventHandler
    public final void onFixMessage(FixMessage message) {
        onReceive();
    }

    @Command(name = "start")
    final public void start(final int count, final int rate, final boolean useFix) {
        if (running.compareAndSet(false, true)) {
            new Thread() {
                @Override
                final public void run() {
                    try {
                        UtlGovernor.run(count, rate, new Runnable() {
                            @Override
                            public void run() {
                                if (!running.get()) {
                                    throw new RuntimeException("interrupted");
                                }
                                final IRogMessage message = createNewOrderMessage(useFix);
                                if (message instanceof FixMessage) {
                                    NewOrderMessagePopulator.populate((FixMessage)message);
                                }
                                else {
                                    NewOrderMessagePopulator.populate((NewOrderMessage)message);
                                }
                                sendTs = UtlTime.now();
                                receivedResponse.set(false);
                                messageSender.sendMessage(1, message);
                                sentCount.increment();
                                while (!receivedResponse.get())
                                    ;
                            }
                        });
                    }
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    finally {
                        running.set(false);
                    }
                }
            }.start();
        }
    }

    @Command(name = "getSentCount", displayName = "Get Sent Count")
    final public long getSentCount() throws Exception {
        return sentCount.getCount();
    }

    @Command(name = "getReceivedCount", displayName = "Get Received Count")
    final public long getReceivedCount() throws Exception {
        return receivedCount.getCount();
    }

    @Command(name = "stop")
    final public void stop() throws Exception {
        running.set(false);
    }

    @AppMain
    final public void main(final String args[]) throws Exception {
        if (autoStart) {
            start(sendCount, sendRate, useFix);
        }
    }
}
