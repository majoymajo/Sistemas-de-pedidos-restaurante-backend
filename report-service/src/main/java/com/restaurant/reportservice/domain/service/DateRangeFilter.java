package com.restaurant.reportservice.domain.service;

import com.restaurant.reportservice.domain.model.DateRange;
import com.restaurant.reportservice.exception.InvalidDateRangeException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain service for date range validation and filtering.
 * Pure business logic without infrastructure dependencies.
 */
@Component
public class DateRangeFilter {

    public DateRange validateAndCreate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidDateRangeException("Start date and end date must not be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidDateRangeException("Start date must not be after end date");
        }
        return DateRange.of(startDate, endDate);
    }

    public boolean isWithinRange(LocalDateTime timestamp, DateRange range) {
        LocalDateTime start = range.getStartDate().atStartOfDay();
        LocalDateTime end = range.getEndDate().atTime(23, 59, 59);
        return !timestamp.isBefore(start) && !timestamp.isAfter(end);
    }
}
