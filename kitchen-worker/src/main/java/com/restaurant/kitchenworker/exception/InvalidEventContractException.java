package com.restaurant.kitchenworker.exception;

/**
 * Thrown when an integration event does not satisfy required contract fields.
 */
public class InvalidEventContractException extends RuntimeException {
    public InvalidEventContractException(String message) {
        super(message);
    }
}
