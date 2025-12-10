package com.sensorbite.evacroute.geo;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

/**
 * Utility methods for geometry operations using JTS.
 * 
 * JTS (Java Topology Suite) is the de facto standard for geometry
 * operations in Java. We use it for:
 * - Creating geometries from coordinates
 * - Intersection checks (does road segment cross flood zone?)
 * - Distance calculations (though we mostly use Haversine in Coordinate)
 * 
 * Note: JTS uses planar geometry, not spherical. For our use case
 * (small geographic areas like a city), the distortion is negligible.
 * For global routing, we'd need to handle projections properly.
 */
public final class GeometryUtils {

    private static final GeometryFactory FACTORY = new GeometryFactory();

    private GeometryUtils() {
        // Utility class, no instantiation
    }

    /**
     * Create a JTS Point from lon/lat.
     * Note: JTS uses (x, y) which maps to (longitude, latitude).
     */
    public static Point createPoint(double lon, double lat) {
        return FACTORY.createPoint(new org.locationtech.jts.geom.Coordinate(lon, lat));
    }

    /**
     * Create a LineString from two points.
     */
    public static LineString createLine(double lon1, double lat1, double lon2, double lat2) {
        org.locationtech.jts.geom.Coordinate[] coords = new org.locationtech.jts.geom.Coordinate[] {
                new org.locationtech.jts.geom.Coordinate(lon1, lat1),
                new org.locationtech.jts.geom.Coordinate(lon2, lat2)
        };
        return FACTORY.createLineString(coords);
    }

    /**
     * Create a Polygon from a list of coordinate pairs.
     * The last coordinate should equal the first to close the ring.
     */
    public static Polygon createPolygon(double[][] coords) {
        org.locationtech.jts.geom.Coordinate[] jtsCoords = new org.locationtech.jts.geom.Coordinate[coords.length];

        for (int i = 0; i < coords.length; i++) {
            // coords[i] = [lon, lat]
            jtsCoords[i] = new org.locationtech.jts.geom.Coordinate(coords[i][0], coords[i][1]);
        }

        LinearRing ring = FACTORY.createLinearRing(jtsCoords);
        return FACTORY.createPolygon(ring);
    }

    /**
     * Create a Polygon from a list of coordinates in GeoJSON format.
     * GeoJSON uses [longitude, latitude] order.
     */
    public static Polygon createPolygonFromGeoJson(java.util.List<double[]> coords) {
        org.locationtech.jts.geom.Coordinate[] jtsCoords = new org.locationtech.jts.geom.Coordinate[coords.size()];

        for (int i = 0; i < coords.size(); i++) {
            double[] coord = coords.get(i);
            jtsCoords[i] = new org.locationtech.jts.geom.Coordinate(coord[0], coord[1]);
        }

        // Ensure the ring is closed
        if (!jtsCoords[0].equals(jtsCoords[jtsCoords.length - 1])) {
            org.locationtech.jts.geom.Coordinate[] closedCoords = new org.locationtech.jts.geom.Coordinate[jtsCoords.length
                    + 1];
            System.arraycopy(jtsCoords, 0, closedCoords, 0, jtsCoords.length);
            closedCoords[closedCoords.length - 1] = jtsCoords[0];
            jtsCoords = closedCoords;
        }

        LinearRing ring = FACTORY.createLinearRing(jtsCoords);
        return FACTORY.createPolygon(ring);
    }

    /**
     * Check if a line intersects a polygon.
     */
    public static boolean lineIntersectsPolygon(LineString line, Polygon polygon) {
        return line.intersects(polygon);
    }

    /**
     * Get the geometry factory for direct use when needed.
     */
    public static GeometryFactory getFactory() {
        return FACTORY;
    }
}
