package com.restaurant.reportservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    private LocalDate startDate;
    private LocalDate endDate;

    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return DateRange.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}
