package com.neeve.geofencer;

import com.neeve.aep.*;
import com.neeve.aep.event.*;
import com.neeve.aep.annotations.*;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.geofencer.entities.GPSCoordinate;
import com.neeve.geofencer.entities.VehicleRoute;
import com.neeve.server.app.annotations.*;
import com.neeve.geofencer.messages.*;
import com.neeve.geofencer.state.Repository;
import com.neeve.geofencer.state.Vehicle;
import com.neeve.geofencer.vehiclemaster.messages.GetVehiclesMessage;
import com.neeve.geofencer.vehiclemaster.messages.GetVehiclesResponseEvent;
import com.neeve.geofencer.vehiclemaster.messages.VehicleUpdatedEvent;
import com.neeve.sma.MessageView;
import com.neeve.stats.IStats;
import com.neeve.stats.StatsFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AppVersion(1)
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
final public class VehicleEventProcessor {
    @Configured(property = "master.channel")
    private String masterChannel;
    @AppStat
    private final IStats.Counter processedCount = StatsFactory.createCounterStat("Num Processed");
    @AppStat
    private final IStats.Counter invalidEventCount = StatsFactory.createCounterStat("Num Invalid Events");
    @AppStat
    private final IStats.Counter droppedCount = StatsFactory.createCounterStat("Num Dropped");
    @AppStat
    private final IStats.Counter alertCount = StatsFactory.createCounterStat("Num Alerts");

    private AepMessageSender _messageSender;
    private UUID _transactionId;
    private Map<String, VehicleRoute> _vehicleRoutes = new HashMap<String, VehicleRoute>();

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public Repository createState(MessageView view) {
                return Repository.create();
            }
        };
    }

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    /**
     * Messaging prestart event handler
     */
    @EventHandler
    final public void onMessagingPrestart(final AepMessagingPrestartEvent event) throws Exception {
        final FirstMessage request = FirstMessage.create();
        event.setFirstMessage(request);
    }

    private void validate(LocationEventMessage message) throws Exception {
        if (message.getVehicleID() == null) {
            throw new Exception("Vehicle id is required");
        }
        if (message.getLocation() == null) {
            throw new Exception("Location is required");
        }
    }

    @EventHandler
    final public void onFirstMessage(final FirstMessage message, final Repository repository) throws Exception {
        // solicit vehicles from vehicle master
        GetVehiclesMessage request = GetVehiclesMessage.create();
        request.setTransactionID(_transactionId = UUID.randomUUID());
        _messageSender.sendMessage(masterChannel, request);
    }

    @EventHandler
    final public void onGetVehiclesResponseEvent(final GetVehiclesResponseEvent event, final Repository repository) throws Exception {
        // if response is for request solicited by this instance
        if (_transactionId.equals(event.getTransactionID())) {
            _vehicleRoutes.put(event.getVehicleID(), (VehicleRoute)event.getRoute().clone());
        }
    }

    @EventHandler
    final public void onVehicleUpdatedEvent(final VehicleUpdatedEvent event, final Repository repository) throws Exception {
        _vehicleRoutes.put(event.getVehicleID(), (VehicleRoute)event.getRoute().clone());
        // if vehicle exists, clear the corresponding vars to start from the beginning of the route
        Vehicle vehicle = repository.getVehicles().get(event.getVehicleID());
        if (vehicle != null) {
            vehicle.setRouteBlock(0);
            vehicle.setLocation(null);
            vehicle.setSpeed(0);
        }
    }

    @EventHandler
    final public void onLocationEvent(final LocationEventMessage message, final Repository repository) throws Exception {
        try {
            validate(message);
        }
        catch (Exception e) {
            invalidEventCount.increment();
            return;
        }

        // check if we have the vehicle route
        final VehicleRoute route = _vehicleRoutes.get(message.getVehicleID());
        if (route == null) {
            droppedCount.increment();
            return;
        }

        // update stats
        processedCount.increment();

        // update state
        final Vehicle vehicle = getOrCreateVehicle(repository, message.getVehicleID());
        if (!updateVehicleLocationIfInRouteBoundary(vehicle, route, message.getLocation(), message.getSpeed())) {
            sendRouteViolationAlert(vehicle, message.getLocation(), message.getSpeed());
            alertCount.increment();
        }
    }

    private Vehicle getOrCreateVehicle(Repository repository, final String vehicleId) {
        Vehicle vehicle = repository.getVehicles().get(vehicleId);
        if (vehicle == null) {
            vehicle = Vehicle.create();
            vehicle.setVehicleID(vehicleId);
            repository.getVehicles().put(vehicleId, vehicle);
        }
        return vehicle;
    }

    private boolean updateVehicleLocationIfInRouteBoundary(final Vehicle vehicle,
                                                           final VehicleRoute route,
                                                           final GPSCoordinate location,
                                                           final int speed) {
        int block = FenceUtil.locate(location, route, vehicle.getRouteBlock());
        if (block >= 0) {
            vehicle.setRouteBlock(block);
            vehicle.setLocation((GPSCoordinate)location.clone());
            vehicle.setSpeed(speed);
            vehicle.setLastUpdated(new Date());
            return true;
        }
        return false;
    }

    private void sendRouteViolationAlert(Vehicle vehicle, final GPSCoordinate location, final int speed) {
        final RouteViolationEvent event = RouteViolationEvent.create();
        event.setVehicleID(vehicle.getVehicleID());
        event.setLocation((GPSCoordinate)location.clone());
        event.setSpeed(speed);
        event.setLastLocation(vehicle.getLocation() != null ? (GPSCoordinate)vehicle.getLocation().clone() : null);
        event.setLastSpeed(vehicle.getSpeed());
        _messageSender.sendMessage("alerts", event);
    }

    @Command
    public long getProcessedCount() {
        return processedCount.getCount();
    }

    @Command
    public long getDroppedCount() {
        return droppedCount.getCount();
    }

    @Command
    public long getAlertCount() {
        return alertCount.getCount();
    }
}
