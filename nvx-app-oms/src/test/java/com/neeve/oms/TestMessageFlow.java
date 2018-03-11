package com.neeve.oms;
/**
 * Copyright (c) 2017 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.neeve.ddl.DdlConfigConstants;
import com.neeve.oms.driver.local.LocalDriver;
import com.neeve.oms.driver.remote.Driver;
import com.neeve.oms.es.Application;

/**
 * Test end to end message flow. 
 */
public class TestMessageFlow extends AbstractTest {

    @Test
    public void testMessageFlow() throws Throwable {
        Properties env = new Properties();
        env.put("nv.ddl.profiles", "test");

        // start oms:
        startApp(Application.class, "oms", "oms1", env);

        // start driver:
        Driver driverApp = startApp(Driver.class, "driver", "driver", env);

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

    @Test
    @Ignore
    public void testLocalDriverFlow() throws Throwable {
        Properties env = new Properties();
        env.put(DdlConfigConstants.DDL_PROFILES_PROPNAME, "test,local");

        // start oms:
        final Application application = startApp(Application.class, "oms", "oms1", env);
        final LocalDriver localDriver = application.getLocalDriver();

        int newOrderCount = 1000;
        System.out.println("Sending in " + newOrderCount + " New Orders...");
        localDriver.start(newOrderCount, 1000, true);
        // poll for ad response received
        while (localDriver.getSentCount() < newOrderCount) {
            Thread.sleep(200);
        }
        System.out.println("...New Orders Sent ... Waiting for responses");

        while (localDriver.getReceivedCount() < newOrderCount) {
            Thread.sleep(200);
        }
        System.out.println("Responses received ... validating final counts.");

        // sleep to check for any extra messages
        Thread.sleep(1000);

        assertEquals("Wrong number of orders sent", newOrderCount, localDriver.getSentCount());
        assertEquals("Wrong number of orders received", newOrderCount, localDriver.getReceivedCount());
    }
}
