package org.opentripplanner.routing.bike_rental;

import com.fasterxml.jackson.databind.JsonNode;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opentripplanner.common.geometry.GeoJsonModule;

public class GeofencingZone {

    final Geometry geometry;

    public GeofencingZone(Geometry geometry) { this.geometry = geometry; }

    boolean canDropOffVehicle(Coordinate c) {
        return true;
    }
}
