package com.gerson.bikerental.dto;

import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BicycleRequest {

    @NotBlank
    private String code;

    @NotNull
    private BicycleType type;

    @NotNull
    private BicycleStatus status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BicycleType getType() {
        return type;
    }

    public void setType(BicycleType type) {
        this.type = type;
    }

    public BicycleStatus getStatus() {
        return status;
    }

    public void setStatus(BicycleStatus status) {
        this.status = status;
    }
}
