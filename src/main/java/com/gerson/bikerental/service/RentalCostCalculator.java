package com.gerson.bikerental.service;

import com.gerson.bikerental.enums.BicycleType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RentalCostCalculator{

    public long calculateBillableHours(
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time"
            );
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();

        long billableHours = (long) Math.ceil(minutes / 60.0);

        return Math.max(1, billableHours);
    }
    public BigDecimal calculateBaseCost(
            BicycleType bicycleType,
            long billableHours) {

        if (bicycleType == null) {
            throw new IllegalArgumentException(
                    "Bicycle type is required"
            );
        }

        if (billableHours < 1) {
            throw new IllegalArgumentException(
                    "Billable hours must be greater than zero"
            );
        }

        return bicycleType.getHourlyRate()
                .multiply(BigDecimal.valueOf(billableHours));
    }
    public BigDecimal calculatePenalty(
            BicycleType bicycleType,
            long estimatedHours,
            long realHours) {

        if (bicycleType == null) {
            throw new IllegalArgumentException(
                    "Bicycle type is required"
            );
        }

        if (estimatedHours < 0) {
            throw new IllegalArgumentException(
                    "Estimated hours must be greater than zero"
            );
        }

        if (realHours <= estimatedHours) {
            return BigDecimal.ZERO;
        }

        long lateHours = realHours - estimatedHours;
        BigDecimal penaltyRate = bicycleType.getHourlyRate()
                .multiply(BigDecimal.valueOf(0.5));

        return penaltyRate.multiply(BigDecimal.valueOf(lateHours));
    }
}
