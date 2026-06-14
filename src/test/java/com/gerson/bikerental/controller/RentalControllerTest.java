package com.gerson.bikerental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.bikerental.dto.FinishRentalRequest;
import com.gerson.bikerental.dto.RentalRequest;
import com.gerson.bikerental.dto.RentalResponse;
import com.gerson.bikerental.exception.BadRequestException;
import com.gerson.bikerental.exception.NotFoundException;
import com.gerson.bikerental.service.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalController.class)
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RentalService rentalService;

    private final RentalResponse sampleRental = new RentalResponse(
            1L, "BIC-001", "Juan", LocalDateTime.of(2026, 1, 1, 10, 0),
            null, 2, null, null, null
    );

    @Nested
    @DisplayName("POST /api/rentals")
    class StartRental {

        @Test
        @DisplayName("201 cuando los datos son válidos")
        void validRequest_returns201() throws Exception {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-001");
            request.setCustomerName("Juan");
            request.setEstimatedHours(2);
            request.setStartTime(LocalDateTime.of(2026, 1, 1, 10, 0));

            when(rentalService.startRental(any())).thenReturn(sampleRental);

            mockMvc.perform(post("/api/rentals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.customerName").value("Juan"));
        }

        @Test
        @DisplayName("400 cuando el código de bicicleta está vacío")
        void blankBicycleCode_returns400() throws Exception {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("");
            request.setCustomerName("Juan");
            request.setEstimatedHours(2);

            mockMvc.perform(post("/api/rentals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 cuando estimatedHours es 0")
        void zeroEstimatedHours_returns400() throws Exception {
            RentalRequest request = new RentalRequest();
            request.setBicycleCode("BIC-001");
            request.setCustomerName("Juan");
            request.setEstimatedHours(0);

            mockMvc.perform(post("/api/rentals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/rentals/{id}/finish")
    class FinishRental {

        @Test
        @DisplayName("200 cuando el alquiler existe")
        void validRequest_returns200() throws Exception {
            RentalResponse finished = new RentalResponse(
                    1L, "BIC-001", "Juan",
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 1, 1, 12, 0),
                    2, new BigDecimal("7000"), BigDecimal.ZERO, new BigDecimal("7000")
            );

            when(rentalService.finishRental(eq(1L), any())).thenReturn(finished);

            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));

            mockMvc.perform(post("/api/rentals/1/finish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCost").value(7000));
        }

        @Test
        @DisplayName("400 cuando el alquiler ya terminó")
        void alreadyFinished_returns400() throws Exception {
            when(rentalService.finishRental(eq(1L), any()))
                    .thenThrow(new BadRequestException("Rental has already been finished"));

            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));

            mockMvc.perform(post("/api/rentals/1/finish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 cuando el alquiler no existe")
        void notFound_returns404() throws Exception {
            when(rentalService.finishRental(eq(999L), any()))
                    .thenThrow(new NotFoundException("Rental not found: 999"));

            FinishRentalRequest request = new FinishRentalRequest();
            request.setEndTime(LocalDateTime.of(2026, 1, 1, 12, 0));

            mockMvc.perform(post("/api/rentals/999/finish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/rentals/history/{bicycleCode}")
    class GetHistory {

        @Test
        @DisplayName("200 con historial")
        void existingBicycle_returns200() throws Exception {
            when(rentalService.getRentalHistory("BIC-001"))
                    .thenReturn(List.of(sampleRental));

            mockMvc.perform(get("/api/rentals/history/BIC-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(1));
        }

        @Test
        @DisplayName("404 si la bicicleta no existe")
        void nonExistentBicycle_returns404() throws Exception {
            when(rentalService.getRentalHistory("BIC-999"))
                    .thenThrow(new NotFoundException("Bicycle not found: BIC-999"));

            mockMvc.perform(get("/api/rentals/history/BIC-999"))
                    .andExpect(status().isNotFound());
        }
    }
}