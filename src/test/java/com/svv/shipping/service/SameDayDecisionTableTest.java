package com.svv.shipping.service;

import com.svv.shipping.model.PriceBreakdown;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-10 SAME_DAY eligibility.
 *
 * <pre>
 * Decision table                 R1  R2  R3  R4  R5  R6  R7  R8
 * C1 zone is LOCAL (<= 50 km)    Y   Y   Y   Y   N   N   N   N
 * C2 billable weight <= 10 kg    Y   Y   N   N   Y   Y   N   N
 * C3 parcel is NOT fragile       Y   N   Y   N   Y   N   Y   N
 * A1 accepted                    X
 * A2 UnsupportedServiceException     X   X   X   X   X   X   X
 * </pre>
 * Rule R3 deliberately uses a parcel whose ACTUAL weight is small (2 kg) but whose
 * BILLABLE (volumetric) weight is 24 kg, so the condition is tested on the correct quantity.
 */
@DisplayName("SAME_DAY eligibility - decision table (FR-10)")
class SameDayDecisionTableTest {

    private final ShippingCostCalculator calculator = new ShippingCostCalculator();

    private static Shipment sameDay(int km, double kg, int l, int w, int h, boolean fragile) {
        return Shipment.builder().service(ServiceType.SAME_DAY)
                .distance(km).weight(kg).dimensions(l, w, h).fragile(fragile).build();
    }

    @Test
    @DisplayName("TC-S-01 [DT R1] LOCAL, light, not fragile -> accepted, 0 delivery days")
    void rule1Accepted() {
        PriceBreakdown p = calculator.calculate(sameDay(10, 2.0, 20, 20, 10, false));
        assertEquals(0, p.estimatedDeliveryDays());
    }

    @ParameterizedTest(name = "{0} [DT {1}] {2} km, {3} kg, {4}x{5}x{6} cm, fragile={7} -> rejected")
    @CsvSource({
            "TC-S-02, R2, 10,  2.0,  20, 20, 10, true",
            "TC-S-03, R3, 10,  2.0,  60, 40, 30, false",
            "TC-S-04, R3b,10,  12.0, 20, 20, 10, false",
            "TC-S-05, R4, 10,  12.0, 20, 20, 10, true",
            "TC-S-06, R5, 120, 2.0,  20, 20, 10, false",
            "TC-S-07, R6, 120, 2.0,  20, 20, 10, true",
            "TC-S-08, R7, 120, 12.0, 20, 20, 10, false",
            "TC-S-09, R8, 120, 12.0, 20, 20, 10, true"
    })
    void otherRulesAreRejected(String id, String rule, int km, double kg, int l, int w, int h, boolean fragile) {
        assertThrows(UnsupportedServiceException.class, () -> calculator.calculate(sameDay(km, kg, l, w, h, fragile)));
    }

    @ParameterizedTest(name = "{0} [BVA] {1}: {2} km, {3} kg -> accepted={4}")
    @CsvSource({
            "TC-S-10, distance at LOCAL edge,          50, 2.0,  true",
            "TC-S-11, distance just outside LOCAL,     51, 2.0,  false",
            "TC-S-12, billable weight at limit,        10, 10.0, true",
            "TC-S-13, billable weight just over limit, 10, 10.1, false"
    })
    void sameDayBoundaries(String id, String description, int km, double kg, boolean accepted) {
        Shipment s = sameDay(km, kg, 20, 20, 10, false);
        if (accepted) {
            assertDoesNotThrow(() -> calculator.calculate(s));
        } else {
            assertThrows(UnsupportedServiceException.class, () -> calculator.calculate(s));
        }
    }

    @Test
    @DisplayName("TC-S-14 [EP] the SAME_DAY restrictions do not apply to EXPRESS (national, heavy, fragile)")
    void restrictionsOnlyApplyToSameDay() {
        Shipment s = Shipment.builder().service(ServiceType.EXPRESS)
                .distance(900).weight(25.0).fragile(true).build();
        assertDoesNotThrow(() -> calculator.calculate(s));
    }
}
