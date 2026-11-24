package com.svv.shipping.service;

import com.svv.shipping.model.CustomerType;
import com.svv.shipping.model.PriceBreakdown;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import com.svv.shipping.model.Zone;
import com.svv.shipping.validation.InvalidShipmentException;
import com.svv.shipping.validation.ShipmentValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression suite: one test per defect found in version 1.0.0 (see the defect log in the report).
 * These tests failed on v1.0.0 and pass on v1.1.0. They are tagged so they can be run alone:
 * <pre>mvn test -Dgroups=regression</pre>
 */
@Tag("regression")
@DisplayName("Regression suite - defects D1..D8 from v1.0.0")
class DefectRegressionTest {

    private final ShippingCostCalculator calculator = new ShippingCostCalculator();
    private final ShipmentValidator validator = new ShipmentValidator();

    @Test
    @DisplayName("REG-D1 50 km belongs to the LOCAL zone (was REGIONAL)")
    void d1LocalZoneUpperBoundary() {
        assertEquals(Zone.LOCAL, Zone.fromDistance(50));
    }

    @Test
    @DisplayName("REG-D2 minimum weight 0.1 kg is accepted (was rejected)")
    void d2MinimumWeightAccepted() {
        assertDoesNotThrow(() -> validator.validate(Shipment.builder().weight(0.1).build()));
    }

    @Test
    @DisplayName("REG-D3 NaN weight is rejected by the validator (was accepted)")
    void d3NanWeightRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> validator.validate(Shipment.builder().weight(Double.NaN).build()));
    }

    @Test
    @DisplayName("REG-D4 billable weight 2.1 kg is rounded UP to 2.5 kg (was rounded to 2.0 kg)")
    void d4BillableWeightRoundedUp() {
        PriceBreakdown p = calculator.calculate(Shipment.builder().weight(2.1).dimensions(10, 10, 10).build());
        assertEquals(new BigDecimal("2.5"), p.billableWeightKg());
    }

    @Test
    @DisplayName("REG-D5 SAME_DAY uses billable weight: 2 kg box of 60x40x30 cm (24 kg) is rejected")
    void d5SameDayUsesBillableWeight() {
        Shipment s = Shipment.builder().service(ServiceType.SAME_DAY).distance(10)
                .weight(2.0).dimensions(60, 40, 30).build();
        assertThrows(UnsupportedServiceException.class, () -> calculator.calculate(s));
    }

    @Test
    @DisplayName("REG-D6 subtotal of exactly 500.00 TL gets the high-volume rate (GOLD 15%, was 10%)")
    void d6ThresholdInclusive() {
        assertEquals(new BigDecimal("0.15"), calculator.discountRate(CustomerType.GOLD, new BigDecimal("500.00")));
        assertEquals(new BigDecimal("0.08"), calculator.discountRate(CustomerType.SILVER, new BigDecimal("500.00")));
    }

    @Test
    @DisplayName("REG-D7 discount is not applied to the insurance fee (total 543.93 TL, was 531.93 TL)")
    void d7DiscountExcludesInsurance() {
        PriceBreakdown p = calculator.calculate(Shipment.builder()
                .weight(12.0).dimensions(60, 40, 40).distance(450)
                .service(ServiceType.EXPRESS).customer(CustomerType.GOLD)
                .fragile(true).insured(true).declaredValue("8000").build());
        assertEquals(new BigDecimal("81.87"), p.discount());
        assertEquals(new BigDecimal("543.93"), p.total());
    }

    @Test
    @DisplayName("REG-D8 insured parcel with declared value 0 is rejected (was accepted)")
    void d8InsuredZeroValueRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> validator.validate(Shipment.builder().insured(true).declaredValue("0").build()));
    }
}
