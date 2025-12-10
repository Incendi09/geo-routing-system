package com.sensorbite.evacroute.service;

import com.sensorbite.evacroute.exception.InvalidInputException;
import com.sensorbite.evacroute.exception.NoRouteFoundException;
import com.sensorbite.evacroute.geo.FloodZoneProvider;
import com.sensorbite.evacroute.model.Coordinate;
import com.sensorbite.evacroute.model.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RoutingService.
 * 
 * These tests use temporary GeoJSON files to simulate real data loading.
 */
class RoutingServiceTest {

    @TempDir
    Path tempDir;

    private Path roadsPath;
    private Path floodsPath;

    @BeforeEach
    void setUp() throws IOException {
        // Create a simple road network for testing
        //
        // A(52.23,21.01) --- B(52.23,21.02) --- C(52.23,21.03)
        // | |
        // D(52.22,21.01) ---------------------------- E(52.22,21.03)
        //
        String roads = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.23], [21.02, 52.23], [21.03, 52.23]]
                      },
                      "properties": {"name": "North Road"}
                    },
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.22], [21.03, 52.22]]
                      },
                      "properties": {"name": "South Road"}
                    },
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.23], [21.01, 52.22]]
                      },
                      "properties": {"name": "West Connector"}
                    },
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.03, 52.23], [21.03, 52.22]]
                      },
                      "properties": {"name": "East Connector"}
                    }
                  ]
                }
                """;

        roadsPath = tempDir.resolve("roads.geojson");
        Files.writeString(roadsPath, roads);

        // Empty flood zones by default
        floodsPath = tempDir.resolve("floods.geojson");
        Files.writeString(floodsPath, """
                {"type": "FeatureCollection", "features": []}
                """);
    }

    @Test
    @DisplayName("Should compute route between valid points")
    void testComputeRoute() {
        RoutingService service = new RoutingService(
                roadsPath.toString(),
                emptyFloodProvider());

        // Route from A area to C area
        Coordinate start = new Coordinate(52.23, 21.01);
        Coordinate end = new Coordinate(52.23, 21.03);

        RouteResult result = service.computeRoute(start, end);

        assertNotNull(result);
        assertNotNull(result.getMeta());
        assertTrue(result.getMeta().getTotalDistanceMeters() > 0);
        assertTrue(result.getMeta().getNodeCount() >= 2);
        assertEquals(0.0, result.getMeta().getRiskScore(), 0.01);
    }

    @Test
    @DisplayName("Should throw InvalidInputException for point too far from roads")
    void testPointTooFar() {
        RoutingService service = new RoutingService(
                roadsPath.toString(),
                emptyFloodProvider());

        // A point nowhere near our road network
        Coordinate start = new Coordinate(50.0, 19.0);
        Coordinate end = new Coordinate(52.23, 21.03);

        assertThrows(InvalidInputException.class, () -> {
            service.computeRoute(start, end);
        });
    }

    @Test
    @DisplayName("Should return graph info")
    void testGraphInfo() {
        RoutingService service = new RoutingService(
                roadsPath.toString(),
                emptyFloodProvider());

        RoutingService.GraphInfo info = service.getGraphInfo();

        assertTrue(info.nodeCount() > 0);
        assertTrue(info.edgeCount() > 0);
        assertEquals(0, info.hazardEdgeCount());
    }

    @Test
    @DisplayName("Should detect hazard edges when flood zones intersect roads")
    void testWithFloodZones() throws IOException {
        // Create a flood zone that covers part of the North Road
        String floods = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "Polygon",
                        "coordinates": [
                          [[21.015, 52.225], [21.025, 52.225], [21.025, 52.235], [21.015, 52.235], [21.015, 52.225]]
                        ]
                      },
                      "properties": {"name": "Flood Zone 1"}
                    }
                  ]
                }
                """;

        Path floodsWithData = tempDir.resolve("floods_with_data.geojson");
        Files.writeString(floodsWithData, floods);

        FloodZoneProvider provider = new com.sensorbite.evacroute.geo.MockFloodZoneProvider(
                floodsWithData.toString());

        RoutingService service = new RoutingService(
                roadsPath.toString(),
                provider);

        RoutingService.GraphInfo info = service.getGraphInfo();
        assertTrue(info.hazardEdgeCount() > 0, "Should have some hazardous edges");
    }

    private FloodZoneProvider emptyFloodProvider() {
        return () -> Collections.emptyList();
    }
}
