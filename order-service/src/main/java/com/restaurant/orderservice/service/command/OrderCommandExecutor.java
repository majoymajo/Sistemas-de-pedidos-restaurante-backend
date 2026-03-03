package com.restaurant.orderservice.service.command;

import org.springframework.stereotype.Component;

/**
 * Invoker for order commands.
 */
@Component
public class OrderCommandExecutor {

    public void execute(OrderCommand command) {
        command.execute();
    }
}
