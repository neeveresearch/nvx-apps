package com.neeve.oms;
/**
 * Copyright (c) 2017 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import com.neeve.oms.driver.remote.Driver;
import com.neeve.oms.es.Application;

/**
 * Test end to end message flow. 
 */
public class TestMessageFlow extends AbstractTest {

    @Test
    public void testMessageFlow() throws Throwable {
        Properties env = new Properties();
        env.put("oms.driver.autoStart", "false");
        env.put("oms.orderPreallocateCount", "1000");
        // disable thread affinities:
        env.put("nv.enablecpuaffinitymasks", "false");
        // use local in process discovery
        env.put("nv.discovery.descriptor", "loopback://discovery");
        // use in process loopback bus for messaging:
        env.put("oms.bus.oms.descriptor", "loopback://oms-bus");

        // start oms:
        startApp(Application.class, "oms", "oms1.local", env);

        // start driver:
        Driver driverApp = startApp(Driver.class, "driver", "driver.neeve", env);

        // sleep to let all connections be established.
        Thread.sleep(5000);

        int newOrderCount = 1000;
        System.out.println("Sending in " + newOrderCount + " New Orders...");
        driverApp.start(newOrderCount, 1000, true);
        // poll for ad response received
        while (driverApp.getSentCount() < newOrderCount) {
            Thread.sleep(200);
        }
        System.out.println("...New Orders Sent ... Waiting for responses");

        while (driverApp.getReceivedCount() < newOrderCount) {
            Thread.sleep(200);
        }
        System.out.println("Responses received ... validating final counts.");

        // sleep to check for any extra messages
        Thread.sleep(1000);

        assertEquals("Wrong number of orders sent", newOrderCount, driverApp.getSentCount());
        assertEquals("Wrong number of orders received", newOrderCount, driverApp.getReceivedCount());
    }
}
