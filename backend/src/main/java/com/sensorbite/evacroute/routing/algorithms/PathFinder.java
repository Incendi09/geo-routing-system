package com.sensorbite.evacroute.routing.algorithms;

import com.sensorbite.evacroute.model.Edge;
import com.sensorbite.evacroute.model.Node;
import com.sensorbite.evacroute.routing.graph.Graph;

import java.util.List;

/**
 * Interface for path-finding algorithms.
 * Keeping this abstract lets us swap implementations later
 * (A*, bidirectional Dijkstra, etc.) without changing the rest of the code.
 */
public interface PathFinder {

    /**
     * Find a path from start to end node.
     * 
     * @param graph The road network graph
     * @param start Starting node
     * @param end   Destination node
     * @return PathResult containing the path and metadata, or null if no path
     *         exists
     */
    PathResult findPath(Graph graph, Node start, Node end);

    /**
     * Result of a path-finding operation.
     * Includes the path itself plus some stats about what happened.
     */
    class PathResult {
        private final List<Node> path;
        private final List<Edge> edges;
        private final double totalDistance;
        private final double totalCost;
        private final int hazardEdgesInPath;
        private final int hazardEdgesConsidered;

        public PathResult(
                List<Node> path,
                List<Edge> edges,
                double totalDistance,
                double totalCost,
                int hazardEdgesInPath,
                int hazardEdgesConsidered) {
            this.path = path;
            this.edges = edges;
            this.totalDistance = totalDistance;
            this.totalCost = totalCost;
            this.hazardEdgesInPath = hazardEdgesInPath;
            this.hazardEdgesConsidered = hazardEdgesConsidered;
        }

        public List<Node> getPath() {
            return path;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public int getHazardEdgesInPath() {
            return hazardEdgesInPath;
        }

        public int getHazardEdgesConsidered() {
            return hazardEdgesConsidered;
        }

        public boolean isEmpty() {
            return path == null || path.isEmpty();
        }
    }
}
