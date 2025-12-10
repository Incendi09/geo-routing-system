package com.sensorbite.evacroute.controller;

import com.sensorbite.evacroute.exception.InvalidInputException;
import com.sensorbite.evacroute.model.Coordinate;
import com.sensorbite.evacroute.model.RouteResult;
import com.sensorbite.evacroute.service.RoutingService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST endpoint for evacuation routing.
 * 
 * Provides a simple GET endpoint that takes start/end coordinates
 * and returns a GeoJSON route with metadata.
 */
@Path("/api/evac")
@Produces(MediaType.APPLICATION_JSON)
public class EvacuationRouteController {

    private static final Logger log = LoggerFactory.getLogger(EvacuationRouteController.class);

    private final RoutingService routingService;

    public EvacuationRouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Compute an evacuation route.
     * 
     * GET /api/evac/route?start=lat,lon&end=lat,lon
     * 
     * @param start Start coordinate in "lat,lon" format
     * @param end   End coordinate in "lat,lon" format
     * @return GeoJSON Feature with route LineString and metadata
     */
    @GET
    @Path("/route")
    public Response getRoute(
            @QueryParam("start") String start,
            @QueryParam("end") String end) {

        log.info("Route request: start={}, end={}", start, end);

        // Validate input
        if (start == null || start.isBlank()) {
            throw new InvalidInputException("Missing required query parameter: start");
        }
        if (end == null || end.isBlank()) {
            throw new InvalidInputException("Missing required query parameter: end");
        }

        // Parse coordinates (Coordinate.fromString throws InvalidInputException on bad
        // input)
        Coordinate startCoord;
        Coordinate endCoord;

        try {
            startCoord = Coordinate.fromString(start);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid start coordinate: " + e.getMessage());
        }

        try {
            endCoord = Coordinate.fromString(end);
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid end coordinate: " + e.getMessage());
        }

        // Compute route
        RouteResult result = routingService.computeRoute(startCoord, endCoord);

        return Response.ok(result).build();
    }

    /**
     * Health check endpoint.
     * Returns basic info about the routing service.
     */
    @GET
    @Path("/health")
    public Response health() {
        var info = routingService.getGraphInfo();
        String json = String.format(
                "{\"status\":\"ok\",\"graphNodes\":%d,\"graphEdges\":%d,\"hazardEdges\":%d}",
                info.nodeCount(), info.edgeCount(), info.hazardEdgeCount());
        return Response.ok(json).build();
    }
}
