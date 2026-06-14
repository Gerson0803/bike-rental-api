package com.gerson.bikerental.enums;

import java.math.BigDecimal;

public enum BicycleType {
    URBANA(new BigDecimal("3500")),
    MONTANA(new BigDecimal("5000")),
    ELECTRICA(new BigDecimal("7500"));
    private final BigDecimal hourlyRate;

    BicycleType(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }
}
