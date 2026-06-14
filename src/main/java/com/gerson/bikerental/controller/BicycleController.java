package com.gerson.bikerental.controller;

import com.gerson.bikerental.dto.BicycleRequest;
import com.gerson.bikerental.dto.BicycleResponse;
import com.gerson.bikerental.enums.BicycleType;
import com.gerson.bikerental.exception.BadRequestException;
import com.gerson.bikerental.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bicycles")
public class BicycleController {

    private final RentalService rentalService;

    public BicycleController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping
    public ResponseEntity<BicycleResponse> createBicycle(
            @Valid @RequestBody BicycleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalService.createBicycle(request));
    }

    @GetMapping("/available")
    public ResponseEntity<List<BicycleResponse>> getAvailableBicycles(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            try {
                BicycleType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid bicycle type: " + type);
            }
        }
        return ResponseEntity.ok(rentalService.findAvailableBicycles(type));
    }
}
