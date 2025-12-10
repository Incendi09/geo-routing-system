package com.sensorbite.evacroute.service;

import com.sensorbite.evacroute.exception.InvalidInputException;
import com.sensorbite.evacroute.exception.NoRouteFoundException;
import com.sensorbite.evacroute.geo.FloodZoneProvider;
import com.sensorbite.evacroute.geo.GeoJsonLoader;
import com.sensorbite.evacroute.model.Coordinate;
import com.sensorbite.evacroute.model.Node;
import com.sensorbite.evacroute.model.RouteMetadata;
import com.sensorbite.evacroute.model.RouteResult;
import com.sensorbite.evacroute.routing.algorithms.DijkstraRouter;
import com.sensorbite.evacroute.routing.algorithms.PathFinder;
import com.sensorbite.evacroute.routing.graph.Graph;
import com.sensorbite.evacroute.routing.graph.GraphBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main service that orchestrates the routing process.
 * 
 * This is the high-level entry point that:
 * 1. Loads and maintains the road graph
 * 2. Handles coordinate snapping (finding nearest nodes)
 * 3. Runs the routing algorithm
 * 4. Assembles the result with metadata
 * 
 * Thread-safe after initialization - the graph is immutable once built.
 */
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    // Max distance (meters) from input coordinate to nearest road node
    // If the user specifies a point too far from any road, we reject it
    private static final double MAX_SNAP_DISTANCE = 500.0;

    private final Graph graph;
    private final PathFinder pathFinder;
    private final int totalHazardEdges; // For computing avoided segments

    /**
     * Create a routing service with pre-loaded data.
     */
    public RoutingService(String roadsPath, FloodZoneProvider floodZoneProvider) {
        log.info("Initializing RoutingService...");
        long startTime = System.currentTimeMillis();

        // Load road segments from GeoJSON
        GeoJsonLoader loader = new GeoJsonLoader();
        var segments = loader.loadRoadNetwork(roadsPath);

        // Build graph with hazard info
        GraphBuilder builder = new GraphBuilder(floodZoneProvider);
        this.graph = builder.buildFromSegments(segments);

        // Count hazardous edges for later reporting
        this.totalHazardEdges = (int) graph.getAllEdges().stream()
                .filter(e -> e.isInHazardZone())
                .count();

        // Default to Dijkstra, but this could be configurable
        this.pathFinder = new DijkstraRouter();

        log.info("RoutingService initialized in {}ms. Graph has {} nodes, {} edges ({} hazardous)",
                System.currentTimeMillis() - startTime,
                graph.getNodeCount(), graph.getEdgeCount(), totalHazardEdges);
    }

    /**
     * Compute an evacuation route from start to end.
     * 
     * @param start Starting coordinate
     * @param end   Ending coordinate
     * @return RouteResult with GeoJSON geometry and metadata
     * @throws InvalidInputException if coordinates are invalid or too far from
     *                               roads
     * @throws NoRouteFoundException if no path exists between the points
     */
    public RouteResult computeRoute(Coordinate start, Coordinate end) {
        log.info("Computing route from {} to {}", start, end);
        long startTime = System.currentTimeMillis();

        // Find nearest nodes to the input coordinates
        Node startNode = snapToNode(start, "start");
        Node endNode = snapToNode(end, "end");

        if (startNode.equals(endNode)) {
            throw new InvalidInputException("Start and end points resolve to the same road node. " +
                    "Please provide more distant locations.");
        }

        // Run the routing algorithm
        PathFinder.PathResult pathResult = pathFinder.findPath(graph, startNode, endNode);

        if (pathResult == null || pathResult.isEmpty()) {
            throw new NoRouteFoundException(String.format(
                    "No route found from %s to %s. The locations may not be connected.",
                    start, end));
        }

        long computeTime = System.currentTimeMillis() - startTime;

        // Calculate risk score: 0 = completely safe, 1 = all hazardous
        int hazardInPath = pathResult.getHazardEdgesInPath();
        int totalInPath = pathResult.getEdges().size();
        double riskScore = totalInPath > 0 ? (double) hazardInPath / totalInPath : 0.0;

        // Avoided segments = hazard segments we considered but didn't use
        int avoided = pathResult.getHazardEdgesConsidered() - hazardInPath;

        RouteMetadata meta = RouteMetadata.builder()
                .totalDistanceMeters(pathResult.getTotalDistance())
                .nodeCount(pathResult.getPath().size())
                .avoidedHazardSegments(avoided)
                .hazardSegmentsTraversed(hazardInPath)
                .computationTimeMs(computeTime)
                .riskScore(riskScore)
                .build();

        log.info("Route computed: {:.1f}m, {} nodes, {}ms, risk={:.2f}",
                pathResult.getTotalDistance(), pathResult.getPath().size(),
                computeTime, riskScore);

        return RouteResult.fromPath(pathResult.getPath(), meta);
    }

    /**
     * Find the nearest road node to a coordinate.
     * Throws if the coordinate is too far from any road.
     */
    private Node snapToNode(Coordinate coord, String label) {
        Node nearest = graph.findNearestNodeWithinDistance(coord, MAX_SNAP_DISTANCE);

        if (nearest == null) {
            throw new InvalidInputException(String.format(
                    "The %s coordinate (%s) is more than %.0fm from any road in the network. " +
                            "Please specify a location closer to a road.",
                    label, coord, MAX_SNAP_DISTANCE));
        }

        double snapDist = coord.distanceTo(nearest.getPosition());
        log.debug("Snapped {} {} to node {} ({:.1f}m away)",
                label, coord, nearest.getId(), snapDist);

        return nearest;
    }

    /**
     * Get info about the loaded graph (for debugging/monitoring).
     */
    public GraphInfo getGraphInfo() {
        return new GraphInfo(
                graph.getNodeCount(),
                graph.getEdgeCount(),
                totalHazardEdges);
    }

    public record GraphInfo(int nodeCount, int edgeCount, int hazardEdgeCount) {
    }
}
