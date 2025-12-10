package com.sensorbite.evacroute.routing.graph;

import com.sensorbite.evacroute.model.Coordinate;
import com.sensorbite.evacroute.model.Edge;
import com.sensorbite.evacroute.model.Node;

import java.util.*;

/**
 * Represents the road network as a graph structure.
 * Uses adjacency list representation - good balance of memory and speed
 * for the kinds of graphs we're dealing with (sparse road networks).
 * 
 * The graph is built once at startup and then queried during routing.
 * It's effectively immutable after construction, which makes it thread-safe.
 */
public class Graph {

    private final Map<String, Node> nodes;
    private final Map<String, List<Edge>> adjacencyList;

    public Graph() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
    }

    /**
     * Add a node to the graph. Idempotent - adding the same node twice
     * just overwrites (they should be identical anyway).
     */
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.computeIfAbsent(node.getId(), k -> new ArrayList<>());
    }

    /**
     * Add an edge. We assume the source and target nodes already exist.
     * For an undirected road network, call this twice with reversed direction.
     */
    public void addEdge(Edge edge) {
        String sourceId = edge.getSource().getId();
        adjacencyList.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(edge);
    }

    /**
     * Get a node by ID, or null if not found.
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * Get all outgoing edges from a node.
     */
    public List<Edge> getEdgesFrom(String nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public List<Edge> getEdgesFrom(Node node) {
        return getEdgesFrom(node.getId());
    }

    /**
     * Get all nodes in the graph.
     */
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Get all edges in the graph.
     */
    public List<Edge> getAllEdges() {
        List<Edge> allEdges = new ArrayList<>();
        for (List<Edge> edges : adjacencyList.values()) {
            allEdges.addAll(edges);
        }
        return allEdges;
    }

    /**
     * Find the node closest to a given coordinate.
     * This is a naive O(n) search - for a production system with lots of nodes
     * we'd want a spatial index (R-tree, KD-tree, etc.), but for our demo
     * data size this is plenty fast.
     */
    public Node findNearestNode(Coordinate coord) {
        Node nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Node node : nodes.values()) {
            double dist = coord.distanceTo(node.getPosition());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = node;
            }
        }

        return nearest;
    }

    /**
     * Find the nearest node within a maximum distance (in meters).
     * Returns null if nothing is close enough.
     */
    public Node findNearestNodeWithinDistance(Coordinate coord, double maxDistanceMeters) {
        Node nearest = findNearestNode(coord);
        if (nearest != null) {
            double dist = coord.distanceTo(nearest.getPosition());
            if (dist <= maxDistanceMeters) {
                return nearest;
            }
        }
        return null;
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public String toString() {
        return "Graph{nodes=" + getNodeCount() + ", edges=" + getEdgeCount() + "}";
    }
}
