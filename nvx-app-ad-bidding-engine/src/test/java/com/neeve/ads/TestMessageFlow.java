/**
 * Copyright (c) 2017 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import com.neeve.ads.driver.AdRequestDriverApplication;

/**
 * Test end to end message flow. 
 */
public class TestMessageFlow extends AbstractTest {

    @Test
    public void testMessageFlow() throws Throwable {
        Properties env = new Properties();
        env.put("nv.ddl.profiles", "test");

        startApp(AdExchangeApplication.class, "ad-exchange", "ad-exchange-1", env);
        startApp(DMPApplication.class, "dmp", "dmp-1", env);
        startApp(DSPApplication.class, "dsp", "dsp-1", env);
        startApp(SSPApplication.class, "ssp", "ssp-1", env);
        AdRequestDriverApplication driverApp = startApp(AdRequestDriverApplication.class, "driver", "driver-1", env);

        // sleep to let all connections be established.
        Thread.sleep(5000);

        driverApp.addCampaigns(100, 100);
        driverApp.addVisitors(100, 100);

        int adSendCount = 1000;
        driverApp.sendAdRequests(adSendCount, 100, false);

        // poll for ad response received
        long timeout = System.currentTimeMillis() + 60000;
        while (driverApp.getAdResponsesCount() < adSendCount && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // sleep to check for any extra messages
        Thread.sleep(1000);

        assertEquals("Wrong number of client ads requested", adSendCount, driverApp.getAdRequestCount());
        assertEquals("Wrong number of client ads served", adSendCount, driverApp.getAdResponsesCount());
    }
}
