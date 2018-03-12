package com.neeve.geofencer;

import com.eaio.uuid.UUID;
import com.neeve.aep.AepMessageSender;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.geofencer.entities.GPSCoordinate;
import com.neeve.geofencer.entities.Segment;
import com.neeve.geofencer.entities.VehicleRoute;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.StatsFactory;

import com.neeve.geofencer.messages.LocationEventMessage;
import com.neeve.geofencer.vehiclemaster.messages.UpdateVehicleMessage;
import com.neeve.server.app.annotations.AppFinalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final public class VehicleEventSender {
    private class TripSimulator implements Runnable {
        private String vehicleId;
        private VehicleRoute route;
        private volatile boolean done = false;

        private VehicleRoute addNewRoute(String vehicleId) {
            VehicleRoute route = FenceUtil.generateRoute(FenceUtil.START_LOCATION, FenceUtil.END_LOCATION, numRouteSegments);

            // send message to vehicle master to add new route
            sendAddNewVehicleRouteMessage(vehicleId, route);

            System.out.println("Added new vehicle route for vehicle id " + vehicleId);
            return route;
        }

        private void traverseRoute() {
            int numSegments = route.getSegments().length;
            int numPoints = numEventsPerSegment;
            int numBreaches = Math.min(2 + numPoints / 100, 100);
            double pointsPerSegment = (double)numPoints / numSegments;
            int breachFreq = numBreaches > 0 ? numPoints / numBreaches : 0;
            GPSCoordinate lastLoc = null;
            int lastFenceIndex = -1;
            int latStep = 0;
            int lngStep = 0;
            sendLocationEventMessage(vehicleId, route.getStartLocation());
            for (int i = 1, b = 0; i < numPoints + numBreaches - 1; i++) {
                if (done) {
                    System.out.println("Aborting trip for vehicle " + vehicleId);
                    return;
                }
                GPSCoordinate location = lastLoc;
                int fenceIndex = (int)((i - b) / pointsPerSegment);
                if (fenceIndex != lastFenceIndex) {
                    GPSCoordinate f1 = getSegmentMid(route.getSegments()[fenceIndex]);
                    GPSCoordinate f2 = getSegmentMid(route.getSegments()[fenceIndex < numSegments - 1 ? fenceIndex + 1 : fenceIndex]);
                    latStep = (int)((f2.getLatitude() - f1.getLatitude()) / (pointsPerSegment + 1));
                    lngStep = (int)((f2.getLongitude() - f1.getLongitude()) / (pointsPerSegment + 1));
                    location = f1;
                }
                location = FenceUtil.createLocation(location.getLatitude() + latStep, location.getLongitude() + lngStep);
                if (breachFreq > 0 && b < numBreaches && i % breachFreq == 0) {
                    // breach
                    sendLocationEventMessage(vehicleId, FenceUtil.createLocation(lastLoc.getLatitude() + 21000, lastLoc.getLongitude()));
                    b++;
                }
                else {
                    sendLocationEventMessage(vehicleId, location);
                    lastLoc = location;
                    lastFenceIndex = fenceIndex;
                }
            }
            sendLocationEventMessage(vehicleId, route.getEndLocation());
            System.out.println("...finished trip simulation for vehicle id " + vehicleId + ", sentCount: " + sentCount.getCount());
        }

        private int getMid(int start, int end) {
            return start + (end - start) / 2;
        }

        private GPSCoordinate getSegmentMid(Segment segment) {
            return FenceUtil.createLocation(getMid(segment.getStartLocation().getLatitude(), segment.getEndLocation().getLatitude()),
                                            getMid(segment.getStartLocation().getLongitude(), segment.getEndLocation().getLongitude()));
        }

        public TripSimulator() {
            vehicleId = new UUID().toString();
        }

        public void run() {
            try {
                while (!done) {
                    route = addNewRoute(vehicleId);
                    // wait for vehicle route to be created and published to the processor
                    Thread.sleep(5000);
                    traverseRoute();
                    if (singleSimulation) {
                        break;
                    }
                    if (!done) {
                        Thread.sleep(250);
                    }
                }
                System.out.println("Aborted trip simulations for vehicle " + vehicleId);
            }
            catch (Exception e) {
                System.out.println("Trip simulation interrupted for vehicle id " + vehicleId);
                e.printStackTrace();
            }

        }

        public void stop() {
            done = true;
        }

    }

    @Configured(property = "sender.numEventsPerSegment")
    private int numEventsPerSegment;
    @Configured(property = "sender.sendRate")
    private int sendRate;
    @Configured(property = "processor.channel")
    private String processorChannel;
    @Configured(property = "master.channel")
    private String masterChannel;
    @Configured(property = "sender.singleSimulation")
    private boolean singleSimulation;
    @Configured(property = "route.numSegments")
    private int numRouteSegments;
    @Configured(property = "sender.numThreads")
    private int numThreads;

    @AppStat
    private final Counter sentCount = StatsFactory.createCounterStat("Vehicle Events Sent");

    private Random random = new Random();
    private ExecutorService _executor;
    private List<TripSimulator> simulators = new ArrayList<TripSimulator>();

    private volatile AepMessageSender messageSender;

    public VehicleEventSender() {}

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    private void sendAddNewVehicleRouteMessage(String vehicleId, VehicleRoute route) {
        UpdateVehicleMessage message = UpdateVehicleMessage.create();
        message.setVehicleID(vehicleId);
        message.setRoute((VehicleRoute)route.clone());
        messageSender.sendMessage(masterChannel, message);
    }

    private void sendLocationEventMessage(String vehicleId, GPSCoordinate location) {
        LocationEventMessage message = LocationEventMessage.create();
        message.setVehicleID(vehicleId);
        message.setSpeed(random.nextInt(80));
        GPSCoordinate gpsCoordinate = GPSCoordinate.create();
        gpsCoordinate.setLatitude(location.getLatitude());
        gpsCoordinate.setLongitude(location.getLongitude());
        message.setLocation(gpsCoordinate);
        //        System.out.println("SENDER: " + message.toJsonString());
        messageSender.sendMessage(processorChannel, message);
        sentCount.increment();
    }

    /**
     * Gets the number of messages sent by the sender.
     *
     * @return The number of messages sent by this sender.
     */
    @Command
    public long getCount() {
        return sentCount.getCount();
    }

    @Command(name = "stop", displayName = "Stop Simulation")
    public final void doStop() {
        System.out.println("Stopping trip simulators...");
        for (TripSimulator simulator : simulators) {
            simulator.stop();
        }
    }

    @Command(name = "start", displayName = "Run Simulation")
    public final void doStart() {
        System.out.println("Starting trip simulators...");
        for (TripSimulator simulator : simulators) {
            _executor.submit(simulator);
        }
    }

    @Command(name = "addnewvehicle", displayName = "Add New Vehicle")
    public final void doAddNewVehicle() {
        System.out.println("Adding new vehicle...");
        TripSimulator simulator = new TripSimulator();
        simulators.add(simulator);
        _executor.submit(simulator);
    }

    @AppMain
    public void run(String[] args) {
        _executor = Executors.newFixedThreadPool(numThreads);
        doAddNewVehicle();
    }

    /**
     * Finalize the application
     */
    @AppFinalizer
    final public void finalize() {
        // shut down executors
        if (_executor != null) {
            _executor.shutdown();
        }
    }
}
