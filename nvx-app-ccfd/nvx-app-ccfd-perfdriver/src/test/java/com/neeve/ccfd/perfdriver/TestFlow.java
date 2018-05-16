package com.neeve.ccfd.perfdriver;

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
        env.put("nv.ddl.profiles", "test");
        // disable clustering to speed up app startup
        env.put("x.apps.templates.fraudanalyzer-app-template.storage.clustering.enabled", "false");
        env.put("x.apps.templates.cardholdermaster-app-template.storage.clustering.enabled", "false");
        env.put("x.apps.templates.merchantmaster-app-template.storage.clustering.enabled", "false");
        env.put("x.apps.templates.cardmaster-app-template.storage.clustering.enabled", "false");

        // start apps
        startApp(com.neeve.ccfd.fraudanalyzer.Application.class, "fraudanalyzer", "fraudanalyzer-1-1", "nvx-app-ccfd-fraudanalyzer", env);
        startApp(com.neeve.ccfd.cardholdermaster.Application.class, "cardholdermaster", "cardholdermaster-1-1", "nvx-app-ccfd-cardholdermaster", env);
        startApp(com.neeve.ccfd.merchantmaster.Application.class, "merchantmaster", "merchantmaster-1-1", "nvx-app-ccfd-merchantmaster", env);
        startApp(com.neeve.ccfd.cardmaster.Application.class, "cardmaster", "cardmaster-1-1", "nvx-app-ccfd-cardmaster", env);

        // sleep to let all connections be established.
        Thread.sleep(5000);

        // start the driver
        Application driverApp = startApp(com.neeve.ccfd.perfdriver.Application.class, "perfdriver", "perfdriver-1", "nvx-app-ccfd-perfdriver", env);

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
