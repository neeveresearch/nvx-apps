package com.neeve.ccfd.fraudanalyzer;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.neeve.ccfd.fraudanalyzer.driver.ReceiveDriver;
import com.neeve.ccfd.fraudanalyzer.driver.SendDriver;

/**
 * A test case that tests the application flow. 
 */
public class TestFlow extends AbstractTest {
    @Test
    public void testFlow() throws Throwable {
        // configure
        Properties env = new Properties();
        env.put("nv.ddl.profiles", "test");
        env.put("x.apps.templates.fraudanalyzer-app-template.storage.clustering.enabled", "false");

        // start the processor
        startApp(Application.class, "fraudanalyzer-1", "fraudanalyzer-1-1", env);

        // start the receiver
        ReceiveDriver receiver = startApp(ReceiveDriver.class, "fraudanalyzer-receive-driver", "fraudanalyzer-receive-driver", env);
        Thread.sleep(1000);

        // start the sender
        SendDriver sender = startApp(SendDriver.class, "fraudanalyzer-send-driver", "fraudanalyzer-send-driver", env);

        // send
        long timeout = System.currentTimeMillis() + 60000;
        while (receiver.getNumReceived() < 1000 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // validate
        assertEquals("Receiver did not receive all Events", sender.getSentCount(), receiver.getNumReceived());
    }
}
