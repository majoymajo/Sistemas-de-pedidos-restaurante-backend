package com.restaurant.orderservice.exception;

/**
 * Exception thrown when the order event cannot be published to the message broker.
 */
public class EventPublicationException extends RuntimeException {

    public EventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
