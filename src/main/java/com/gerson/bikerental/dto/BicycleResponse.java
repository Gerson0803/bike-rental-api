package com.gerson.bikerental.dto;

import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;

public class BicycleResponse {

    private String code;
    private BicycleType type;
    private BicycleStatus status;

    public BicycleResponse() {
    }

    public BicycleResponse(String code, BicycleType type, BicycleStatus status) {
        this.code = code;
        this.type = type;
        this.status = status;
    }

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
