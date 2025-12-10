package com.sensorbite.evacroute.geo;

import com.sensorbite.evacroute.exception.DataLoadException;
import com.sensorbite.evacroute.routing.graph.GraphBuilder.RoadSegment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GeoJSON loading functionality.
 */
class GeoJsonLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should load valid GeoJSON road network")
    void testLoadValidGeoJson() throws IOException {
        // Create a test GeoJSON file
        String geoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [
                          [21.01, 52.23],
                          [21.02, 52.24],
                          [21.03, 52.25]
                        ]
                      },
                      "properties": {
                        "name": "Test Road",
                        "highway": "primary"
                      }
                    }
                  ]
                }
                """;

        Path geoJsonFile = tempDir.resolve("test_roads.geojson");
        Files.writeString(geoJsonFile, geoJson);

        GeoJsonLoader loader = new GeoJsonLoader();
        List<RoadSegment> segments = loader.loadRoadNetwork(geoJsonFile.toString());

        assertNotNull(segments);
        assertEquals(2, segments.size(), "LineString with 3 points should create 2 segments");

        // Verify first segment
        RoadSegment first = segments.get(0);
        assertEquals(52.23, first.startLat, 0.001);
        assertEquals(21.01, first.startLon, 0.001);
        assertEquals(52.24, first.endLat, 0.001);
        assertEquals(21.02, first.endLon, 0.001);
    }

    @Test
    @DisplayName("Should handle multiple road features")
    void testLoadMultipleRoads() throws IOException {
        String geoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.23], [21.02, 52.24]]
                      },
                      "properties": {}
                    },
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.03, 52.25], [21.04, 52.26]]
                      },
                      "properties": {}
                    }
                  ]
                }
                """;

        Path geoJsonFile = tempDir.resolve("multi_roads.geojson");
        Files.writeString(geoJsonFile, geoJson);

        GeoJsonLoader loader = new GeoJsonLoader();
        List<RoadSegment> segments = loader.loadRoadNetwork(geoJsonFile.toString());

        assertEquals(2, segments.size());
    }

    @Test
    @DisplayName("Should skip non-LineString geometries")
    void testSkipNonLineString() throws IOException {
        String geoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "Point",
                        "coordinates": [21.01, 52.23]
                      },
                      "properties": {}
                    },
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.23], [21.02, 52.24]]
                      },
                      "properties": {}
                    }
                  ]
                }
                """;

        Path geoJsonFile = tempDir.resolve("mixed.geojson");
        Files.writeString(geoJsonFile, geoJson);

        GeoJsonLoader loader = new GeoJsonLoader();
        List<RoadSegment> segments = loader.loadRoadNetwork(geoJsonFile.toString());

        assertEquals(1, segments.size(), "Should only include LineString features");
    }

    @Test
    @DisplayName("Should throw DataLoadException for missing file")
    void testMissingFile() {
        GeoJsonLoader loader = new GeoJsonLoader();

        assertThrows(DataLoadException.class, () -> {
            loader.loadRoadNetwork("non_existent.geojson");
        });
    }

    @Test
    @DisplayName("Should extract properties from features")
    void testExtractProperties() throws IOException {
        String geoJson = """
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[21.01, 52.23], [21.02, 52.24]]
                      },
                      "properties": {
                        "name": "ul. Marszałkowska",
                        "highway": "primary",
                        "maxspeed": "50"
                      }
                    }
                  ]
                }
                """;

        Path geoJsonFile = tempDir.resolve("props.geojson");
        Files.writeString(geoJsonFile, geoJson);

        GeoJsonLoader loader = new GeoJsonLoader();
        List<RoadSegment> segments = loader.loadRoadNetwork(geoJsonFile.toString());

        assertEquals(1, segments.size());
        RoadSegment segment = segments.get(0);

        assertEquals("ul. Marszałkowska", segment.properties.get("name"));
        assertEquals("primary", segment.properties.get("highway"));
    }
}
