package com.sensorbite.evacroute.routing.graph;

import com.sensorbite.evacroute.geo.FloodZoneProvider;
import com.sensorbite.evacroute.geo.GeometryUtils;
import com.sensorbite.evacroute.model.Edge;
import com.sensorbite.evacroute.model.Node;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Builds a routing graph from raw GeoJSON data.
 * 
 * The main complexity here is handling hazard zones:
 * - For each road segment (edge), we check if it intersects any flood polygon
 * - If it does, we apply a cost multiplier to discourage routing through it
 * 
 * We're using a multiplier approach rather than outright blocking because:
 * 1. It's more realistic - some flooding is passable depending on
 * vehicle/situation
 * 2. It allows finding suboptimal routes when the optimal one is blocked
 * 3. The multiplier can be tuned based on flood severity if we had that data
 */
public class GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(GraphBuilder.class);

    // How much to penalize edges that cross flood zones
    // Higher = more avoidance. 10x means we'll take a detour up to 10x longer
    // to avoid flooding. Infinity would mean complete avoidance.
    private static final double DEFAULT_HAZARD_MULTIPLIER = 10.0;

    private final FloodZoneProvider floodZoneProvider;
    private final double hazardMultiplier;

    public GraphBuilder(FloodZoneProvider floodZoneProvider) {
        this(floodZoneProvider, DEFAULT_HAZARD_MULTIPLIER);
    }

    public GraphBuilder(FloodZoneProvider floodZoneProvider, double hazardMultiplier) {
        this.floodZoneProvider = floodZoneProvider;
        this.hazardMultiplier = hazardMultiplier;
    }

    /**
     * Build a graph from a list of road segments.
     * Each segment is a pair of coordinates representing a road between two points.
     */
    public Graph buildFromSegments(List<RoadSegment> segments) {
        log.info("Building graph from {} road segments", segments.size());
        long startTime = System.currentTimeMillis();

        Graph graph = new Graph();
        List<Geometry> floodZones = floodZoneProvider.getFloodZones();
        log.debug("Loaded {} flood zones for hazard checking", floodZones.size());

        int hazardous = 0;
        int total = 0;

        for (RoadSegment segment : segments) {
            // Create or reuse nodes for the endpoints
            Node startNode = getOrCreateNode(graph, segment.startId, segment.startLat, segment.startLon);
            Node endNode = getOrCreateNode(graph, segment.endId, segment.endLat, segment.endLon);

            double distance = startNode.distanceTo(endNode);

            // Check if this segment crosses any flood zone
            boolean isHazardous = checkHazardIntersection(segment, floodZones);
            if (isHazardous) {
                hazardous++;
            }

            // Create edges in both directions (roads are typically bidirectional)
            Edge forwardEdge = isHazardous
                    ? new Edge(startNode, endNode, distance, hazardMultiplier, true)
                    : new Edge(startNode, endNode, distance);

            Edge reverseEdge = isHazardous
                    ? new Edge(endNode, startNode, distance, hazardMultiplier, true)
                    : new Edge(endNode, startNode, distance);

            graph.addEdge(forwardEdge);
            graph.addEdge(reverseEdge);
            total++;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Graph built in {}ms: {} nodes, {} edges ({} hazardous)",
                elapsed, graph.getNodeCount(), graph.getEdgeCount(), hazardous);

        return graph;
    }

    private Node getOrCreateNode(Graph graph, String id, double lat, double lon) {
        Node existing = graph.getNode(id);
        if (existing != null) {
            return existing;
        }
        Node node = new Node(id, lat, lon);
        graph.addNode(node);
        return node;
    }

    private boolean checkHazardIntersection(RoadSegment segment, List<Geometry> floodZones) {
        if (floodZones.isEmpty()) {
            return false;
        }

        // Create a JTS LineString for the road segment
        LineString line = GeometryUtils.createLine(
                segment.startLon, segment.startLat,
                segment.endLon, segment.endLat);

        // Check against each flood zone
        for (Geometry zone : floodZones) {
            if (line.intersects(zone)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Simple data class for a road segment before it's converted to graph edges.
     */
    public static class RoadSegment {
        public final String startId;
        public final double startLat;
        public final double startLon;
        public final String endId;
        public final double endLat;
        public final double endLon;
        public final Map<String, String> properties;

        public RoadSegment(
                String startId, double startLat, double startLon,
                String endId, double endLat, double endLon,
                Map<String, String> properties) {
            this.startId = startId;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endId = endId;
            this.endLat = endLat;
            this.endLon = endLon;
            this.properties = properties;
        }
    }
}
