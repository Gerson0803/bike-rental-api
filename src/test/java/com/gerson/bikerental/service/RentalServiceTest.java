package com.gerson.bikerental.service;

import com.gerson.bikerental.dto.BicycleRequest;
import com.gerson.bikerental.dto.FinishRentalRequest;
import com.gerson.bikerental.dto.RentalRequest;
import com.gerson.bikerental.entity.Bicycle;
import com.gerson.bikerental.entity.Rental;
import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import com.gerson.bikerental.exception.BadRequestException;
import com.gerson.bikerental.exception.NotFoundException;
import com.gerson.bikerental.repository.BicycleRepository;
import com.gerson.bikerental.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private BicycleRepository bicycleRepository;

    @Mock
    private RentalRepository rentalRepository;

    private RentalCostCalculator costCalculator;
    private RentalService rentalService;

    @Captor
    private ArgumentCaptor<Bicycle> bicycleCaptor;

    @Captor
    private ArgumentCaptor<Rental> rentalCaptor;

    private Bicycle availableBicycle;
    private Bicycle rentedBicycle;
    private Bicycle maintenanceBicycle;

    @BeforeEach
    void setUp() {
        costCalculator = new RentalCostCalculator();
        rentalService = new RentalService(bicycleRepository, rentalRepository, costCalculator);

        availableBicycle = Bicycle.builder()
                .id(1L).code("BIC-001").type(BicycleType.URBANA)
                .status(BicycleStatus.DISPONIBLE).build();

        rentedBicycle = Bicycle.builder()
                .id(2L).code("BIC-002").type(BicycleType.MONTANA)
                .status(BicycleStatus.ALQUILADA).build();

        maintenanceBicycle = Bicycle.builder()
                .id(3L).code("BIC-003").type(BicycleType.ELECTRICA)
                .status(BicycleStatus.EN_MANTENIMIENTO).build();
    }

    @Nested
    @DisplayName("RF-01 / RN-04: Gestión y disponibilidad de bicicletas")
    class BicycleManagementTest {

        @Test
        @DisplayName("Crear bicicleta exitosamente")
        void createBicycle_success() {
            BicycleRequest request = new BicycleRequest();
            request.setCode("BIC-100");
            request.setType(BicycleType.URBANA);
            request.setStatus(BicycleStatus.DISPONIBLE);

            when(bicycleRepository.findByCode("BIC-100")).thenReturn(Optional.empty());
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var response = rentalService.createBicycle(request);

            assertEquals("BIC-100", response.getCode());
            assertEquals(BicycleType.URBANA, response.getType());
            assertEquals(BicycleStatus.DISPONIBLE, response.getStatus());
        }

        @Test
        @DisplayName("Crear bicicleta con código duplicado → error")
        void createBicycle_duplicateCode_throwsException() {
            BicycleRequest request = new BicycleRequest();
            request.setCode("BIC-001");
            when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(availableBicycle));

            assertThrows(BadRequestException.class, () -> rentalService.createBicycle(request));
        }

        @Test
        @DisplayName("RN-04: Alquilar bicicleta ALQUILADA → error")
        void startRental_alreadyRented_throwsException() {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-002");
            request.setCustomerName("Cliente");
            request.setEstimatedHours(2);

            when(bicycleRepository.findByCode("BIC-002")).thenReturn(Optional.of(rentedBicycle));

            assertThrows(BadRequestException.class, () -> rentalService.startRental(request));
        }

        @Test
        @DisplayName("RN-04: Alquilar bicicleta EN_MANTENIMIENTO → error")
        void startRental_inMaintenance_throwsException() {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-003");
            request.setCustomerName("Cliente");
            request.setEstimatedHours(2);

            when(bicycleRepository.findByCode("BIC-003")).thenReturn(Optional.of(maintenanceBicycle));

            assertThrows(BadRequestException.class, () -> rentalService.startRental(request));
        }
    }

    @Nested
    @DisplayName("RF-02: Inicio de alquiler")
    class StartRentalTest {

        @Test
        @DisplayName("Iniciar alquiler cambia estado a ALQUILADA")
        void startRental_changesBicycleStatus() {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-001");
            request.setCustomerName("Juan Pérez");
            request.setEstimatedHours(3);
            request.setStartTime(LocalDateTime.of(2026, 1, 1, 10, 0));

            when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(availableBicycle));
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var response = rentalService.startRental(request);

            assertEquals("Juan Pérez", response.getCustomerName());
            assertEquals("BIC-001", response.getBicycleCode());

            verify(bicycleRepository).save(bicycleCaptor.capture());
            assertEquals(BicycleStatus.ALQUILADA, bicycleCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("Iniciar alquiler sin startTime usa hora actual")
        void startRental_withoutStartTime_usesNow() {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-001");
            request.setCustomerName("Cliente");
            request.setEstimatedHours(1);

            when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(availableBicycle));
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var response = rentalService.startRental(request);
            assertNotNull(response.getStartTime());
        }
    }

    @Nested
    @DisplayName("RF-03: Finalización de alquiler")
    class FinishRentalTest {

        private Rental activeRental;

        @BeforeEach
        void setUp() {
            activeRental = Rental.builder()
                    .id(100L)
                    .customerName("Juan Pérez")
                    .startTime(LocalDateTime.of(2026, 1, 1, 10, 0))
                    .estimatedHours(2)
                    .bicycle(availableBicycle)
                    .build();
        }

        @Test
        @DisplayName("Finalizar alquiler actualiza estado a DISPONIBLE")
        void finishRental_changesBicycleStatus() {
            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));

            when(rentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            rentalService.finishRental(100L, request);

            verify(bicycleRepository).save(bicycleCaptor.capture());
            assertEquals(BicycleStatus.DISPONIBLE, bicycleCaptor.getValue().getStatus());
        }

        @Test
        @DisplayName("RN-05: Finalizar alquiler inexistente → error")
        void finishRental_notFound_throwsException() {
            when(rentalRepository.findById(999L)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> rentalService.finishRental(999L, new FinishRentalRequest()));
        }

        @Test
        @DisplayName("RN-05: Finalizar alquiler ya finalizado → error")
        void finishRental_alreadyFinished_throwsException() {
            activeRental.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));
            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 14, 0));

            when(rentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));

            assertThrows(BadRequestException.class,
                    () -> rentalService.finishRental(100L, request));
        }

        @Test
        @DisplayName("EndTime antes que startTime → error")
        void finishRental_endBeforeStart_throwsException() {
            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2025, 12, 31, 23, 0));

            when(rentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));

            assertThrows(BadRequestException.class,
                    () -> rentalService.finishRental(100L, request));
        }

        @Test
        @DisplayName("Finalizar alquiler sin endTime usa hora actual")
        void finishRental_withoutEndTime_usesNow() {
            FinishRentalRequest request = new FinishRentalRequest();

            when(rentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var response = rentalService.finishRental(100L, request);

            assertNotNull(response.getEndTime());
        }

        @Test
        @DisplayName("Finalizar alquiler calcula costos correctamente")
        void finishRental_calculatesCosts() {
            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 13, 20));

            when(rentalRepository.findById(100L)).thenReturn(Optional.of(activeRental));
            when(bicycleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(rentalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var response = rentalService.finishRental(100L, request);

            assertEquals(0, new BigDecimal("14000").compareTo(response.getBaseCost()));
            assertEquals(0, new BigDecimal("3500").compareTo(response.getPenalty()));
            assertEquals(0, new BigDecimal("17500").compareTo(response.getTotalCost()));
        }
    }

    @Nested
    @DisplayName("RF-05: Historial de alquileres")
    class HistoryTest {

        @Test
        @DisplayName("Historial de bicicleta existente")
        void existingBicycle_returnsHistory() {
            when(bicycleRepository.findByCode("BIC-001")).thenReturn(Optional.of(availableBicycle));
            when(rentalRepository.findByBicycle(availableBicycle)).thenReturn(List.of());

            var history = rentalService.getRentalHistory("BIC-001");
            assertNotNull(history);
        }

        @Test
        @DisplayName("Historial de bicicleta inexistente → error")
        void nonExistentBicycle_throwsException() {
            when(bicycleRepository.findByCode("BIC-999")).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class,
                    () -> rentalService.getRentalHistory("BIC-999"));
        }
    }
}