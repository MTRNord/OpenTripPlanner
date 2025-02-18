package org.opentripplanner.updater.vehicle_parking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingTestBase;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.updater.DataSource;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterRunnable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class VehicleParkingUpdaterTest extends VehicleParkingTestBase {

  private DataSource<VehicleParking> dataSource;
  private VehicleParkingUpdater vehicleParkingUpdater;

  @BeforeEach
  @SuppressWarnings("unchecked")
  public void setup() {
    initGraph();

    dataSource = (DataSource<VehicleParking>) Mockito.mock(DataSource.class);
    when(dataSource.update()).thenReturn(true);

    var parameters = new VehicleParkingUpdaterParameters(null, null, null, null, -1, false, null, null);
    vehicleParkingUpdater = new VehicleParkingUpdater(parameters, dataSource);
    vehicleParkingUpdater.setup(graph);
  }

  @Test
  public void addVehicleParkingTest() {
    var vehicleParkings = List.of(
        createParingWithEntrances("1", 0.0001, 0)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);
  }

  private void assertVehicleParkingsInGraph(int vehicleParkingNumber) {
    var parkingVertices = graph.getVerticesOfType(VehicleParkingEntranceVertex.class);

    assertEquals(vehicleParkingNumber, parkingVertices.size());

    for (var parkingVertex : parkingVertices) {
      assertEquals(2, parkingVertex.getIncoming().size());
      assertEquals(2, parkingVertex.getOutgoing().size());

      assertEquals(
          1,
          parkingVertex.getIncoming().stream().filter(StreetVehicleParkingLink.class::isInstance).count()
      );

      assertEquals(
          1,
          parkingVertex.getIncoming().stream().filter(VehicleParkingEdge.class::isInstance).count()
      );

      assertEquals(
          1,
          parkingVertex.getOutgoing().stream().filter(StreetVehicleParkingLink.class::isInstance).count()
      );

      assertEquals(
          1,
          parkingVertex.getOutgoing().stream().filter(VehicleParkingEdge.class::isInstance).count()
      );
    }

    assertEquals(vehicleParkingNumber, graph.getService(VehicleParkingService.class).getVehicleParkings().count());
  }

  private void runUpdaterOnce() {
    class GraphUpdaterMock extends GraphUpdaterManager {

      public GraphUpdaterMock(
          Graph graph, List<GraphUpdater> updaters
      ) {
        super(graph, updaters);
      }

      @Override
      public void execute(GraphWriterRunnable runnable) {
        runnable.run(graph);
      }
    }

    var graphUpdaterManager = new GraphUpdaterMock(graph, List.of(vehicleParkingUpdater));
    graphUpdaterManager.startUpdaters();
    graphUpdaterManager.stop();
  }

  @Test
  public void updateVehicleParkingTest() {
    var vehiclePlaces = VehicleParking.VehiclePlaces.builder()
        .bicycleSpaces(1)
        .build();

    var vehicleParkings = List.of(
        createParingWithEntrances("1", 0.0001, 0, vehiclePlaces)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);

    var vehicleParkingInGraph = graph.getService(VehicleParkingService.class).getVehicleParkings().findFirst().orElseThrow();
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getAvailability());
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getCapacity());

    vehiclePlaces = VehicleParking.VehiclePlaces.builder()
        .bicycleSpaces(2)
        .build();
    vehicleParkings = List.of(
        createParingWithEntrances("1", 0.0001, 0, vehiclePlaces)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);

    vehicleParkingInGraph = graph.getService(VehicleParkingService.class).getVehicleParkings().findFirst().orElseThrow();
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getAvailability());
    assertEquals(vehiclePlaces, vehicleParkingInGraph.getCapacity());
  }

  @Test
  public void deleteVehicleParkingTest() {
    var vehicleParkings = List.of(
        createParingWithEntrances("1", 0.0001, 0),
        createParingWithEntrances("2", -0.0001, 0)
    );

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(2);

    vehicleParkings = List.of(createParingWithEntrances("1", 0.0001, 0));

    when(dataSource.getUpdates()).thenReturn(vehicleParkings);
    runUpdaterOnce();

    assertVehicleParkingsInGraph(1);
  }

  @Test
  public void addNotOperatingVehicleParkingTest() {
    var vehicleParking = VehicleParking.builder()
        .state(VehicleParking.VehicleParkingState.CLOSED)
        .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, graph.getService(VehicleParkingService.class).getVehicleParkings().count());
    assertVehicleParkingNotLinked();
  }

  private void assertVehicleParkingNotLinked() {
    assertEquals(0, graph.getVerticesOfType(VehicleParkingEntranceVertex.class).size());
    assertEquals(0, graph.getEdgesOfType(StreetVehicleParkingLink.class).size());
    assertEquals(0, graph.getEdgesOfType(VehicleParkingEdge.class).size());
  }

  @Test
  public void updateNotOperatingVehicleParkingTest() {
    var vehiclePlaces = VehicleParking.VehiclePlaces.builder()
        .bicycleSpaces(1)
        .build();

    var vehicleParking = VehicleParking.builder()
        .availability(vehiclePlaces)
        .state(VehicleParking.VehicleParkingState.CLOSED)
        .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    var vehicleParkingService =  graph.getService(VehicleParkingService.class);
    assertEquals(1, vehicleParkingService.getVehicleParkings().count());
    assertEquals(vehiclePlaces, vehicleParkingService.getVehicleParkings().findFirst().orElseThrow().getAvailability());
    assertVehicleParkingNotLinked();

    vehiclePlaces = VehicleParking.VehiclePlaces.builder()
        .bicycleSpaces(2)
        .build();

    vehicleParking = VehicleParking.builder()
        .availability(vehiclePlaces)
        .state(VehicleParking.VehicleParkingState.CLOSED)
        .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    assertEquals(1, vehicleParkingService.getVehicleParkings().count());
    assertEquals(vehiclePlaces, vehicleParkingService.getVehicleParkings().findFirst().orElseThrow().getAvailability());
    assertVehicleParkingNotLinked();
  }

  @Test
  public void deleteNotOperatingVehicleParkingTest() {
    var vehicleParking = VehicleParking.builder()
        .state(VehicleParking.VehicleParkingState.CLOSED)
        .build();

    when(dataSource.getUpdates()).thenReturn(List.of(vehicleParking));
    runUpdaterOnce();

    var vehicleParkingService =  graph.getService(VehicleParkingService.class);
    assertEquals(1, vehicleParkingService.getVehicleParkings().count());

    when(dataSource.getUpdates()).thenReturn(List.of());
    runUpdaterOnce();

    assertEquals(0, vehicleParkingService.getVehicleParkings().count());
  }

}
