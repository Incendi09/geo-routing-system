package com.sensorbite.evacroute.routing;

import com.sensorbite.evacroute.geo.FloodZoneProvider;
import com.sensorbite.evacroute.model.Edge;
import com.sensorbite.evacroute.model.Node;
import com.sensorbite.evacroute.routing.algorithms.DijkstraRouter;
import com.sensorbite.evacroute.routing.algorithms.PathFinder;
import com.sensorbite.evacroute.routing.graph.Graph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Dijkstra routing algorithm.
 * 
 * We test several scenarios:
 * 1. Simple path without hazards
 * 2. Path forced to detour due to hazards
 * 3. No path exists
 * 4. Multiple paths with different costs
 */
class DijkstraRouterTest {

    private DijkstraRouter router;

    @BeforeEach
    void setUp() {
        router = new DijkstraRouter();
    }

    @Test
    @DisplayName("Should find direct path when no hazards exist")
    void testSimplePathNoHazards() {
        // Build a simple A -> B -> C graph
        Graph graph = new Graph();

        Node a = new Node("A", 52.23, 21.01);
        Node b = new Node("B", 52.24, 21.01);
        Node c = new Node("C", 52.25, 21.01);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);

        // A <-> B (bidirectional)
        graph.addEdge(new Edge(a, b, 100));
        graph.addEdge(new Edge(b, a, 100));

        // B <-> C (bidirectional)
        graph.addEdge(new Edge(b, c, 100));
        graph.addEdge(new Edge(c, b, 100));

        PathFinder.PathResult result = router.findPath(graph, a, c);

        assertNotNull(result, "Should find a path");
        assertFalse(result.isEmpty(), "Path should not be empty");
        assertEquals(3, result.getPath().size(), "Path should have 3 nodes (A, B, C)");
        assertEquals("A", result.getPath().get(0).getId());
        assertEquals("C", result.getPath().get(2).getId());
        assertEquals(200.0, result.getTotalDistance(), 0.1);
        assertEquals(0, result.getHazardEdgesInPath());
    }

    @Test
    @DisplayName("Should prefer longer safe path over shorter hazardous path")
    void testAvoidHazardousPath() {
        Graph graph = new Graph();

        // Diamond-shaped graph:
        // B (through flood zone)
        // / \
        // A D
        // \ /
        // C (safe but longer)

        Node a = new Node("A", 52.23, 21.01);
        Node b = new Node("B", 52.24, 21.02); // Hazard zone
        Node c = new Node("C", 52.22, 21.02); // Safe route
        Node d = new Node("D", 52.23, 21.03);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        graph.addNode(d);

        // A -> B -> D: short (200m) but hazardous
        graph.addEdge(new Edge(a, b, 100, 10.0, true)); // 10x penalty
        graph.addEdge(new Edge(b, d, 100, 10.0, true));

        // A -> C -> D: longer (300m) but safe
        graph.addEdge(new Edge(a, c, 150));
        graph.addEdge(new Edge(c, d, 150));

        PathFinder.PathResult result = router.findPath(graph, a, d);

        assertNotNull(result);
        assertEquals(3, result.getPath().size());

        // Should take the safe A -> C -> D route
        assertEquals("A", result.getPath().get(0).getId());
        assertEquals("C", result.getPath().get(1).getId());
        assertEquals("D", result.getPath().get(2).getId());

        // Actual distance is 300m (not the 200m hazardous route)
        assertEquals(300.0, result.getTotalDistance(), 0.1);
        assertEquals(0, result.getHazardEdgesInPath());
    }

    @Test
    @DisplayName("Should return null when no path exists")
    void testNoPathExists() {
        Graph graph = new Graph();

        // Disconnected nodes
        Node a = new Node("A", 52.23, 21.01);
        Node b = new Node("B", 52.24, 21.01);

        graph.addNode(a);
        graph.addNode(b);
        // No edges connecting them

        PathFinder.PathResult result = router.findPath(graph, a, b);

        assertNull(result, "Should return null when no path exists");
    }

    @Test
    @DisplayName("Should find shortest path among multiple routes")
    void testMultipleRoutes() {
        Graph graph = new Graph();

        // Multiple paths from A to D:
        // A -> B -> D (200m)
        // A -> C -> D (250m)
        // A -> D direct (would be 170m if existed)

        Node a = new Node("A", 52.23, 21.01);
        Node b = new Node("B", 52.24, 21.01);
        Node c = new Node("C", 52.22, 21.01);
        Node d = new Node("D", 52.23, 21.02);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        graph.addNode(d);

        // Shorter path through B
        graph.addEdge(new Edge(a, b, 100));
        graph.addEdge(new Edge(b, d, 100));

        // Longer path through C
        graph.addEdge(new Edge(a, c, 120));
        graph.addEdge(new Edge(c, d, 130));

        PathFinder.PathResult result = router.findPath(graph, a, d);

        assertNotNull(result);
        assertEquals(3, result.getPath().size());
        assertEquals("B", result.getPath().get(1).getId(), "Should take path through B");
        assertEquals(200.0, result.getTotalDistance(), 0.1);
    }

    @Test
    @DisplayName("Should handle same start and end node")
    void testSameStartEnd() {
        Graph graph = new Graph();

        Node a = new Node("A", 52.23, 21.01);
        graph.addNode(a);

        PathFinder.PathResult result = router.findPath(graph, a, a);

        // This is an edge case - should return a path with just the start node
        assertNotNull(result);
        assertEquals(1, result.getPath().size());
        assertEquals("A", result.getPath().get(0).getId());
        assertEquals(0.0, result.getTotalDistance(), 0.1);
    }

    @Test
    @DisplayName("Should track hazard edges traversed")
    void testHazardEdgeTracking() {
        Graph graph = new Graph();

        // Only path available goes through one hazard zone
        Node a = new Node("A", 52.23, 21.01);
        Node b = new Node("B", 52.24, 21.01);
        Node c = new Node("C", 52.25, 21.01);

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);

        // A -> B is safe
        graph.addEdge(new Edge(a, b, 100));

        // B -> C is hazardous
        graph.addEdge(new Edge(b, c, 100, 5.0, true));

        PathFinder.PathResult result = router.findPath(graph, a, c);

        assertNotNull(result);
        assertEquals(1, result.getHazardEdgesInPath(), "Should count the hazard edge traversed");
    }
}
