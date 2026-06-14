package com.gerson.bikerental.controller;

import com.gerson.bikerental.dto.FinishRentalRequest;
import com.gerson.bikerental.dto.RentalRequest;
import com.gerson.bikerental.dto.RentalResponse;
import com.gerson.bikerental.service.RentalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping
    public ResponseEntity<RentalResponse> startRental(
            @Valid @RequestBody RentalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rentalService.startRental(request));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<RentalResponse> finishRental(
            @PathVariable Long id,
            @RequestBody FinishRentalRequest request) {
        return ResponseEntity.ok(rentalService.finishRental(id, request));
    }

    @GetMapping("/history/{bicycleCode}")
    public ResponseEntity<List<RentalResponse>> getHistory(
            @PathVariable String bicycleCode) {
        return ResponseEntity.ok(rentalService.getRentalHistory(bicycleCode));
    }
}
