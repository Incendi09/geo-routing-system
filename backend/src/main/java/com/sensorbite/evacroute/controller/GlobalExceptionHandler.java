package com.sensorbite.evacroute.controller;

import com.sensorbite.evacroute.exception.DataLoadException;
import com.sensorbite.evacroute.exception.ExternalServiceException;
import com.sensorbite.evacroute.exception.InvalidInputException;
import com.sensorbite.evacroute.exception.NoRouteFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler that maps exceptions to HTTP responses.
 * 
 * This keeps error handling logic out of controllers and ensures
 * consistent error response format across all endpoints.
 */
@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Throwable exception) {

        if (exception instanceof InvalidInputException) {
            log.warn("Invalid input: {}", exception.getMessage());
            return buildResponse(Response.Status.BAD_REQUEST, exception.getMessage());
        }

        if (exception instanceof NoRouteFoundException) {
            log.warn("No route found: {}", exception.getMessage());
            return buildResponse(Response.Status.NOT_FOUND, exception.getMessage());
        }

        if (exception instanceof DataLoadException) {
            log.error("Data load error: {}", exception.getMessage(), exception);
            return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Server configuration error: " + exception.getMessage());
        }

        if (exception instanceof ExternalServiceException ese) {
            log.error("External service failure ({}): {}", ese.getServiceName(), ese.getMessage(), ese);
            return buildResponse(Response.Status.BAD_GATEWAY,
                    "External service unavailable: " + ese.getServiceName());
        }

        // Catch-all for unexpected errors
        log.error("Unexpected error: {}", exception.getMessage(), exception);
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private Response buildResponse(Response.Status status, String message) {
        ErrorResponse error = new ErrorResponse(
                status.getStatusCode(),
                status.getReasonPhrase(),
                message);

        return Response.status(status)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Standard error response format.
     */
    public record ErrorResponse(
            int status,
            String error,
            String message) {
    }
}
