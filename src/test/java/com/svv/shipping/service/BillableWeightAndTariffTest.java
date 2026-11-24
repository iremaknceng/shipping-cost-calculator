package com.svv.shipping.service;

import com.svv.shipping.model.Shipment;
import com.svv.shipping.validation.InvalidShipmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-06 billable weight, FR-07 base tariff and FR-07a oversize limit.
 */
@DisplayName("Billable weight (FR-06), base tariff (FR-07) and oversize limit (FR-07a)")
class BillableWeightAndTariffTest {

    private ShippingCostCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShippingCostCalculator();
    }

    @Nested
    @DisplayName("FR-06 Billable weight = max(actual, volumetric) rounded up to 0.5 kg")
    class BillableWeight {

        @ParameterizedTest(name = "{0} [{1}] {2} kg, {3}x{4}x{5} cm -> {6} kg")
        @CsvSource({
                "TC-B-01, EP  actual weight dominates,       2.0,  10, 10, 10, 2.0",
                "TC-B-02, EP  volumetric weight dominates,   1.0,  60, 40, 30, 24.0",
                "TC-B-03, BVA exact half step,               2.5,  10, 10, 10, 2.5",
                "TC-B-04, BVA just above whole kg,           2.1,  10, 10, 10, 2.5",
                "TC-B-05, BVA just above half step,          2.51, 10, 10, 10, 3.0",
                "TC-B-06, BVA minimum weight,                0.1,  10, 10, 10, 0.5",
                "TC-B-07, BVA maximum weight,                30.0, 10, 10, 10, 30.0",
                "TC-B-08, BVA volumetric exact (10.5),       1.0,  50, 30, 21, 10.5",
                "TC-B-09, BVA volumetric between steps,      1.0,  41, 30, 25, 10.5",
                "TC-B-10, BVA actual = volumetric,           2.0,  30, 20, 10, 2.0"
        })
        void billableWeightIsComputed(String id, String technique, double kg, int l, int w, int h, String expected) {
            Shipment s = Shipment.builder().weight(kg).dimensions(l, w, h).build();
            assertEquals(new BigDecimal(expected), calculator.billableWeight(s));
        }
    }

    @Nested
    @DisplayName("FR-07 Base tariff brackets")
    class BaseTariff {

        @ParameterizedTest(name = "{0} [{1}] billable {2} kg -> {3} TL")
        @CsvSource({
                "TC-P-01, EP  bracket 1,           0.5,  40.00",
                "TC-P-02, BVA bracket 1 upper,     1.0,  40.00",
                "TC-P-03, BVA bracket 2 lower,     1.5,  44.00",
                "TC-P-04, BVA bracket 2 upper-1,   4.5,  68.00",
                "TC-P-05, BVA bracket 2 upper,     5.0,  72.00",
                "TC-P-06, BVA bracket 3 lower,     5.5,  75.00",
                "TC-P-07, BVA bracket 3 upper-1,   14.5, 129.00",
                "TC-P-08, BVA bracket 3 upper,     15.0, 132.00",
                "TC-P-09, BVA bracket 4 lower,     15.5, 134.50",
                "TC-P-10, BVA bracket 4 max,       50.0, 307.00"
        })
        void baseCostFollowsBrackets(String id, String technique, String kg, String expected) {
            assertEquals(new BigDecimal(expected), calculator.baseCost(new BigDecimal(kg)));
        }
    }

    @Nested
    @DisplayName("FR-07a Oversize limit (billable weight <= 50 kg)")
    class Oversize {

        @ParameterizedTest(name = "{0} [BVA] {1}x{2}x{3} cm (billable {4} kg) is accepted")
        @CsvSource({
                "TC-O-01, 100, 50, 30, 50.0",
                "TC-O-02, 99,  50, 30, 49.5"
        })
        void atOrBelowLimitIsAccepted(String id, int l, int w, int h, String billable) {
            Shipment s = Shipment.builder().weight(1.0).dimensions(l, w, h).build();
            assertDoesNotThrow(() -> calculator.calculate(s));
            assertEquals(new BigDecimal(billable), calculator.billableWeight(s));
        }

        @ParameterizedTest(name = "{0} [BVA/NEG] {1}x{2}x{3} cm (billable {4} kg) is rejected as oversized")
        @CsvSource({
                "TC-O-03, 101, 50,  30,  50.5",
                "TC-O-04, 100, 100, 100, 333.5"
        })
        void aboveLimitIsRejected(String id, int l, int w, int h, String billable) {
            Shipment s = Shipment.builder().weight(1.0).dimensions(l, w, h).build();
            InvalidShipmentException ex = assertThrows(InvalidShipmentException.class, () -> calculator.calculate(s));
            assertEquals(true, ex.getMessage().contains("oversized"), ex.getMessage());
        }
    }
}
