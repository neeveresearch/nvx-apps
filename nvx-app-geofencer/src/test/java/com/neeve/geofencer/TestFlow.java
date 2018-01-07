package com.neeve.geofencer;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.junit.Test;

import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.util.UtlFile;

/**
 * A test case that tests the application flow.
 */
public class TestFlow {

    @Test
    public void testFlow() throws Throwable {

        URL config = new File(System.getProperty("basedir"), "conf/config.xml").toURI().toURL();
        File testBedRoot = new File(System.getProperty("basedir"), "target/testbed/TestFlow");
        UtlFile.deleteDirectory(testBedRoot);

        Properties env = new Properties();
        env.put("NVROOT", testBedRoot.getCanonicalPath().toString());
        env.put("nv.optimizefor", "latency");
        env.put("nv.conservecpu", "true");
        env.put("x.xvms.templates.xvm-template.heartbeats.enabled", "false");
        env.put("route.numSegments", "1000");
        env.put("sender.numEventsPerSegment", "2000");
        env.put("sender.singleSimulation", "true");

        // Use loopback for in process discovery:
        env.put("nv.discovery.descriptor", "loopback://discovery&initWaitTime=0");

        // Disable clustering to speed up app startup (just functional testing here)
        env.put("x.apps.vehicle-master.clustering.enabled", "false");
        env.put("x.apps.vehicle-alert-receiver.clustering.enabled", "false");
        env.put("x.apps.vehicle-event-processor.clustering.enabled", "false");

        //Start the vehicle master service (standalone, persistent)
        EmbeddedXVM masterXVM = EmbeddedXVM.create(config, "vehicle-master-1", env);
        masterXVM.start();

        //Start the alert receiver
        EmbeddedXVM receiverXVM = EmbeddedXVM.create(config, "vehicle-alert-receiver", env);
        receiverXVM.start();
        VehicleAlertReceiver receiver = (VehicleAlertReceiver)receiverXVM.getApplication("vehicle-alert-receiver");

        //Start the primary event processor service (standalone, persistent)
        EmbeddedXVM processor1XVM = EmbeddedXVM.create(config, "vehicle-event-processor-1", env);
        processor1XVM.start();
        VehicleEventProcessor processor = (VehicleEventProcessor)processor1XVM.getApplication("vehicle-event-processor");

        //Start the vehicle event simulator:
        EmbeddedXVM senderXVM = EmbeddedXVM.create(config, "vehicle-event-sender", env);
        senderXVM.start();
        VehicleEventSender sender = (VehicleEventSender)senderXVM.getApplication("vehicle-event-sender");

        long timeout = System.currentTimeMillis() + 60000;
        while (processor.getProcessedCount() + processor.getDroppedCount() < 2000 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }
        Thread.sleep(100);

        System.out.println(" -- processed=" + processor.getProcessedCount() + ", alerts=" + processor.getAlertCount() + ", alerts received=" + receiver.getNumReceived() + " --");
        assertEquals("Processor did not receive all Events", sender.getCount(), processor.getProcessedCount());
    }
}
