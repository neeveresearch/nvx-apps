package com.neeve.ccfd.cardmaster;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import com.neeve.ccfd.cardmaster.driver.ReceiveDriver;
import com.neeve.ccfd.cardmaster.driver.SendDriver;

/**
 * A test case that tests the application flow. 
 */
public class TestFlow extends AbstractTest {
    @Test
    public void testFlow() throws Throwable {
        // configure
        Properties env = new Properties();
        env.put("nv.ddl.profiles", "test");
        env.put("x.apps.cardmaster.storage.clustering.enabled", "false");

        // start the processor
        startApp(Application.class, "cardmaster-1", "cardmaster-1", env);

        // start the receiver
        ReceiveDriver receiver = startApp(ReceiveDriver.class, "cardmaster-receive-driver", "cardmaster-receive-driver", env);
        Thread.sleep(1000);

        // start the sender
        SendDriver sender = startApp(SendDriver.class, "cardmaster-send-driver", "cardmaster-send-driver", env);

        // send
        long timeout = System.currentTimeMillis() + 60000;
        while (receiver.getNumReceived() < 1000 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // validate
        assertEquals("Receiver did not receive all Events", sender.getSentCount(), receiver.getNumReceived());
    }
}
