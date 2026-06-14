package com.gerson.bikerental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerson.bikerental.dto.BicycleRequest;
import com.gerson.bikerental.dto.BicycleResponse;
import com.gerson.bikerental.enums.BicycleStatus;
import com.gerson.bikerental.enums.BicycleType;
import com.gerson.bikerental.exception.BadRequestException;
import com.gerson.bikerental.service.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BicycleController.class)
class BicycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RentalService rentalService;

    @Nested
    @DisplayName("POST /api/bicycles")
    class CreateBicycle {

        @Test
        @DisplayName("201 cuando los datos son válidos")
        void validRequest_returns201() throws Exception {
            BicycleRequest request = new BicycleRequest();
            request.setCode("BIC-100");
            request.setType(BicycleType.URBANA);
            request.setStatus(BicycleStatus.DISPONIBLE);

            when(rentalService.createBicycle(any()))
                    .thenReturn(new BicycleResponse("BIC-100", BicycleType.URBANA, BicycleStatus.DISPONIBLE));

            mockMvc.perform(post("/api/bicycles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("BIC-100"))
                    .andExpect(jsonPath("$.type").value("URBANA"))
                    .andExpect(jsonPath("$.status").value("DISPONIBLE"));
        }

        @Test
        @DisplayName("400 cuando el código está vacío")
        void blankCode_returns400() throws Exception {
            BicycleRequest request = new BicycleRequest();
            request.setCode("");
            request.setType(BicycleType.URBANA);
            request.setStatus(BicycleStatus.DISPONIBLE);

            mockMvc.perform(post("/api/bicycles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 cuando el tipo es null")
        void nullType_returns400() throws Exception {
            BicycleRequest request = new BicycleRequest();
            request.setCode("BIC-100");
            request.setStatus(BicycleStatus.DISPONIBLE);

            mockMvc.perform(post("/api/bicycles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/bicycles/available")
    class GetAvailable {

        @Test
        @DisplayName("200 sin filtro")
        void withoutFilter_returns200() throws Exception {
            when(rentalService.findAvailableBicycles(null))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/bicycles/available"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 con filtro válido")
        void validTypeFilter_returns200() throws Exception {
            when(rentalService.findAvailableBicycles("URBANA"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/bicycles/available")
                            .param("type", "URBANA"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("400 con filtro inválido")
        void invalidTypeFilter_returns400() throws Exception {
            mockMvc.perform(get("/api/bicycles/available")
                            .param("type", "INVALIDO"))
                    .andExpect(status().isBadRequest());
        }
    }
}