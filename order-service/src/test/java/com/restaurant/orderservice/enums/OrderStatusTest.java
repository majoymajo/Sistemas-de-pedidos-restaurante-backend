package com.restaurant.orderservice.enums;

import com.restaurant.orderservice.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderStatus – State machine transition tests")
class OrderStatusTest {

    // ── UNIT-DOM-01 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("UNIT-DOM-01: PENDING → IN_PREPARATION should be valid")
    void validateTransition_pendingToInPreparation_shouldBeValid() {
        // Arrange
        OrderStatus current = OrderStatus.PENDING;
        OrderStatus next = OrderStatus.IN_PREPARATION;

        // Act & Assert
        assertThatCode(() -> OrderStatus.validateTransition(current, next))
                .doesNotThrowAnyException();
    }

    // ── UNIT-DOM-02 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("UNIT-DOM-02: IN_PREPARATION → READY should be valid")
    void validateTransition_inPreparationToReady_shouldBeValid() {
        // Arrange
        OrderStatus current = OrderStatus.IN_PREPARATION;
        OrderStatus next = OrderStatus.READY;

        // Act & Assert
        assertThatCode(() -> OrderStatus.validateTransition(current, next))
                .doesNotThrowAnyException();
    }

    // ── UNIT-DOM-03 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("UNIT-DOM-03: PENDING → READY should be invalid (skip state)")
    void validateTransition_pendingToReady_shouldBeInvalid() {
        // Arrange
        OrderStatus current = OrderStatus.PENDING;
        OrderStatus next = OrderStatus.READY;

        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(current, next))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── UNIT-DOM-04 ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("UNIT-DOM-04: READY is a terminal state – no outgoing transitions")
    class ReadyTerminalStateTests {

        @Test
        @DisplayName("READY → IN_PREPARATION should be invalid (reverse)")
        void validateTransition_readyToInPreparation_shouldBeInvalid() {
            // Arrange
            OrderStatus current = OrderStatus.READY;
            OrderStatus next = OrderStatus.IN_PREPARATION;

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(current, next))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("READY → PENDING should be invalid (reverse)")
        void validateTransition_readyToPending_shouldBeInvalid() {
            // Arrange
            OrderStatus current = OrderStatus.READY;
            OrderStatus next = OrderStatus.PENDING;

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(current, next))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("READY → READY should be invalid (self-transition on terminal)")
        void validateTransition_readyToReady_shouldBeInvalid() {
            // Arrange
            OrderStatus current = OrderStatus.READY;
            OrderStatus next = OrderStatus.READY;

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(current, next))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }

    // ── Additional reverse-transition coverage ───────────────────────────────

    @Test
    @DisplayName("IN_PREPARATION → PENDING should be invalid (reverse)")
    void validateTransition_inPreparationToPending_shouldBeInvalid() {
        // Arrange
        OrderStatus current = OrderStatus.IN_PREPARATION;
        OrderStatus next = OrderStatus.PENDING;

        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(current, next))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── Self-transition coverage ─────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {0} should be invalid (self-transition)")
    @EnumSource(OrderStatus.class)
    @DisplayName("Self-transitions should be invalid for every status")
    void validateTransition_sameState_shouldBeInvalid(OrderStatus status) {
        // Arrange – current and next are identical

        // Act & Assert
        assertThatThrownBy(() -> OrderStatus.validateTransition(status, status))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // ── Null-safety coverage ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Null arguments should throw InvalidStatusTransitionException")
    class NullArgumentTests {

        @ParameterizedTest(name = "null → {0} should be invalid")
        @EnumSource(OrderStatus.class)
        @DisplayName("null as current status should be invalid")
        void validateTransition_nullCurrent_shouldBeInvalid(OrderStatus next) {
            // Arrange – current is null

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(null, next))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @ParameterizedTest(name = "{0} → null should be invalid")
        @EnumSource(OrderStatus.class)
        @DisplayName("null as new status should be invalid")
        void validateTransition_nullNext_shouldBeInvalid(OrderStatus current) {
            // Arrange – next is null

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(current, null))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        @DisplayName("null → null should be invalid")
        void validateTransition_bothNull_shouldBeInvalid() {
            // Arrange – both null

            // Act & Assert
            assertThatThrownBy(() -> OrderStatus.validateTransition(null, null))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
