package com.svv.shipping.service;

import com.svv.shipping.model.CustomerType;
import com.svv.shipping.model.PriceBreakdown;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import com.svv.shipping.model.Zone;
import com.svv.shipping.validation.InvalidShipmentException;
import com.svv.shipping.validation.ShipmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end price calculation (FR-09, FR-11, FR-13, FR-14) and calculator-level negative tests.
 * Expected values were calculated by hand from the requirements, not copied from the program output.
 */
@DisplayName("ShippingCostCalculator - full price calculation")
class ShippingCostCalculatorTest {

    private ShippingCostCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ShippingCostCalculator();
    }

    private static BigDecimal tl(String value) {
        return new BigDecimal(value);
    }

    @Nested
    @DisplayName("End-to-end scenarios")
    class Scenarios {

        @Test
        @DisplayName("TC-E-01 [EP] simple local standard parcel: 2 kg, 30 km -> 48.00 TL")
        void simpleLocalStandard() {
            PriceBreakdown p = calculator.calculate(Shipment.builder().build());
            assertAll(
                    () -> assertEquals(tl("2.0"), p.billableWeightKg()),
                    () -> assertEquals(Zone.LOCAL, p.zone()),
                    () -> assertEquals(tl("48.00"), p.baseCost()),
                    () -> assertEquals(tl("48.00"), p.transportCost()),
                    () -> assertEquals(tl("0.00"), p.discount()),
                    () -> assertEquals(tl("48.00"), p.total()),
                    () -> assertEquals(1, p.estimatedDeliveryDays()));
        }

        @Test
        @DisplayName("TC-E-02 [EP] regional express: 3 kg, 120 km -> 56 x 1.3 x 1.5 = 109.20 TL, 1 day")
        void regionalExpress() {
            PriceBreakdown p = calculator.calculate(Shipment.builder()
                    .weight(3.0).dimensions(20, 20, 20).distance(120).service(ServiceType.EXPRESS).build());
            assertAll(
                    () -> assertEquals(Zone.REGIONAL, p.zone()),
                    () -> assertEquals(tl("109.20"), p.total()),
                    () -> assertEquals(1, p.estimatedDeliveryDays()));
        }

        @Test
        @DisplayName("TC-E-03 [EP] fragile surcharge of 25.00 TL is added: 48 + 25 = 73.00 TL")
        void fragileSurcharge() {
            PriceBreakdown p = calculator.calculate(Shipment.builder().fragile(true).build());
            assertAll(
                    () -> assertEquals(tl("25.00"), p.fragileSurcharge()),
                    () -> assertEquals(tl("73.00"), p.total()));
        }

        @Test
        @DisplayName("TC-E-04 [EP] SILVER below threshold gets 5%: 48.00 - 2.40 = 45.60 TL")
        void silverDiscount() {
            PriceBreakdown p = calculator.calculate(Shipment.builder().customer(CustomerType.SILVER).build());
            assertAll(
                    () -> assertEquals(tl("2.40"), p.discount()),
                    () -> assertEquals(tl("45.60"), p.total()));
        }

        @Test
        @DisplayName("TC-E-05 [EP] minimum insurance fee 15.00 TL applies for 1000 TL value: 63.00 TL")
        void minimumInsurance() {
            PriceBreakdown p = calculator.calculate(
                    Shipment.builder().insured(true).declaredValue("1000").build());
            assertAll(
                    () -> assertEquals(tl("15.00"), p.insuranceFee()),
                    () -> assertEquals(tl("63.00"), p.total()));
        }

        @Test
        @DisplayName("TC-E-06 [DT] GOLD, high volume, fragile, insured, volumetric, national express -> 543.93 TL")
        void complexScenario() {
            // billable = max(12, 60*40*40/3000 = 32) = 32 kg; base = 132 + 17*5 = 217
            // transport = 217 * 1.6 * 1.5 = 520.80; subtotal = 520.80 + 25 = 545.80 (>= 500 -> 15%)
            // discount = 81.87; insurance = 1% of 8000 = 80.00; total = 545.80 - 81.87 + 80.00 = 543.93
            PriceBreakdown p = calculator.calculate(Shipment.builder()
                    .weight(12.0).dimensions(60, 40, 40).distance(450)
                    .service(ServiceType.EXPRESS).customer(CustomerType.GOLD)
                    .fragile(true).insured(true).declaredValue("8000").build());
            assertAll(
                    () -> assertEquals(tl("32.0"), p.billableWeightKg()),
                    () -> assertEquals(Zone.NATIONAL, p.zone()),
                    () -> assertEquals(tl("217.00"), p.baseCost()),
                    () -> assertEquals(tl("520.80"), p.transportCost()),
                    () -> assertEquals(tl("545.80"), p.subtotal()),
                    () -> assertEquals(tl("81.87"), p.discount()),
                    () -> assertEquals(tl("80.00"), p.insuranceFee()),
                    () -> assertEquals(tl("543.93"), p.total()),
                    () -> assertEquals(2, p.estimatedDeliveryDays()));
        }

        @Test
        @DisplayName("TC-E-07 [EP] national standard: 5 kg, 301 km -> 72 x 1.6 = 115.20 TL, 3 days")
        void nationalStandard() {
            PriceBreakdown p = calculator.calculate(Shipment.builder().weight(5.0).distance(301).build());
            assertAll(
                    () -> assertEquals(tl("115.20"), p.total()),
                    () -> assertEquals(3, p.estimatedDeliveryDays()));
        }

        @Test
        @DisplayName("TC-E-08 [EP] valid same-day parcel: billable 1.5 kg, 10 km -> 44 x 2.0 = 88.00 TL")
        void sameDayPrice() {
            PriceBreakdown p = calculator.calculate(Shipment.builder()
                    .weight(1.0).dimensions(20, 20, 10).distance(10).service(ServiceType.SAME_DAY).build());
            assertEquals(tl("88.00"), p.total());
        }

        @Test
        @DisplayName("TC-E-09 [BVA] half-up rounding: 134.5 x 1.3 x 1.5 = 262.275 -> 262.28; SILVER 5% -> 249.17 TL")
        void roundingHalfUp() {
            PriceBreakdown p = calculator.calculate(Shipment.builder()
                    .weight(15.5).distance(100).service(ServiceType.EXPRESS).customer(CustomerType.SILVER).build());
            assertAll(
                    () -> assertEquals(tl("262.28"), p.transportCost()),
                    () -> assertEquals(tl("13.11"), p.discount()),
                    () -> assertEquals(tl("249.17"), p.total()));
        }
    }

    @Nested
    @DisplayName("FR-13 Insurance fee")
    class Insurance {

        @ParameterizedTest(name = "{0} [{1}] declared {2} TL -> fee {3} TL")
        @CsvSource({
                "TC-I-01, EP  minimum fee applies,   100,      15.00",
                "TC-I-02, BVA just below 1500,       1499.99,  15.00",
                "TC-I-03, BVA exactly 1500 (1%=15),  1500,     15.00",
                "TC-I-04, BVA just above 1500,       1500.50,  15.01",
                "TC-I-05, EP  percentage applies,    2000,     20.00",
                "TC-I-06, BVA max declared value,    50000,    500.00"
        })
        void insuranceFee(String id, String technique, String declared, String expected) {
            assertEquals(new BigDecimal(expected), calculator.insuranceFee(new BigDecimal(declared)));
        }

        @Test
        @DisplayName("TC-I-07 [EP] no insurance fee when the parcel is not insured, even with a declared value")
        void notInsured() {
            PriceBreakdown p = calculator.calculate(Shipment.builder().declaredValue("5000").insured(false).build());
            assertEquals(tl("0.00"), p.insuranceFee());
        }
    }

    @Nested
    @DisplayName("Invariants and negative tests")
    class InvariantsAndNegative {

        @ParameterizedTest(name = "TC-X-01.{index} [EP] total = subtotal - discount + insurance ({0}, {1}, {2} km)")
        @CsvSource({
                "STANDARD, REGULAR, 30,   false",
                "EXPRESS,  SILVER,  200,  true",
                "EXPRESS,  GOLD,    1500, true",
                "STANDARD, GOLD,    2000, false"
        })
        void totalIsConsistent(ServiceType service, CustomerType customer, int km, boolean insured) {
            PriceBreakdown p = calculator.calculate(Shipment.builder().weight(28.0).dimensions(50, 40, 30)
                    .service(service).customer(customer).distance(km).fragile(true)
                    .insured(insured).declaredValue(insured ? "12000" : "0").build());
            assertEquals(p.subtotal().subtract(p.discount()).add(p.insuranceFee()), p.total());
            assertTrue(p.total().signum() > 0);
            assertEquals(2, p.total().scale(), "money must have 2 decimals");
        }

        @Test
        @DisplayName("TC-X-02 [NEG] calculate(null) is rejected with InvalidShipmentException")
        void nullShipment() {
            assertThrows(InvalidShipmentException.class, () -> calculator.calculate(null));
        }

        @Test
        @DisplayName("TC-X-03 [NEG] invalid input never produces a price (validation runs first)")
        void invalidInputNeverPriced() {
            InvalidShipmentException ex = assertThrows(InvalidShipmentException.class,
                    () -> calculator.calculate(Shipment.builder().weight(-1).distance(0).build()));
            assertTrue(ex.getMessage().startsWith("Weight"), ex.getMessage());
        }

        @Test
        @DisplayName("TC-X-05 [EP] the price breakdown is printable and shows the total")
        void breakdownToString() {
            String text = calculator.calculate(Shipment.builder().build()).toString();
            assertTrue(text.contains("TOTAL=48.00 TL") && text.contains("zone=LOCAL"), text);
        }

        @Test
        @DisplayName("TC-X-04 [NEG] a calculator built with a custom validator still validates")
        void customValidatorIsUsed() {
            ShippingCostCalculator c = new ShippingCostCalculator(new ShipmentValidator());
            assertThrows(InvalidShipmentException.class,
                    () -> c.calculate(Shipment.builder().distance(5000).build()));
        }
    }
}
