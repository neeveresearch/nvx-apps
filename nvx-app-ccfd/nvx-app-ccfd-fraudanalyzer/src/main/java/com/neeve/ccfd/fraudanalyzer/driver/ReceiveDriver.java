package com.neeve.ccfd.fraudanalyzer.driver;

import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.messages.AuthorizationApprovedMessage;
import com.neeve.ccfd.messages.AuthorizationDeclinedMessage;
import com.neeve.cli.annotations.Command;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
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
    public final void handleAuthorizationApproved(AuthorizationApprovedMessage message) {
        receivedCount.increment();
        if (message.getFlowStartTs() > 0) {
            receiveLatencies.add(UtlTime.now() - message.getFlowStartTs());
        }
    }

    @EventHandler
    public final void handleAuthorizationDeclined(AuthorizationDeclinedMessage message) {
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
