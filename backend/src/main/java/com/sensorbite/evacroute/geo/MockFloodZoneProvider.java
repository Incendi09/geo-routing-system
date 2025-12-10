package com.sensorbite.evacroute.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensorbite.evacroute.exception.DataLoadException;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads flood zone polygons from a local GeoJSON file.
 * 
 * This is our "mock" implementation of FloodZoneProvider - instead of
 * calling Sentinel Hub or another real data source, we just load
 * pre-defined polygons from a file.
 * 
 * In a real system, you'd replace this with a client that:
 * - Authenticates with Sentinel Hub
 * - Fetches recent water detection results
 * - Parses the response into polygons
 * - Caches with appropriate TTL
 */
public class MockFloodZoneProvider implements FloodZoneProvider {

    private static final Logger log = LoggerFactory.getLogger(MockFloodZoneProvider.class);

    private final String geoJsonPath;
    private final ObjectMapper objectMapper;
    private List<Geometry> floodZones;
    private long lastLoadTime;

    public MockFloodZoneProvider(String geoJsonPath) {
        this.geoJsonPath = geoJsonPath;
        this.objectMapper = new ObjectMapper();
        this.floodZones = Collections.emptyList();
        this.lastLoadTime = -1;
    }

    @Override
    public List<Geometry> getFloodZones() {
        // Lazy load on first access
        if (floodZones.isEmpty() && lastLoadTime == -1) {
            load();
        }
        return floodZones;
    }

    @Override
    public void refresh() {
        load();
    }

    @Override
    public long getLastUpdateTime() {
        return lastLoadTime;
    }

    private void load() {
        log.info("Loading flood zones from: {}", geoJsonPath);
        long startTime = System.currentTimeMillis();

        try {
            JsonNode root = loadGeoJson();
            List<Geometry> polygons = parsePolygons(root);
            this.floodZones = Collections.unmodifiableList(polygons);
            this.lastLoadTime = System.currentTimeMillis();

            log.info("Loaded {} flood zones in {}ms",
                    floodZones.size(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Failed to load flood zones from {}: {}", geoJsonPath, e.getMessage());
            // Don't throw - just return empty list. This allows the app to work
            // without flood data (routes just won't avoid hazards)
            this.floodZones = Collections.emptyList();
        }
    }

    private JsonNode loadGeoJson() throws IOException {
        // Try as file path first, then as classpath resource
        Path filePath = Path.of(geoJsonPath);
        if (Files.exists(filePath)) {
            log.debug("Loading from file system: {}", filePath.toAbsolutePath());
            return objectMapper.readTree(Files.newInputStream(filePath));
        }

        // Try classpath
        String resourcePath = geoJsonPath.startsWith("/") ? geoJsonPath : "/" + geoJsonPath;
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is != null) {
            log.debug("Loading from classpath: {}", resourcePath);
            return objectMapper.readTree(is);
        }

        throw new DataLoadException("Flood zone file not found: " + geoJsonPath);
    }

    private List<Geometry> parsePolygons(JsonNode root) {
        List<Geometry> result = new ArrayList<>();

        // GeoJSON can be a FeatureCollection, Feature, or raw Geometry
        String type = root.path("type").asText();

        if ("FeatureCollection".equals(type)) {
            JsonNode features = root.path("features");
            for (JsonNode feature : features) {
                Geometry geom = parseFeature(feature);
                if (geom != null) {
                    result.add(geom);
                }
            }
        } else if ("Feature".equals(type)) {
            Geometry geom = parseFeature(root);
            if (geom != null) {
                result.add(geom);
            }
        } else {
            // Assume it's a raw geometry
            Geometry geom = parseGeometry(root);
            if (geom != null) {
                result.add(geom);
            }
        }

        return result;
    }

    private Geometry parseFeature(JsonNode feature) {
        JsonNode geometry = feature.path("geometry");
        return parseGeometry(geometry);
    }

    private Geometry parseGeometry(JsonNode geomNode) {
        String geomType = geomNode.path("type").asText();
        JsonNode coordinates = geomNode.path("coordinates");

        if ("Polygon".equals(geomType)) {
            return parsePolygon(coordinates);
        } else if ("MultiPolygon".equals(geomType)) {
            return parseMultiPolygon(coordinates);
        } else {
            log.debug("Skipping geometry type: {}", geomType);
            return null;
        }
    }

    private Geometry parsePolygon(JsonNode coordinates) {
        // Polygon coordinates: [ [ [lon, lat], [lon, lat], ... ] ]
        // First array is the outer ring, subsequent arrays are holes
        JsonNode outerRing = coordinates.get(0);
        if (outerRing == null) {
            return null;
        }

        List<double[]> coords = new ArrayList<>();
        for (JsonNode point : outerRing) {
            double lon = point.get(0).asDouble();
            double lat = point.get(1).asDouble();
            coords.add(new double[] { lon, lat });
        }

        return GeometryUtils.createPolygonFromGeoJson(coords);
    }

    private Geometry parseMultiPolygon(JsonNode coordinates) {
        // MultiPolygon: [ [ [ [lon, lat], ... ] ], [ [ [lon, lat], ... ] ] ]
        // For simplicity, we just collect all polygons and union them
        List<Geometry> polygons = new ArrayList<>();
        for (JsonNode polygonCoords : coordinates) {
            Geometry poly = parsePolygon(polygonCoords);
            if (poly != null) {
                polygons.add(poly);
            }
        }

        if (polygons.isEmpty()) {
            return null;
        }

        // Union all polygons into a single geometry
        Geometry result = polygons.get(0);
        for (int i = 1; i < polygons.size(); i++) {
            result = result.union(polygons.get(i));
        }
        return result;
    }
}
