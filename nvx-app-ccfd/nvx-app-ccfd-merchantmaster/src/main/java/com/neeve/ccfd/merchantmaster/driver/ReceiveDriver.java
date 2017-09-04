package com.neeve.ccfd.merchantmaster.driver;

import com.neeve.ccfd.messages.AuthorizationRequestMessage;

import com.neeve.aep.annotations.EventHandler;
import com.neeve.cli.annotations.Command;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.StatsFactory;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.util.UtlTime;

/**
 * A test driver app for the Application
 */
public class ReceiveDriver {
    @AppStat
    private final Counter receivedCount = StatsFactory.createCounterStat("ReceiveDriver Count");

    @AppStat(name = "ReceiveDriver Event Latency")
    private volatile Latencies receiveLatencies = StatsFactory.createLatencyStat("ReceiveDriver Event Latency");

    @EventHandler
    public final void handleAuthorizationRequest(AuthorizationRequestMessage message) {
        receivedCount.increment();
        if (message.getFlowStartTs() > 0) {
            receiveLatencies.add(UtlTime.now() - message.getFlowStartTs());
        }
    }

    @Command
    public long getNumReceived() {
        return receivedCount.getCount();
    }
}
