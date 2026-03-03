package com.restaurant.reportservice.exception;

public class InvalidEventContractException extends RuntimeException {
    public InvalidEventContractException(String message) {
        super(message);
    }

    public InvalidEventContractException(String message, Throwable cause) {
        super(message, cause);
    }
}
