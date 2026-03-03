package com.restaurant.reportservice.domain;

import com.restaurant.reportservice.domain.model.DateRange;
import com.restaurant.reportservice.domain.service.DateRangeFilter;
import com.restaurant.reportservice.exception.InvalidDateRangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for date range filtering and validation.
 * Applies equivalence partitioning and boundary value analysis.
 */
class DateRangeFilterTest {

    private DateRangeFilter dateRangeFilter;

    @BeforeEach
    void setUp() {
        dateRangeFilter = new DateRangeFilter();
    }

    @Test
    @DisplayName("Should accept valid date range where startDate <= endDate")
    void shouldAcceptValidDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        // Act & Assert
        assertDoesNotThrow(() -> {
            DateRange range = dateRangeFilter.validateAndCreate(startDate, endDate);
            assertEquals(startDate, range.getStartDate());
            assertEquals(endDate, range.getEndDate());
        });
    }

    @Test
    @DisplayName("Should accept date range where startDate equals endDate (boundary)")
    void shouldAcceptSameDateRange() {
        // Arrange
        LocalDate date = LocalDate.of(2026, 2, 15);

        // Act & Assert
        assertDoesNotThrow(() -> {
            DateRange range = dateRangeFilter.validateAndCreate(date, date);
            assertEquals(date, range.getStartDate());
            assertEquals(date, range.getEndDate());
        });
    }

    @Test
    @DisplayName("Should throw exception when startDate > endDate")
    void shouldRejectInvalidDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 28);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> 
            dateRangeFilter.validateAndCreate(startDate, endDate)
        );
    }

    @Test
    @DisplayName("Should throw exception when startDate is null")
    void shouldRejectNullStartDate() {
        // Arrange
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> 
            dateRangeFilter.validateAndCreate(null, endDate)
        );
    }

    @Test
    @DisplayName("Should throw exception when endDate is null")
    void shouldRejectNullEndDate() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 1);

        // Act & Assert
        assertThrows(InvalidDateRangeException.class, () -> 
            dateRangeFilter.validateAndCreate(startDate, null)
        );
    }

    @Test
    @DisplayName("Should correctly filter timestamp within date range (inclusive boundaries)")
    void shouldFilterTimestampWithinRange() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange range = DateRange.of(startDate, endDate);

        LocalDateTime withinRange = LocalDateTime.of(2026, 2, 15, 14, 30);

        // Act
        boolean isInRange = dateRangeFilter.isWithinRange(withinRange, range);

        // Assert
        assertTrue(isInRange);
    }

    @Test
    @DisplayName("Should include timestamp at start boundary (inclusive)")
    void shouldIncludeStartBoundary() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange range = DateRange.of(startDate, endDate);

        LocalDateTime atStartBoundary = LocalDateTime.of(2026, 2, 10, 0, 0, 0);

        // Act
        boolean isInRange = dateRangeFilter.isWithinRange(atStartBoundary, range);

        // Assert
        assertTrue(isInRange);
    }

    @Test
    @DisplayName("Should include timestamp at end boundary (inclusive)")
    void shouldIncludeEndBoundary() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange range = DateRange.of(startDate, endDate);

        LocalDateTime atEndBoundary = LocalDateTime.of(2026, 2, 20, 23, 59, 59);

        // Act
        boolean isInRange = dateRangeFilter.isWithinRange(atEndBoundary, range);

        // Assert
        assertTrue(isInRange);
    }

    @Test
    @DisplayName("Should exclude timestamp before start date")
    void shouldExcludeTimestampBeforeStart() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange range = DateRange.of(startDate, endDate);

        LocalDateTime beforeStart = LocalDateTime.of(2026, 2, 9, 23, 59, 59);

        // Act
        boolean isInRange = dateRangeFilter.isWithinRange(beforeStart, range);

        // Assert
        assertFalse(isInRange);
    }

    @Test
    @DisplayName("Should exclude timestamp after end date")
    void shouldExcludeTimestampAfterEnd() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 2, 10);
        LocalDate endDate = LocalDate.of(2026, 2, 20);
        DateRange range = DateRange.of(startDate, endDate);

        LocalDateTime afterEnd = LocalDateTime.of(2026, 2, 21, 0, 0, 1);

        // Act
        boolean isInRange = dateRangeFilter.isWithinRange(afterEnd, range);

        // Assert
        assertFalse(isInRange);
    }

    @ParameterizedTest
    @CsvSource({
        "2026-01-01, 2026-01-01, true",   // Same day
        "2026-01-01, 2026-01-02, true",   // Consecutive days
        "2026-01-01, 2026-12-31, true",   // Full year
        "2026-02-01, 2026-01-31, false",  // Inverted
        "2025-12-31, 2026-01-01, true"    // Year boundary
    })
    @DisplayName("Should validate date ranges with various combinations")
    void shouldValidateVariousDateRangeCombinations(String start, String end, boolean shouldBeValid) {
        // Arrange
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);

        // Act & Assert
        if (shouldBeValid) {
            assertDoesNotThrow(() -> dateRangeFilter.validateAndCreate(startDate, endDate));
        } else {
            assertThrows(InvalidDateRangeException.class, 
                () -> dateRangeFilter.validateAndCreate(startDate, endDate));
        }
    }
}
