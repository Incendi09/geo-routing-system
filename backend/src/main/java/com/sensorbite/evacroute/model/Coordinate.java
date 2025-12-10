package com.sensorbite.evacroute.model;

import java.util.Objects;

/**
 * Represents a geographic coordinate (WGS84).
 * Immutable value object - no setters, just construct and use.
 */
public class Coordinate {

    private final double latitude;
    private final double longitude;

    public Coordinate(double latitude, double longitude) {
        // Basic sanity checks - lat should be -90 to 90, lon -180 to 180
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got: " + longitude);
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Calculate approximate distance to another coordinate using Haversine formula.
     * Returns distance in meters. Good enough for routing purposes.
     */
    public double distanceTo(Coordinate other) {
        final double R = 6371000; // Earth's radius in meters

        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                        * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Parse from "lat,lon" string format (what we get from query params).
     */
    public static Coordinate fromString(String latLon) {
        if (latLon == null || latLon.isBlank()) {
            throw new IllegalArgumentException("Coordinate string cannot be empty");
        }

        String[] parts = latLon.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected format 'lat,lon', got: " + latLon);
        }

        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            return new Coordinate(lat, lon);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate values: " + latLon);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Coordinate that = (Coordinate) o;
        // Using a small epsilon for floating point comparison
        return Math.abs(latitude - that.latitude) < 1e-9
                && Math.abs(longitude - that.longitude) < 1e-9;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("%.6f,%.6f", latitude, longitude);
    }
}
