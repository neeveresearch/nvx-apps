package com.neeve.geofencer;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

/**
 * A test case that tests the application flow.
 */
public class TestFlow extends AbstractTest {

    @Test
    public void testFlow() throws Throwable {

        Properties env = new Properties();
        env.put("nv.ddl.profiles", "test");
        env.put("x.env.nv.optimizefor", "latency");

        // Configure driver:
        env.put("x.env.route.numSegments", "1000");
        env.put("x.env.sender.numEventsPerSegment", "2000");
        env.put("x.env.sender.singleSimulation", "true");

        // Disable clustering to speed up app startup (just functional testing here)
        env.put("x.apps.vehicle-master.clustering.enabled", "false");
        env.put("x.apps.vehicle-alert-receiver.clustering.enabled", "false");
        env.put("x.apps.vehicle-event-processor.clustering.enabled", "false");

        // Disable heartbeats
        //env.put("x.xvms.templates.xvm-template.heartbeats.enabled", "false");

        //Start the vehicle master service (standalone, persistent)
        startApp(VehicleMaster.class, "vehicle-master", "vehicle-master-1", env);

        //Start the alert receiver
        VehicleAlertReceiver receiver = startApp(VehicleAlertReceiver.class, "vehicle-alert-receiver", "vehicle-alert-receiver", env);

        //Start the primary event processor service (standalone, persistent)
        VehicleEventProcessor processor = startApp(VehicleEventProcessor.class, "vehicle-event-processor", "vehicle-event-processor-1", env);

        //Start the vehicle event simulator:
        VehicleEventSender sender = startApp(VehicleEventSender.class, "vehicle-event-sender", "vehicle-event-sender", env);

        Thread.sleep(2000);

        long timeout = System.currentTimeMillis() + 60000;
        while (processor.getProcessedCount() + processor.getDroppedCount() < 2000 && System.currentTimeMillis() < timeout) {
            Thread.sleep(500);
        }
        Thread.sleep(100);

        System.out.println(" -- processed=" + processor.getProcessedCount() + ", alerts=" + processor.getAlertCount() + ", alerts received=" + receiver.getNumReceived() + " --");
        assertEquals("Processor did not receive all Events", sender.getCount(), processor.getProcessedCount());
    }
}
