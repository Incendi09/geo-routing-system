package com.sensorbite.evacroute.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensorbite.evacroute.exception.DataLoadException;
import com.sensorbite.evacroute.routing.graph.GraphBuilder.RoadSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads road network from a GeoJSON file.
 * 
 * The GeoJSON should contain LineString features representing road segments.
 * Each segment becomes one or more edges in the routing graph.
 * 
 * Expected format:
 * {
 * "type": "FeatureCollection",
 * "features": [
 * {
 * "type": "Feature",
 * "geometry": {
 * "type": "LineString",
 * "coordinates": [[lon1, lat1], [lon2, lat2], ...]
 * },
 * "properties": { "name": "ul. Marszałkowska", "highway": "primary" }
 * },
 * ...
 * ]
 * }
 */
public class GeoJsonLoader {

    private static final Logger log = LoggerFactory.getLogger(GeoJsonLoader.class);

    private final ObjectMapper objectMapper;

    public GeoJsonLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Load road segments from a GeoJSON file.
     * 
     * @param geoJsonPath Path to the file (file system or classpath)
     * @return List of road segments ready for graph building
     */
    public List<RoadSegment> loadRoadNetwork(String geoJsonPath) {
        log.info("Loading road network from: {}", geoJsonPath);
        long startTime = System.currentTimeMillis();

        try {
            JsonNode root = loadGeoJson(geoJsonPath);
            List<RoadSegment> segments = parseRoadSegments(root);

            log.info("Loaded {} road segments in {}ms",
                    segments.size(), System.currentTimeMillis() - startTime);

            return segments;

        } catch (IOException e) {
            throw new DataLoadException("Failed to load road network from " + geoJsonPath, e);
        }
    }

    private JsonNode loadGeoJson(String geoJsonPath) throws IOException {
        // Try as file path first
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

        throw new DataLoadException("Road network file not found: " + geoJsonPath);
    }

    private List<RoadSegment> parseRoadSegments(JsonNode root) {
        List<RoadSegment> segments = new ArrayList<>();

        String type = root.path("type").asText();
        if (!"FeatureCollection".equals(type)) {
            throw new DataLoadException("Expected GeoJSON FeatureCollection, got: " + type);
        }

        JsonNode features = root.path("features");
        int featureIndex = 0;

        for (JsonNode feature : features) {
            try {
                List<RoadSegment> featureSegments = parseFeature(feature, featureIndex);
                segments.addAll(featureSegments);
            } catch (Exception e) {
                log.warn("Failed to parse feature {}: {}", featureIndex, e.getMessage());
            }
            featureIndex++;
        }

        return segments;
    }

    private List<RoadSegment> parseFeature(JsonNode feature, int featureIndex) {
        List<RoadSegment> segments = new ArrayList<>();

        JsonNode geometry = feature.path("geometry");
        String geomType = geometry.path("type").asText();

        if (!"LineString".equals(geomType)) {
            log.trace("Skipping non-LineString feature: {}", geomType);
            return segments;
        }

        // Extract properties for the road
        Map<String, String> properties = extractProperties(feature.path("properties"));

        // Parse coordinates and create segments
        JsonNode coordinates = geometry.path("coordinates");
        double[] prevCoord = null;
        int pointIndex = 0;

        for (JsonNode coord : coordinates) {
            double lon = coord.get(0).asDouble();
            double lat = coord.get(1).asDouble();
            double[] currentCoord = new double[] { lon, lat };

            if (prevCoord != null) {
                // Create a segment from previous point to current point
                String startId = generateNodeId(prevCoord[1], prevCoord[0]);
                String endId = generateNodeId(lat, lon);

                RoadSegment segment = new RoadSegment(
                        startId, prevCoord[1], prevCoord[0], // lat, lon
                        endId, lat, lon,
                        properties);
                segments.add(segment);
            }

            prevCoord = currentCoord;
            pointIndex++;
        }

        return segments;
    }

    private Map<String, String> extractProperties(JsonNode propsNode) {
        Map<String, String> props = new HashMap<>();

        if (propsNode.isObject()) {
            propsNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    props.put(entry.getKey(), value.asText());
                } else if (value.isNumber()) {
                    props.put(entry.getKey(), String.valueOf(value.numberValue()));
                }
            });
        }

        return props;
    }

    /**
     * Generate a stable node ID from coordinates.
     * We round to 6 decimal places (~0.1m precision) to handle
     * slight coordinate variations at intersections.
     */
    private String generateNodeId(double lat, double lon) {
        // Round to 6 decimal places
        long latScaled = Math.round(lat * 1_000_000);
        long lonScaled = Math.round(lon * 1_000_000);
        return latScaled + "_" + lonScaled;
    }
}
