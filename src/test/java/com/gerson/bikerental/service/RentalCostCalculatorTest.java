package com.gerson.bikerental.service;

import com.gerson.bikerental.enums.BicycleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RentalCostCalculatorTest {

    private final RentalCostCalculator calculator = new RentalCostCalculator();

    @Nested
    @DisplayName("RN-02: Cálculo de horas facturables")
    class BillableHoursTest {

        @Test
        @DisplayName("1h 10min → 2 horas (redondeo al alza)")
        void oneHourTenMinutes_roundsUpToTwoHours() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 1, 11, 10);
            assertEquals(2, calculator.calculateBillableHours(start, end));
        }

        @Test
        @DisplayName("2h exactas → 2 horas")
        void twoExactHours_returnsTwo() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 1, 12, 0);
            assertEquals(2, calculator.calculateBillableHours(start, end));
        }

        @Test
        @DisplayName("30 minutos → 1 hora (mínimo 1 hora)")
        void thirtyMinutes_roundsUpToOneHour() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 30);
            assertEquals(1, calculator.calculateBillableHours(start, end));
        }

        @Test
        @DisplayName("0 minutos → 1 hora (mínimo)")
        void zeroMinutes_returnsOne() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 10, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
            assertEquals(1, calculator.calculateBillableHours(start, end));
        }

        @Test
        @DisplayName("Lanza excepción si end < start")
        void endBeforeStart_throwsException() {
            LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);
            LocalDateTime end = LocalDateTime.of(2026, 1, 1, 10, 0);
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculateBillableHours(start, end));
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada en calculateBillableHours")
    class BillableHoursValidation {

        @Test
        @DisplayName("startTime null → IllegalArgumentException")
        void nullStart_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculateBillableHours(null, LocalDateTime.now()));
        }

        @Test
        @DisplayName("endTime null → IllegalArgumentException")
        void nullEnd_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculateBillableHours(LocalDateTime.now(), null));
        }
    }

    @Nested
    @DisplayName("RN-01/RN-02: Cálculo de costo base")
    class BaseCostTest {

        @ParameterizedTest
        @CsvSource({
                "URBANA, 1, 3500",
                "URBANA, 2, 7000",
                "MONTANA, 1, 5000",
                "MONTANA, 3, 15000",
                "ELECTRICA, 1, 7500",
                "ELECTRICA, 5, 37500"
        })
        @DisplayName("Costo base = tarifa/hora × horas facturables")
        void baseCostForDifferentTypes(BicycleType type, long hours, int expected) {
            BigDecimal cost = calculator.calculateBaseCost(type, hours);
            assertEquals(new BigDecimal(expected), cost);
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada en calculateBaseCost")
    class BaseCostValidation {

        @Test
        @DisplayName("bicycleType null → IllegalArgumentException")
        void nullType_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculateBaseCost(null, 1));
        }

        @Test
        @DisplayName("billableHours < 1 → IllegalArgumentException")
        void zeroHours_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculateBaseCost(BicycleType.URBANA, 0));
        }
    }

    @Nested
    @DisplayName("RN-03: Cálculo de multa por devolución tardía")
    class PenaltyTest {

        @Test
        @DisplayName("Sin retraso → multa $0")
        void noDelay_noPenalty() {
            BigDecimal penalty = calculator.calculatePenalty(
                    BicycleType.MONTANA, 3, 3);
            assertEquals(BigDecimal.ZERO, penalty);
        }

        @Test
        @DisplayName("Devolución antes de lo estimado → multa $0")
        void earlyReturn_noPenalty() {
            BigDecimal penalty = calculator.calculatePenalty(
                    BicycleType.URBANA, 5, 3);
            assertEquals(BigDecimal.ZERO, penalty);
        }

        @Test
        @DisplayName("Ejemplo del enunciado: MONTAÑA 2h estimadas, 3h20min real → 4h fact, 2h retraso → multa $5.000")
        void exampleFromSpec() {
            long billableHours = calculator.calculateBillableHours(
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 1, 1, 13, 20));
            assertEquals(4, billableHours);

            BigDecimal baseCost = calculator.calculateBaseCost(BicycleType.MONTANA, billableHours);
            assertEquals(new BigDecimal("20000"), baseCost);

            BigDecimal penalty = calculator.calculatePenalty(BicycleType.MONTANA, 2, billableHours);
            assertEquals(0, new BigDecimal("5000").compareTo(penalty));

            BigDecimal total = baseCost.add(penalty);
            assertEquals(0, new BigDecimal("25000").compareTo(total));
        }

        @Test
        @DisplayName("URBANA: 1h estimada, 2h10min real → 3h fact, 2h retraso → multa $3.500")
        void urbanaLateReturn() {
            long billableHours = calculator.calculateBillableHours(
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 1, 1, 12, 10));
            assertEquals(3, billableHours);

            BigDecimal penalty = calculator.calculatePenalty(BicycleType.URBANA, 1, billableHours);
            assertEquals(0, new BigDecimal("3500").compareTo(penalty));
        }

        @Test
        @DisplayName("ELECTRICA: 2h estimadas, 2h05min real → 3h fact, 1h retraso → multa $3.750")
        void electronicaLateReturn() {
            long billableHours = calculator.calculateBillableHours(
                    LocalDateTime.of(2026, 1, 1, 10, 0),
                    LocalDateTime.of(2026, 1, 1, 12, 5));
            assertEquals(3, billableHours);

            BigDecimal penalty = calculator.calculatePenalty(BicycleType.ELECTRICA, 2, billableHours);
            assertEquals(0, new BigDecimal("3750").compareTo(penalty));
        }
    }

    @Nested
    @DisplayName("Validaciones de entrada en calculatePenalty")
    class PenaltyValidation {

        @Test
        @DisplayName("bicycleType null → IllegalArgumentException")
        void nullType_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculatePenalty(null, 2, 3));
        }

        @Test
        @DisplayName("estimatedHours negativo → IllegalArgumentException")
        void negativeEstimated_throwsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> calculator.calculatePenalty(BicycleType.URBANA, -1, 3));
        }
    }
}