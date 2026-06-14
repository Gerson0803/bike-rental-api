package com.gerson.bikerental.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.bikerental.dto.RentalRequest;
import com.gerson.bikerental.service.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RentalService rentalService;

    @Test
    @DisplayName("NotFound → 404 con mensaje")
    void notFound_returns404() throws Exception {
        when(rentalService.startRental(any()))
                .thenThrow(new NotFoundException("Bicycle not found: BIC-999"));

        RentalRequest request = new RentalRequest();
        request.setBicycleCode("BIC-999");
        request.setCustomerName("Juan");
        request.setEstimatedHours(2);

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Bicycle not found: BIC-999"));
    }

    @Test
    @DisplayName("BadRequest → 400 con mensaje")
    void badRequest_returns400() throws Exception {
        when(rentalService.startRental(any()))
                .thenThrow(new BadRequestException("Bicycle is not available"));

        RentalRequest request = new RentalRequest();
        request.setBicycleCode("BIC-001");
        request.setCustomerName("Juan");
        request.setEstimatedHours(2);

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Bicycle is not available"));
    }

    @Test
    @DisplayName("Validation error → 400 con detalles")
    void validationError_returns400() throws Exception {
        RentalRequest request = new RentalRequest();
        request.setBicycleCode("");
        request.setCustomerName("");
        request.setEstimatedHours(0);

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    @DisplayName("Excepción genérica → 500")
    void genericException_returns500() throws Exception {
        when(rentalService.startRental(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        RentalRequest request = new RentalRequest();
        request.setBicycleCode("BIC-001");
        request.setCustomerName("Juan");
        request.setEstimatedHours(2);

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}