package org.opentripplanner.model;

import java.util.Collection;
import java.util.List;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred toin stop times.
 */
public interface StopLocation {

  /** The ID for the StopLocation */
  FeedScopedId getId();

  /** Name of the StopLocation, if provided */
  String getName();

  /**
   * Short text or a number that identifies the location for riders. These codes are often used in
   * phone-based reservation systems to make it easier for riders to specify a particular location.
   * The stop_code can be the same as id if it is public facing. This field should be left empty for
   * locations without a code presented to riders.
   */
  default String getCode() {
    return null;
  }

  default String getPlatformCode() {
    return null;
  }

  default Collection<FareZone> getFareZones() {
    return List.of();
  }

  /**
   * This is to ensure backwards compatibility with the REST API, which expects the GTFS zone_id
   * which only permits one zone per stop.
   */
  default String getFirstZoneAsString() {
    return getFareZones().stream().map(t -> t.getId().getId()).findFirst().orElse(null);
  }

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  WgsCoordinate getCoordinate();
}
