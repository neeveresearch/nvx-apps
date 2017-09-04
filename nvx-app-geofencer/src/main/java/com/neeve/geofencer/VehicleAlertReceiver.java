package com.neeve.geofencer;

import com.neeve.geofencer.messages.*;

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
public class VehicleAlertReceiver {
    @AppStat
    private final Counter receivedCount = StatsFactory.createCounterStat("Num Received");

    @AppStat(name = "VehicleAlertReceiver Event Latency")
    private volatile Latencies receiveLatencies;

    @EventHandler
    public final void onEvent(RouteViolationEvent event) {
        receivedCount.increment();
        if (event.getOriginTs() > 0) {
            receiveLatencies.add(UtlTime.now() - event.getOriginTs());
        }
    }

    @Command
    public long getNumReceived() {
        return receivedCount.getCount();
    }
}
