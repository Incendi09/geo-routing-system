package com.sensorbite.evacroute.model;

import java.util.Objects;

/**
 * An edge in the road network graph, connecting two nodes.
 * 
 * The key insight here is that we track both the base distance AND a hazard
 * multiplier.
 * When routing through flood zones, we bump up the effective cost without
 * losing
 * the original distance info (useful for reporting).
 */
public class Edge {

    private final Node source;
    private final Node target;
    private final double distance; // Base distance in meters
    private final double hazardMultiplier; // 1.0 = normal, higher = more dangerous
    private final boolean inHazardZone;

    /**
     * Create a normal (non-hazardous) edge.
     */
    public Edge(Node source, Node target, double distance) {
        this(source, target, distance, 1.0, false);
    }

    /**
     * Full constructor with hazard info.
     */
    public Edge(Node source, Node target, double distance, double hazardMultiplier, boolean inHazardZone) {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        this.distance = distance;
        this.hazardMultiplier = hazardMultiplier;
        this.inHazardZone = inHazardZone;
    }

    /**
     * Create a copy of this edge but with hazard info applied.
     * Useful when we detect an edge crosses a flood zone.
     */
    public Edge withHazard(double multiplier) {
        return new Edge(source, target, distance, multiplier, true);
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public double getDistance() {
        return distance;
    }

    public double getHazardMultiplier() {
        return hazardMultiplier;
    }

    public boolean isInHazardZone() {
        return inHazardZone;
    }

    /**
     * The effective cost used by routing algorithms.
     * This is where the hazard penalty kicks in.
     */
    public double getEffectiveCost() {
        return distance * hazardMultiplier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Edge edge = (Edge) o;
        return source.equals(edge.source) && target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public String toString() {
        String hazardInfo = inHazardZone ? ", HAZARD x" + hazardMultiplier : "";
        return "Edge{" + source.getId() + " -> " + target.getId()
                + ", dist=" + String.format("%.1fm", distance) + hazardInfo + "}";
    }
}
