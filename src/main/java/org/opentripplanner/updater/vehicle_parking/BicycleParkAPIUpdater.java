package org.opentripplanner.updater.vehicle_parking;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import org.opentripplanner.routing.vehicle_parking.VehicleParking.VehiclePlaces;

public class BicycleParkAPIUpdater extends ParkAPIUpdater {

    public BicycleParkAPIUpdater(String url, String feedId, Collection<String> staticTags) {
        super(url, feedId, staticTags);
    }

    @Override
    protected VehiclePlaces parseCapacity(JsonNode jsonNode) {
        return parseVehiclePlaces(jsonNode, "total", null, null);
    }

    @Override
    protected VehiclePlaces parseAvailability(JsonNode jsonNode) {
        return parseVehiclePlaces(jsonNode, "free", null, null);
    }
}
