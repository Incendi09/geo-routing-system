package com.sensorbite.evacroute.geo;

import org.locationtech.jts.geom.Geometry;

import java.util.List;

/**
 * Abstraction for loading flood zone (hazard) data.
 * 
 * This interface exists so we can:
 * 1. Use mock data during development and testing
 * 2. Integrate with real services (Sentinel Hub, etc.) in production
 * 3. Cache data or refresh it periodically
 * 
 * The mock implementation loads from a local GeoJSON file.
 * A real implementation might call the Sentinel Hub API, process
 * satellite imagery, or pull from a weather service.
 */
public interface FloodZoneProvider {

    /**
     * Get all current flood zone polygons.
     * Returns JTS Geometry objects that can be used for intersection checks.
     */
    List<Geometry> getFloodZones();

    /**
     * Force a refresh of flood zone data.
     * For mock implementation this is a no-op.
     * For real services this would fetch latest data.
     */
    default void refresh() {
        // Default: no-op
    }

    /**
     * Get the last time flood zone data was updated.
     * Returns epoch millis, or -1 if unknown.
     */
    default long getLastUpdateTime() {
        return -1;
    }
}
