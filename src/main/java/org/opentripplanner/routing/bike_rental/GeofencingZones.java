package org.opentripplanner.routing.bike_rental;

import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class GeofencingZones {
    private final Set<GeofencingZone> zones;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    public GeofencingZones(Set<GeofencingZone> zones) {this.zones = zones;}

    public boolean canDropOffVehicle(Coordinate c) {
        var point = geometryFactory.createPoint(c);
        return zones.stream().anyMatch(zone -> zone.canDropOffVehicle(point));
    };

    public int size() { return zones.size(); };

    public static class GeofencingZone {

        final Geometry geometry;

        public GeofencingZone(Geometry geometry) { this.geometry = geometry; }

        boolean canDropOffVehicle(Point p) {
            return geometry.contains(p);
        }
    }
}

