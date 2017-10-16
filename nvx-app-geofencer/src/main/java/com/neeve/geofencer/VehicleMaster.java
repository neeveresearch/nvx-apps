package com.neeve.geofencer;

import com.neeve.aep.*;
import com.neeve.aep.annotations.*;
import com.neeve.cli.annotations.Command;
import com.neeve.geofencer.entities.VehicleRoute;
import com.neeve.server.app.annotations.*;
import com.neeve.sma.MessageView;
import com.neeve.stats.IStats;
import com.neeve.stats.StatsFactory;

import com.neeve.geofencer.vehiclemaster.messages.*;
import com.neeve.geofencer.vehiclemaster.state.*;

@AppVersion(1)
@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
final public class VehicleMaster {
    @AppStat
    private final IStats.Counter numVehicles = StatsFactory.createCounterStat("Num Vehicles");
    @AppStat
    private final IStats.Counter numRoutes = StatsFactory.createCounterStat("Num Routes");

    private AepMessageSender _messageSender;

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

    private void validate(UpdateVehicleMessage message) throws Exception {
        if (message.getVehicleID() == null) {
            throw new Exception("Vehicle id is required");
        }
        if (message.getRoute() == null) {
            throw new Exception("Route is required");
        }
        if (message.getRoute().getStartLocation() == null) {
            throw new Exception("Route start location is required");
        }
        if (message.getRoute().getEndLocation() == null) {
            throw new Exception("Route end location is required");
        }
        if (message.getRoute().getSegments() == null) {
            throw new Exception("Route segments are required");
        }
        if (message.getRoute().getSegments().length < 2) {
            throw new Exception("At least 2 route segments are required");
        }
    }

    private void validate(GetVehiclesMessage message) throws Exception {
        if (message.getTransactionID() == null) {
            throw new Exception("Transaction id is required");
        }
    }

    @EventHandler
    final public void onUpdateVehicle(final UpdateVehicleMessage message, final Repository repository) throws Exception {
        validate(message);

        // update state
        Vehicle vehicle = repository.getVehicles().get(message.getVehicleID());
        if (vehicle == null) {
            vehicle = Vehicle.create();
            vehicle.setVehicleID(message.getVehicleID());
            repository.getVehicles().put(message.getVehicleID(), vehicle);
            numVehicles.increment();
        }
        vehicle.setRoute((VehicleRoute)message.getRoute().clone());

        // send vehicle updated event

        VehicleUpdatedEvent event = VehicleUpdatedEvent.create();
        event.setVehicleID(message.getVehicleID());
        event.setRoute((VehicleRoute)message.getRoute().clone());
        _messageSender.sendMessage("master-events", event);
        numRoutes.increment();
    }

    @EventHandler
    final public void onGetVehicles(final GetVehiclesMessage message, final Repository repository) throws Exception {
        validate(message);

        for (Vehicle vehicle : repository.getVehicles().values()) {
            GetVehiclesResponseEvent event = GetVehiclesResponseEvent.create();
            event.setTransactionID(message.getTransactionID());
            event.setVehicleID(vehicle.getVehicleID());
            event.setRoute((VehicleRoute)vehicle.getRoute().clone());
            _messageSender.sendMessage("master-events", event);
        }
    }

    @Command
    public long getNumVehicles() {
        return numVehicles.getCount();
    }

    @Command
    public long getNumRoutes() {
        return numRoutes.getCount();
    }
}
