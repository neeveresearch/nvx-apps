package com.neeve.ccfd.all;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

/**
 * Test end to end message flow. 
 */
public class TestFlow extends AbstractTest {
    @Test
    public void testFlow() throws Throwable {
        // configure
        Properties env = new Properties();
        env.put("lumino.agent.env", "neeve-lab");
        env.put("driver.interactive", "false");
        env.put("nv.conservecpu", "true");
        env.put("CCFD_BUS_DESCRIPTOR", "loopback://ccfd");
        env.put("nv.discovery.descriptor", "local://test&initWaitTime=0");
        // disable clustering to speed up app startup
        env.put("x.apps.fraudanalyzer-1.storage.clustering.enabled", "false");
        env.put("x.apps.fraudanalyzer-2.storage.clustering.enabled", "false");
        env.put("x.apps.cardholdermaster-1.storage.clustering.enabled", "false");
        env.put("x.apps.cardholdermaster-2.storage.clustering.enabled", "false");
        env.put("x.apps.merchantmaster-1.storage.clustering.enabled", "false");
        env.put("x.apps.merchantmaster-2.storage.clustering.enabled", "false");
        env.put("x.apps.cardmaster-1.storage.clustering.enabled", "false");
        env.put("x.apps.cardmaster-2.storage.clustering.enabled", "false");

        // start apps
        startApp(com.neeve.ccfd.fraudanalyzer.Application.class, "fraudanalyzer-1", "fraudanalyzer-1-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.fraudanalyzer.Application.class, "fraudanalyzer-2", "fraudanalyzer-2-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.cardholdermaster.Application.class, "cardholdermaster-1", "cardholdermaster-1-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.cardholdermaster.Application.class, "cardholdermaster-2", "cardholdermaster-2-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.merchantmaster.Application.class, "merchantmaster-1", "merchantmaster-1-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.merchantmaster.Application.class, "merchantmaster-2", "merchantmaster-2-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.cardmaster.Application.class, "cardmaster-1", "cardmaster-1-1", "nvx-app-ccfd-all", env);
        startApp(com.neeve.ccfd.cardmaster.Application.class, "cardmaster-2", "cardmaster-2-1", "nvx-app-ccfd-all", env);

        // sleep to let all connections be established.
        Thread.sleep(5000);

        // start the driver
        com.neeve.ccfd.perfdriver.Application driverApp = startApp(com.neeve.ccfd.perfdriver.Application.class, "perfdriver", "perfdriver-1", "nvx-app-ccfd-perfdriver", env);

        // poll for authorization response received
        while (driverApp.getAuthorizationResponseCount() < 1000) {
            Thread.sleep(1000);
        }

        // sleep to check for any extra messages
        Thread.sleep(1000);

        // validate
        assertEquals("Wrong number of authorizations requested", 1000, driverApp.getAuthorizationRequestCount());
        assertEquals("Wrong number of authorizations performed", 1000, driverApp.getAuthorizationResponseCount());
    }
}
