package com.restaurant.orderservice.enums;

import com.restaurant.orderservice.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Branch coverage tests for OrderStatus enum.
 * Tests all branches of the state machine logic.
 */
class OrderStatusBranchTest {

    @Test
    void isValidTransition_fromPendingToInPreparation_returnTrue() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.PENDING, OrderStatus.IN_PREPARATION);
        assertThat(result).isTrue();
    }

    @Test
    void isValidTransition_fromInPreparationToReady_returnTrue() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.IN_PREPARATION, OrderStatus.READY);
        assertThat(result).isTrue();
    }

    @Test
    void isValidTransition_fromPendingToReady_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.PENDING, OrderStatus.READY);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_fromReadyToPending_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.READY, OrderStatus.PENDING);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_fromReadyToInPreparation_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.READY, OrderStatus.IN_PREPARATION);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_fromReadyToReady_returnFalse() {
        // Act & Assert - READY state has no outgoing transitions
        boolean result = OrderStatus.isValidTransition(OrderStatus.READY, OrderStatus.READY);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_fromInPreparationToPending_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.IN_PREPARATION, OrderStatus.PENDING);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_withNullCurrentStatus_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(null, OrderStatus.IN_PREPARATION);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_withNullNewStatus_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(OrderStatus.PENDING, null);
        assertThat(result).isFalse();
    }

    @Test
    void isValidTransition_withBothNull_returnFalse() {
        // Act & Assert
        boolean result = OrderStatus.isValidTransition(null, null);
        assertThat(result).isFalse();
    }

    @Test
    void validateTransition_withValidTransition_doesNotThrow() {
        // Act & Assert
        assertThatCode(() -> OrderStatus.validateTransition(OrderStatus.PENDING, OrderStatus.IN_PREPARATION))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTransition_withInvalidTransition_throwsInvalidStatusTransitionException() {
        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(OrderStatus.PENDING, OrderStatus.READY))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void validateTransition_fromReadyToAnyState_throwsInvalidStatusTransitionException() {
        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(OrderStatus.READY, OrderStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void validateTransition_withNullStates_throwsInvalidStatusTransitionException() {
        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(null, OrderStatus.PENDING))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
