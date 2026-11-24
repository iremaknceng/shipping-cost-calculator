package com.svv.shipping.service;

import com.svv.shipping.model.CustomerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FR-12 loyalty discount.
 *
 * <pre>
 * Decision table            R1   R2   R3   R4   R5   R6
 * C1 customer type          REG  REG  SIL  SIL  GOLD GOLD
 * C2 subtotal >= 500 TL     N    Y    N    Y    N    Y
 * A1 discount rate          0%   0%   5%   8%   10%  15%
 * </pre>
 * Each rule is exercised; C2 is additionally tested at 499.99 / 500.00 / 500.01 (BVA).
 */
@DisplayName("Loyalty discount - decision table (FR-12)")
class DiscountDecisionTableTest {

    private final ShippingCostCalculator calculator = new ShippingCostCalculator();

    @ParameterizedTest(name = "{0} [DT {1}] {2}, subtotal {3} TL -> rate {4}")
    @CsvSource({
            "TC-R-01, R1,       REGULAR, 100.00, 0.00",
            "TC-R-02, R2,       REGULAR, 800.00, 0.00",
            "TC-R-03, R3,       SILVER,  100.00, 0.05",
            "TC-R-04, R4,       SILVER,  800.00, 0.08",
            "TC-R-05, R5,       GOLD,    100.00, 0.10",
            "TC-R-06, R6,       GOLD,    800.00, 0.15",
            "TC-R-07, R5 BVA,   GOLD,    499.99, 0.10",
            "TC-R-08, R6 BVA,   GOLD,    500.00, 0.15",
            "TC-R-09, R6 BVA,   GOLD,    500.01, 0.15",
            "TC-R-10, R3 BVA,   SILVER,  499.99, 0.05",
            "TC-R-11, R4 BVA,   SILVER,  500.00, 0.08"
    })
    void discountRateFollowsDecisionTable(String id, String rule, CustomerType customer,
                                          String subtotal, String expectedRate) {
        assertEquals(new BigDecimal(expectedRate), calculator.discountRate(customer, new BigDecimal(subtotal)));
    }
}
