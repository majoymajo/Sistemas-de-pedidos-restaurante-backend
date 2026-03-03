package com.restaurant.reportservice.exception;

public class UnsupportedEventVersionException extends RuntimeException {
    public UnsupportedEventVersionException(Integer version) {
        super("Unsupported event version: " + version);
    }
}
