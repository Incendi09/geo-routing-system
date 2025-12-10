package com.sensorbite.evacroute.exception;

/**
 * Thrown when we can't load required data (road network, flood zones, etc.)
 * Maps to HTTP 500 since this is a server-side configuration issue.
 */
public class DataLoadException extends RuntimeException {

    public DataLoadException(String message) {
        super(message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
