package com.neeve.ccfd.merchantmaster;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.neeve.ccfd.merchantmaster.driver.ReceiveDriver;
import com.neeve.ccfd.merchantmaster.driver.SendDriver;

/**
 * A test case that tests the application flow. 
 */
public class TestFlow extends AbstractTest {
    @Test
    public void testFlow() throws Throwable {
        int sendCount = 1000;
        int sendRate = 1000;
        // configure
        Properties env = new Properties();
        env.put("x.env.driver.sendCount", sendCount);
        env.put("x.env.driver.sendRate", sendRate);
        env.put("nv.ddl.profiles", "test");
        env.put("x.apps.templates.merchantmaster-app-template.storage.clustering.enabled", "false");

        // start the processor
        startApp(Application.class, "merchantmaster-1", "merchantmaster-1-1", env);

        // start the receiver
        ReceiveDriver receiver = startApp(ReceiveDriver.class, "merchantmaster-receive-driver", "merchantmaster-receive-driver", env);
        Thread.sleep(1000);

        // start the sender
        SendDriver sender = startApp(SendDriver.class, "merchantmaster-send-driver", "merchantmaster-send-driver", env);

        // send
        long timeout = System.currentTimeMillis() + (30 + (sendCount / sendRate)) * 1000;
        while (receiver.getNumReceived() < sendCount && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // validate
        assertEquals("Receiver did not receive all Events", sender.getSentCount(), receiver.getNumReceived());
    }
}
