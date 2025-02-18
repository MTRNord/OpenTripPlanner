package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.function.IntFunction;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * This a just an adaptor to look up transfers constraints. The adaptor hides the
 * {@link TransferService} specific API and functions as a bridge to the
 * {@code transferoptimization} model. The best solution would be to use the same
 * mechanism in Raptor and here, but that would require the main transit model to be refactored.
 * <p>
 * The adaptor makes it easy to test the {@link TransferGenerator} by mocking.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class TransferServiceAdaptor<T extends RaptorTripSchedule> {
    private final IntFunction<Stop> stopLookup;
    private final TransferService transferService;


    protected TransferServiceAdaptor(
            IntFunction<Stop> stopLookup,
            TransferService transferService
    ) {
        this.stopLookup = stopLookup;
        this.transferService = transferService;
    }

    public static <T extends RaptorTripSchedule> TransferServiceAdaptor<T> create(
            IntFunction<Stop> stopLookup,
            TransferService transferService
    ) {
        return new TransferServiceAdaptor<>(stopLookup, transferService);
    }


    public static <T extends RaptorTripSchedule> TransferServiceAdaptor<T> noop() {
        return new TransferServiceAdaptor<>(null, null) {
            @Override
            protected ConstrainedTransfer findTransfer(TripStopTime<T> from, T toTrip, int toStop) { return null; }
        };
    }

    /**
     * Find transfer in the same stop for the given from location and to trip/stop.
     */
    protected ConstrainedTransfer findTransfer(TripStopTime<T> from, T toTrip, int toStop) {
        return transferService.findTransfer(
                stop(from.stop()),
                stop(toStop),
                trip(from.trip()),
                trip(toTrip),
                from.stopPosition(),
                toTrip.findDepartureStopPosition(from.time(), toStop)
        );
    }

    private Stop stop(int index) {
        return stopLookup.apply(index);
    }

    private Trip trip(T raptorTripSchedule) {
        return ((TripSchedule)raptorTripSchedule).getOriginalTripTimes().getTrip();
    }
}
