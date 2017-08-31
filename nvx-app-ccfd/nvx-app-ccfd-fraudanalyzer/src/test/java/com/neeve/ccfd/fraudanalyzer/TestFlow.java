package com.neeve.ccfd.fraudanalyzer;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

import com.neeve.ccfd.fraudanalyzer.driver.ReceiveDriver;
import com.neeve.ccfd.fraudanalyzer.driver.SendDriver;
import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.util.UtlFile;

/**
 * A test case that tests the application flow. 
 */
public class TestFlow {
    @Test
    public void testFlow() throws Throwable {
        // configure
        URL config = new File(System.getProperty("basedir"), "conf/config.xml").toURI().toURL();
        File testBedRoot = new File(System.getProperty("basedir"), "target/testbed/TestFlow");
        UtlFile.deleteDirectory(testBedRoot);
        Properties env = new Properties();
        env.put("NVROOT", testBedRoot.getCanonicalPath().toString());
        env.put("nv.conservecpu", "true");
        env.put("nv.discovery.descriptor", "local://test&initWaitTime=0");
        env.put("x.apps.cardmaster.storage.clustering.enabled", "false");
        env.put("CCFD_BUS_DESCRIPTOR", "loopback://ccfd");

        // start the processor
        EmbeddedXVM processor1XVM = EmbeddedXVM.create(config, "fraudanalyzer-1", env);
        processor1XVM.start();

        // start the receiver
        EmbeddedXVM receiverXVM = EmbeddedXVM.create(config, "fraudanalyzer-receive-driver", env);
        receiverXVM.start();
        ReceiveDriver receiver = (ReceiveDriver)receiverXVM.getApplication("fraudanalyzer-receive-driver");
        Thread.sleep(1000);

        // start the sender
        EmbeddedXVM senderXVM = EmbeddedXVM.create(config, "fraudanalyzer-send-driver", env);
        senderXVM.start();
        SendDriver sender = (SendDriver)senderXVM.getApplication("fraudanalyzer-send-driver");

        // send
        long timeout = System.currentTimeMillis() + 60000;
        while (receiver.getNumReceived() < 1000 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }

        // validate
        assertEquals("Receiver did not receive all Events", sender.getSentCount(), receiver.getNumReceived());
    }
}
