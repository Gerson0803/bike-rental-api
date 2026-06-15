package com.gerson.bikerental.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "app", "Bike Rental API",
                "docs", "/README.md",
                "endpoints", Map.of(
                        "bicycles", "/api/bicycles/available",
                        "createBicycle", "POST /api/bicycles",
                        "startRental", "POST /api/rentals",
                        "finishRental", "POST /api/rentals/{id}/finish",
                        "history", "/api/rentals/history/{code}"
                )
        );
    }
}