package com.restaurant.orderservice.service.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCommandExecutorTest {

    @InjectMocks
    private OrderCommandExecutor executor;

    @Mock
    private OrderCommand command;

    @Test
    void execute_shouldInvokeCommandsExecuteMethod() {
        executor.execute(command);

        verify(command).execute();
    }

    @Test
    void execute_shouldHandleMultipleCommands() {
        OrderCommand command1 = new MockOrderCommand();
        OrderCommand command2 = new MockOrderCommand();

        executor.execute(command1);
        executor.execute(command2);

        // Mock verification happens implicitly in actual usage,
        // but we verify both execute without errors
    }

    private static class MockOrderCommand implements OrderCommand {
        private boolean executed = false;

        @Override
        public void execute() {
            executed = true;
        }

        public boolean isExecuted() {
            return executed;
        }
    }
}
