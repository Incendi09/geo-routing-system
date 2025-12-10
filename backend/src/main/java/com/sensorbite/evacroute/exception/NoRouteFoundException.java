package com.sensorbite.evacroute.exception;

/**
 * Thrown when no valid route exists between start and end.
 * This could be because:
 * - Start/end are not connected to the road network
 * - All paths are blocked by hazards (when we're in strict mode)
 * - The graph simply doesn't have connectivity
 * 
 * Maps to HTTP 404 (debatable - could also be 422, but 404 feels right
 * since the requested resource, a route, doesn't exist).
 */
public class NoRouteFoundException extends RuntimeException {

    public NoRouteFoundException(String message) {
        super(message);
    }
}
