package com.gerson.bikerental.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RentalResponse {

    private Long id;
    private String bicycleCode;
    private String customerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer estimatedHours;
    private BigDecimal baseCost;
    private BigDecimal penalty;
    private BigDecimal totalCost;

    public RentalResponse() {
    }

    public RentalResponse(Long id,
                          String bicycleCode,
                          String customerName,
                          LocalDateTime startTime,
                          LocalDateTime endTime,
                          Integer estimatedHours,
                          BigDecimal baseCost,
                          BigDecimal penalty,
                          BigDecimal totalCost) {
        this.id = id;
        this.bicycleCode = bicycleCode;
        this.customerName = customerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.estimatedHours = estimatedHours;
        this.baseCost = baseCost;
        this.penalty = penalty;
        this.totalCost = totalCost;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public BigDecimal getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(BigDecimal baseCost) {
        this.baseCost = baseCost;
    }

    public BigDecimal getPenalty() {
        return penalty;
    }

    public void setPenalty(BigDecimal penalty) {
        this.penalty = penalty;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }
}
