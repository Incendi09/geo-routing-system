package com.sensorbite.evacroute.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata about a computed route.
 * This gets returned alongside the route geometry to give the client
 * useful info about what happened during routing.
 */
public class RouteMetadata {

    @JsonProperty("totalDistanceMeters")
    private final double totalDistanceMeters;

    @JsonProperty("nodeCount")
    private final int nodeCount;

    @JsonProperty("avoidedHazardSegments")
    private final int avoidedHazardSegments;

    @JsonProperty("hazardSegmentsTraversed")
    private final int hazardSegmentsTraversed;

    @JsonProperty("computationTimeMs")
    private final long computationTimeMs;

    @JsonProperty("riskScore")
    private final double riskScore; // 0.0 = safe, 1.0 = very risky

    public RouteMetadata(
            double totalDistanceMeters,
            int nodeCount,
            int avoidedHazardSegments,
            int hazardSegmentsTraversed,
            long computationTimeMs,
            double riskScore) {
        this.totalDistanceMeters = totalDistanceMeters;
        this.nodeCount = nodeCount;
        this.avoidedHazardSegments = avoidedHazardSegments;
        this.hazardSegmentsTraversed = hazardSegmentsTraversed;
        this.computationTimeMs = computationTimeMs;
        this.riskScore = riskScore;
    }

    public double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getAvoidedHazardSegments() {
        return avoidedHazardSegments;
    }

    public int getHazardSegmentsTraversed() {
        return hazardSegmentsTraversed;
    }

    public long getComputationTimeMs() {
        return computationTimeMs;
    }

    public double getRiskScore() {
        return riskScore;
    }

    /**
     * Builder for cleaner construction. The constructor params were getting
     * unwieldy.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private double totalDistanceMeters;
        private int nodeCount;
        private int avoidedHazardSegments;
        private int hazardSegmentsTraversed;
        private long computationTimeMs;
        private double riskScore;

        public Builder totalDistanceMeters(double val) {
            this.totalDistanceMeters = val;
            return this;
        }

        public Builder nodeCount(int val) {
            this.nodeCount = val;
            return this;
        }

        public Builder avoidedHazardSegments(int val) {
            this.avoidedHazardSegments = val;
            return this;
        }

        public Builder hazardSegmentsTraversed(int val) {
            this.hazardSegmentsTraversed = val;
            return this;
        }

        public Builder computationTimeMs(long val) {
            this.computationTimeMs = val;
            return this;
        }

        public Builder riskScore(double val) {
            this.riskScore = val;
            return this;
        }

        public RouteMetadata build() {
            return new RouteMetadata(
                    totalDistanceMeters,
                    nodeCount,
                    avoidedHazardSegments,
                    hazardSegmentsTraversed,
                    computationTimeMs,
                    riskScore);
        }
    }
}
