package com.restaurant.orderservice.service.command;

/**
 * Command abstraction for order-related operations that must execute as a unit.
 */
@FunctionalInterface
public interface OrderCommand {

    void execute();
}
