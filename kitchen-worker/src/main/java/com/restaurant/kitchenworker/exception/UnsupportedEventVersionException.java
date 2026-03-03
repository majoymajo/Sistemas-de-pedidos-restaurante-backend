package com.restaurant.kitchenworker.exception;

/**
 * Thrown when an event version is not supported by the current consumer.
 */
public class UnsupportedEventVersionException extends RuntimeException {
    public UnsupportedEventVersionException(Integer version) {
        super("Unsupported order.placed event version: " + version);
    }
}
