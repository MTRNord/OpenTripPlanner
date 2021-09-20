package org.opentripplanner.routing.algorithm.mapping;

import java.time.Instant;
import java.util.function.Consumer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.GroupBySimilarity;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;

import java.time.Instant;
import java.util.function.Consumer;
import org.opentripplanner.routing.api.request.StreetMode;

public class RoutingRequestToFilterChainMapper {
  private static final int KEEP_ONE = 1;

  /** Filter itineraries down to this limit, but not below. */
  private static final int KEEP_THREE = 3;

  /** Never return more that this limit of itineraries. */
  private static final int MAX_NUMBER_OF_ITINERARIES = 200;

  public static ItineraryFilter createFilterChain(
      RoutingRequest request,
      Instant filterOnLatestDepartureTime,
      boolean removeWalkAllTheWayResults,
      Consumer<Itinerary> maxLimitReachedSubscriber
  ) {
    var builder = new ItineraryFilterChainBuilder(request.arriveBy);
    var p = request.itineraryFilters;

    // Group by similar legs filter
    if(request.itineraryFilters != null) {

      builder.withMinSafeTransferTimeFactor(p.minSafeTransferTimeFactor);

      if (p.groupSimilarityKeepOne >= 0.5) {
        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.groupSimilarityKeepOne, KEEP_ONE)
        );
      }

      if (p.groupSimilarityKeepThree >= 0.5) {
        builder.addGroupBySimilarity(
            new GroupBySimilarity(p.groupSimilarityKeepThree, KEEP_THREE)
        );
      }
    }

    if (request.modes.contains(StreetMode.BIKE_TO_PARK)) {
      builder.withMinBikeParkingDistance(p.minBikeParkingDistance);
    }

    if (request.modes.contains(StreetMode.CAR_TO_PARK) && request.modes.directMode == StreetMode.BIKE) {
      builder.withRemoveBikeOnlyParkAndRideItineraries(true);
    }

    var flexWasRequested = request.modes.egressMode == StreetMode.FLEXIBLE ||
            request.modes.directMode == StreetMode.FLEXIBLE;
    builder
        .withMaxNumberOfItineraries(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES))
        .withMinSafeTransferTimeFactor(p.minSafeTransferTimeFactor)
        .withTransitGeneralizedCostLimit(p.transitGeneralizedCostLimit)
        .withBikeRentalDistanceRatio(p.bikeRentalDistanceRatio)
        .withParkAndRideDurationRatio(p.parkAndRideDurationRatio)
        .withNonTransitGeneralizedCostLimit(p.nonTransitGeneralizedCostLimit)
        .withRemoveTransitWithHigherCostThanBestOnStreetOnly(true)
        .withLatestDepartureTimeLimit(filterOnLatestDepartureTime)
        .withMaxLimitReachedSubscriber(maxLimitReachedSubscriber)
        .withRemoveWalkAllTheWayResults(removeWalkAllTheWayResults)
        .withFlexOnlyToDestination(flexWasRequested && p.flexOnlyToDestination)
        .withDebugEnabled(p.debug);

    return builder.build();
  }
}
