package com.gerson.bikerental.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class RentalRequest {

    @NotBlank
    private String bicycleCode;

    @NotBlank
    private String customerName;

    @NotNull
    @Min(1)
    private Integer estimatedHours;

    private LocalDateTime startTime;

    public String getBicycleCode() {
        return bicycleCode;
    }

    public void setBicycleCode(String bicycleCode) {
        this.bicycleCode = bicycleCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}
