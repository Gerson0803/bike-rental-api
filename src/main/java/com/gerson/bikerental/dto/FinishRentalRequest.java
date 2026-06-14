package com.gerson.bikerental.dto;

import java.time.LocalDateTime;

public class FinishRentalRequest {

    private LocalDateTime endTime;

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
