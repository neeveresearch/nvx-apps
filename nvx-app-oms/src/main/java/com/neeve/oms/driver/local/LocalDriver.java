/**
 * Copyright (c) 2017 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.oms.driver.local;

import com.neeve.aep.AepBusManager;
import com.neeve.aep.AepEngine;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.cli.annotations.Argument;
import com.neeve.cli.annotations.Command;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.Stats.LatencyManager;

/**
 * Controls the local message bus binding driver when running in local driver mode. 
 */
public class LocalDriver {

    private volatile LocalMessageBusBinding bus;

    @AppStat(name = "local-w2w")
    final public LatencyManager localW2W = new LatencyManager("oms-w2w", 1024 * 1024);

    @EventHandler
    public void onBusCreation(AepMessagingStartedEvent event) {
        for (AepBusManager manager : ((AepEngine)event.getSource()).getBusManagers()) {
            if (manager.getBusBinding() instanceof LocalMessageBusBinding) {
                bus = (LocalMessageBusBinding)manager.getBusBinding();
                bus.setW2WLatencyManager(localW2W);
            }
        }
    }

    @Command(name = "startLocalDriver", displayName = "Start Local Driver", description = "Starts sending new orders via the local driver.")
    final public void start(@Argument(position = 1, name = "count", displayName = "Order Count", defaultValue = "50000", description = "The number of orders to send") final int sendCount,
                            @Argument(position = 2, name = "rate", displayName = "Order Rate", defaultValue = "1000", description = "The rate at which to send orders") final int sendRate,
                            @Argument(position = 3, name = "useFix", displayName = "Use FIX", defaultValue = "false", description = "The rate at which to send orders") final boolean useFix) throws Exception {
        if (bus == null) {
            throw new IllegalStateException("Local Driver is either not available or not configured");
        }
        else {
            bus.startSender(sendCount, sendRate, useFix);
        }
    }

    @Command(name = "getSentCount", displayName = "Get Sent Count")
    final public long getSentCount() throws Exception {
        return bus.getTotalSent();
    }

    @Command(name = "getReceivedCount", displayName = "Get Received Count")
    final public long getReceivedCount() throws Exception {
        return bus.getTotalReceived();
    }
}
