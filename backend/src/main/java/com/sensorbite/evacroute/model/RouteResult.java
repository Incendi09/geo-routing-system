package com.sensorbite.evacroute.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * The final route result, ready to be serialized as JSON.
 * Contains a GeoJSON LineString geometry plus metadata.
 * 
 * We're building GeoJSON manually here rather than pulling in a library -
 * keeps dependencies minimal and we only need simple structures anyway.
 */
public class RouteResult {

    @JsonProperty("type")
    private final String type = "Feature";

    @JsonProperty("geometry")
    private final Map<String, Object> geometry;

    @JsonProperty("properties")
    private final Map<String, Object> properties;

    @JsonProperty("meta")
    private final RouteMetadata meta;

    private RouteResult(List<Node> path, RouteMetadata meta) {
        this.meta = meta;

        // Build GeoJSON LineString geometry
        // Note: GeoJSON uses [longitude, latitude] order (opposite of what most people
        // expect)
        List<double[]> coordinates = new ArrayList<>();
        for (Node node : path) {
            coordinates.add(new double[] { node.getLongitude(), node.getLatitude() });
        }

        this.geometry = new HashMap<>();
        this.geometry.put("type", "LineString");
        this.geometry.put("coordinates", coordinates);

        // Properties can hold whatever extra info the client might want
        this.properties = new HashMap<>();
        this.properties.put("routeType", "evacuation");
        this.properties.put("distanceKm", String.format("%.2f", meta.getTotalDistanceMeters() / 1000));
    }

    /**
     * Factory method - cleaner than exposing constructor.
     */
    public static RouteResult fromPath(List<Node> path, RouteMetadata meta) {
        return new RouteResult(path, meta);
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getGeometry() {
        return Collections.unmodifiableMap(geometry);
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public RouteMetadata getMeta() {
        return meta;
    }
}
