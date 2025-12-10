package com.sensorbite.evacroute.exception;

/**
 * Thrown when the client sends bad input (invalid coordinates, missing params,
 * etc.)
 * Maps to HTTP 400.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
