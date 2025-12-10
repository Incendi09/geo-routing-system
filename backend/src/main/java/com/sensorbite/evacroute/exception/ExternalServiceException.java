package com.sensorbite.evacroute.exception;

/**
 * Thrown when an external service call fails (Sentinel Hub, etc.)
 * In our mock implementation this won't happen much, but it's here
 * for when we integrate real services.
 * Maps to HTTP 502 (Bad Gateway) or 503 (Service Unavailable).
 */
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;

    public ExternalServiceException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
