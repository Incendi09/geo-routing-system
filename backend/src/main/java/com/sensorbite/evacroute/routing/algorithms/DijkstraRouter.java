package com.sensorbite.evacroute.routing.algorithms;

import com.sensorbite.evacroute.model.Edge;
import com.sensorbite.evacroute.model.Node;
import com.sensorbite.evacroute.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Modified Dijkstra's shortest path algorithm with hazard awareness.
 * 
 * The key modification is that we use Edge.getEffectiveCost() instead of
 * raw distance. This means edges crossing flood zones have inflated costs,
 * causing the algorithm to prefer safer routes even if they're longer.
 * 
 * Implementation notes:
 * - Uses a priority queue (min-heap) for efficiency
 * - Standard Dijkstra complexity: O((V + E) log V) with binary heap
 * - For our road network size, this is plenty fast
 */
public class DijkstraRouter implements PathFinder {

    private static final Logger log = LoggerFactory.getLogger(DijkstraRouter.class);

    @Override
    public PathResult findPath(Graph graph, Node start, Node end) {
        log.debug("Finding path from {} to {}", start.getId(), end.getId());

        // Distance from start to each node (using effective cost, not raw distance)
        Map<String, Double> costs = new HashMap<>();

        // Previous node in the optimal path
        Map<String, String> previousNode = new HashMap<>();

        // Edge used to reach each node (needed to track hazard info)
        Map<String, Edge> previousEdge = new HashMap<>();

        // Track which hazard edges we considered vs. used
        int hazardEdgesConsidered = 0;

        // Priority queue: [cost, nodeId]
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        Map<String, Boolean> visited = new HashMap<>();

        // Initialize
        costs.put(start.getId(), 0.0);
        pq.offer(new double[] { 0.0, nodeIdToDouble(start.getId()) });

        // We need a way to map node IDs back and forth for the priority queue
        // Since we're using String IDs, we'll just use a separate map
        Map<Double, String> idMapping = new HashMap<>();
        idMapping.put(nodeIdToDouble(start.getId()), start.getId());

        while (!pq.isEmpty()) {
            double[] current = pq.poll();
            double currentCost = current[0];
            String currentId = idMapping.get(current[1]);

            // Skip if already processed (can happen with duplicate entries in PQ)
            if (visited.getOrDefault(currentId, false)) {
                continue;
            }
            visited.put(currentId, true);

            // Found the destination!
            if (currentId.equals(end.getId())) {
                log.debug("Reached destination after visiting {} nodes", visited.size());
                return reconstructPath(graph, start, end, previousNode, previousEdge, hazardEdgesConsidered);
            }

            // Explore neighbors
            for (Edge edge : graph.getEdgesFrom(currentId)) {
                String neighborId = edge.getTarget().getId();

                if (visited.getOrDefault(neighborId, false)) {
                    continue;
                }

                if (edge.isInHazardZone()) {
                    hazardEdgesConsidered++;
                }

                double newCost = currentCost + edge.getEffectiveCost();
                double oldCost = costs.getOrDefault(neighborId, Double.MAX_VALUE);

                if (newCost < oldCost) {
                    costs.put(neighborId, newCost);
                    previousNode.put(neighborId, currentId);
                    previousEdge.put(neighborId, edge);

                    double neighborKey = nodeIdToDouble(neighborId);
                    idMapping.put(neighborKey, neighborId);
                    pq.offer(new double[] { newCost, neighborKey });
                }
            }
        }

        // No path found
        log.warn("No path found from {} to {} after visiting {} nodes",
                start.getId(), end.getId(), visited.size());
        return null;
    }

    /**
     * Reconstruct the path from start to end using the previous node map.
     */
    private PathResult reconstructPath(
            Graph graph,
            Node start,
            Node end,
            Map<String, String> previousNode,
            Map<String, Edge> previousEdge,
            int hazardEdgesConsidered) {

        List<Node> path = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        String current = end.getId();

        // Walk backwards from end to start
        while (current != null) {
            path.add(graph.getNode(current));
            Edge edge = previousEdge.get(current);
            if (edge != null) {
                edges.add(edge);
            }
            current = previousNode.get(current);
        }

        // Reverse to get start -> end order
        Collections.reverse(path);
        Collections.reverse(edges);

        // Calculate totals
        double totalDistance = 0;
        double totalCost = 0;
        int hazardEdgesInPath = 0;

        for (Edge edge : edges) {
            totalDistance += edge.getDistance();
            totalCost += edge.getEffectiveCost();
            if (edge.isInHazardZone()) {
                hazardEdgesInPath++;
            }
        }

        log.debug("Path reconstructed: {} nodes, {:.1f}m distance, {} hazard edges used, {} hazard edges avoided",
                path.size(), totalDistance, hazardEdgesInPath, hazardEdgesConsidered - hazardEdgesInPath);

        return new PathResult(
                path,
                edges,
                totalDistance,
                totalCost,
                hazardEdgesInPath,
                hazardEdgesConsidered);
    }

    /**
     * Hash string node ID to a double for use in the priority queue.
     * This is a workaround since Java's PriorityQueue doesn't efficiently
     * support decreaseKey operations. We use the hash as a secondary key.
     */
    private double nodeIdToDouble(String nodeId) {
        return nodeId.hashCode();
    }
}
