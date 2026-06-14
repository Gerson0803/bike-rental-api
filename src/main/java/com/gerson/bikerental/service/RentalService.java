package com.gerson.bikerental.service;

import com.gerson.bikerental.dto.BicycleRequest;
import com.gerson.bikerental.dto.BicycleResponse;
import com.gerson.bikerental.dto.FinishRentalRequest;
import com.gerson.bikerental.dto.RentalRequest;
import com.gerson.bikerental.dto.RentalResponse;
import com.gerson.bikerental.entity.Bicycle;
import com.gerson.bikerental.entity.Rental;
import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import com.gerson.bikerental.exception.BadRequestException;
import com.gerson.bikerental.exception.NotFoundException;
import com.gerson.bikerental.repository.BicycleRepository;
import com.gerson.bikerental.repository.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RentalService {

    private final BicycleRepository bicycleRepository;
    private final RentalRepository rentalRepository;
    private final RentalCostCalculator costCalculator;

    public RentalService(
            BicycleRepository bicycleRepository,
            RentalRepository rentalRepository,
            RentalCostCalculator costCalculator) {
        this.bicycleRepository = bicycleRepository;
        this.rentalRepository = rentalRepository;
        this.costCalculator = costCalculator;
    }

    @Transactional
    public BicycleResponse createBicycle(BicycleRequest request) {
        if (bicycleRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Bicycle code already exists");
        }

        Bicycle bicycle = Bicycle.builder()
                .code(request.getCode())
                .type(request.getType())
                .status(request.getStatus())
                .build();

        Bicycle saved = bicycleRepository.save(bicycle);
        return toBicycleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BicycleResponse> findAvailableBicycles(String type) {
        List<Bicycle> bicycles;
        if (type == null || type.isBlank()) {
            bicycles = bicycleRepository.findByStatus(BicycleStatus.DISPONIBLE);
        } else {
            bicycles = bicycleRepository.findByStatusAndType(
                    BicycleStatus.DISPONIBLE,
                    BicycleType.valueOf(type.toUpperCase())
            );
        }
        return bicycles.stream()
                .map(this::toBicycleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RentalResponse startRental(RentalRequest request) {
        Bicycle bicycle = bicycleRepository.findByCode(request.getBicycleCode())
                .orElseThrow(() -> new NotFoundException("Bicycle not found: " + request.getBicycleCode()));

        if (bicycle.getStatus() != BicycleStatus.DISPONIBLE) {
            throw new BadRequestException("Bicycle is not available for rent");
        }

        LocalDateTime startTime = request.getStartTime() == null
                ? LocalDateTime.now()
                : request.getStartTime();

        Rental rental = Rental.builder()
                .bicycle(bicycle)
                .customerName(request.getCustomerName())
                .startTime(startTime)
                .estimatedHours(request.getEstimatedHours())
                .build();

        bicycle.setStatus(BicycleStatus.ALQUILADA);
        bicycleRepository.save(bicycle);
        Rental saved = rentalRepository.save(rental);

        return toRentalResponse(saved);
    }

    @Transactional
    public RentalResponse finishRental(Long rentalId, FinishRentalRequest request) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new NotFoundException("Rental not found: " + rentalId));

        if (rental.getEndTime() != null) {
            throw new BadRequestException("Rental has already been finished");
        }

        LocalDateTime endTime = request.getEndTime() == null
                ? LocalDateTime.now()
                : request.getEndTime();

        if (endTime.isBefore(rental.getStartTime())) {
            throw new BadRequestException("End time cannot be before start time");
        }

        long realHours = costCalculator.calculateBillableHours(rental.getStartTime(), endTime);
        BigDecimal baseCost = costCalculator.calculateBaseCost(
                rental.getBicycle().getType(),
                realHours
        );
        BigDecimal penalty = costCalculator.calculatePenalty(
                rental.getBicycle().getType(),
                rental.getEstimatedHours(),
                realHours
        );
        BigDecimal totalCost = baseCost.add(penalty);

        rental.setEndTime(endTime);
        rental.setBaseCost(baseCost);
        rental.setPenalty(penalty);
        rental.setTotalCost(totalCost);

        Bicycle bicycle = rental.getBicycle();
        bicycle.setStatus(BicycleStatus.DISPONIBLE);

        bicycleRepository.save(bicycle);
        Rental saved = rentalRepository.save(rental);

        return toRentalResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getRentalHistory(String bicycleCode) {
        Bicycle bicycle = bicycleRepository.findByCode(bicycleCode)
                .orElseThrow(() -> new NotFoundException("Bicycle not found: " + bicycleCode));

        return rentalRepository.findByBicycle(bicycle).stream()
                .map(this::toRentalResponse)
                .collect(Collectors.toList());
    }

    private BicycleResponse toBicycleResponse(Bicycle bicycle) {
        return new BicycleResponse(
                bicycle.getCode(),
                bicycle.getType(),
                bicycle.getStatus()
        );
    }

    private RentalResponse toRentalResponse(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                rental.getBicycle().getCode(),
                rental.getCustomerName(),
                rental.getStartTime(),
                rental.getEndTime(),
                rental.getEstimatedHours(),
                rental.getBaseCost(),
                rental.getPenalty(),
                rental.getTotalCost()
        );
    }
}
