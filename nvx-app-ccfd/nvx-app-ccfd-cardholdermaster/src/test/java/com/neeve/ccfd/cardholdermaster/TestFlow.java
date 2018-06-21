package com.neeve.ccfd.cardholdermaster;

import static org.junit.Assert.*;

import java.util.Properties;

import com.neeve.ccfd.cardholdermaster.driver.ReceiveDriver;
import com.neeve.ccfd.cardholdermaster.driver.SendDriver;
import org.junit.Test;

/**
 * A test case that tests the application flow. 
 */
public class TestFlow extends AbstractTest {
    @Test
    public void testFlow() throws Throwable {
        int sendCount = 1000;
        int sendRate = 100;
        // configure
        Properties env = new Properties();
        env.put("x.env.driver.sendCount", String.valueOf(sendCount));
        env.put("x.env.driver.sendRate", String.valueOf(sendRate));
        env.put("nv.ddl.profiles", "test");
        env.put("x.apps.templates.cardholdermaster-app-template.storage.clustering.enabled", "false");

        // start the processor
        startApp(Application.class, "cardholdermaster-1", "cardholdermaster-1-1", env);

        // start the receiver
        ReceiveDriver receiver = startApp(ReceiveDriver.class, "cardholdermaster-receive-driver", "cardholdermaster-receive-driver", env);
        Thread.sleep(1000);

        // start the sender
        SendDriver sender = startApp(SendDriver.class, "cardholdermaster-send-driver", "cardholdermaster-send-driver", env);

        // send
        long timeout = System.currentTimeMillis() + (30 + (sendCount / sendRate)) * 1000;
        while (receiver.getNumReceived() < sendCount && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // validate
        assertEquals("Receiver did not receive all Events", sender.getSentCount(), receiver.getNumReceived());
    }
}
