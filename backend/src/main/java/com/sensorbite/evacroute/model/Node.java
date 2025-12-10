package com.sensorbite.evacroute.model;

import java.util.Objects;

/**
 * A node in the road network graph.
 * Each node represents an intersection or endpoint in the road network.
 */
public class Node {

    private final String id;
    private final Coordinate position;

    public Node(String id, Coordinate position) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.position = Objects.requireNonNull(position, "Node position cannot be null");
    }

    /**
     * Convenience constructor when you have raw coordinates.
     */
    public Node(String id, double lat, double lon) {
        this(id, new Coordinate(lat, lon));
    }

    public String getId() {
        return id;
    }

    public Coordinate getPosition() {
        return position;
    }

    public double getLatitude() {
        return position.getLatitude();
    }

    public double getLongitude() {
        return position.getLongitude();
    }

    /**
     * Calculate distance to another node in meters.
     */
    public double distanceTo(Node other) {
        return this.position.distanceTo(other.position);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{id='" + id + "', pos=" + position + "}";
    }
}
