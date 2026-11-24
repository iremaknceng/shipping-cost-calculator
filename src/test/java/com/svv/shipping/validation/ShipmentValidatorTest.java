package com.svv.shipping.validation;

import com.svv.shipping.model.CustomerType;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Input validation tests for FR-01 .. FR-05.
 * Techniques: Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing (NEG).
 */
@DisplayName("ShipmentValidator - input validation (FR-01..FR-05)")
class ShipmentValidatorTest {

    private ShipmentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ShipmentValidator();
    }

    private void assertRejected(Shipment s, String expectedMessagePart) {
        InvalidShipmentException ex = assertThrows(InvalidShipmentException.class, () -> validator.validate(s));
        assertTrue(ex.getMessage().contains(expectedMessagePart),
                () -> "Expected message to contain '" + expectedMessagePart + "' but was: " + ex.getMessage());
    }

    @Nested
    @DisplayName("FR-01 Weight")
    class Weight {

        @ParameterizedTest(name = "{0} [{1}] weight {2} kg is accepted")
        @CsvSource({
                "TC-W-01, EP  valid class,       15.0",
                "TC-W-02, BVA min,               0.1",
                "TC-W-03, BVA just above min,    0.11",
                "TC-W-04, BVA just below max,    29.99",
                "TC-W-05, BVA max,               30.0"
        })
        void validWeightsAreAccepted(String id, String technique, double weight) {
            assertDoesNotThrow(() -> validator.validate(Shipment.builder().weight(weight).build()));
        }

        @ParameterizedTest(name = "{0} [{1}] weight {2} kg is rejected")
        @CsvSource({
                "TC-W-06, BVA just below min,       0.09",
                "TC-W-07, BVA just above max,       30.01",
                "TC-W-08, EP  invalid: zero,        0.0",
                "TC-W-09, EP  invalid: negative,    -5.0",
                "TC-W-10, EP  invalid: too heavy,   45.0"
        })
        void invalidWeightsAreRejected(String id, String technique, double weight) {
            assertRejected(Shipment.builder().weight(weight).build(), "Weight");
        }

        @ParameterizedTest(name = "TC-W-11.{index} [NEG] non-finite weight {0} is rejected")
        @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
        void nonFiniteWeightsAreRejected(double weight) {
            assertRejected(Shipment.builder().weight(weight).build(), "Weight");
        }
    }

    @Nested
    @DisplayName("FR-02 Dimensions")
    class Dimensions {

        @ParameterizedTest(name = "{0} [{1}] dimensions {2}x{3}x{4} cm are accepted")
        @CsvSource({
                "TC-D-01, EP  valid class,         40,  30,  20",
                "TC-D-02, BVA side min,            1,   1,   1",
                "TC-D-03, BVA side min+1,          2,   2,   2",
                "TC-D-04, BVA side max-1,          149, 10,  10",
                "TC-D-05, BVA side max,            150, 100, 50",
                "TC-D-06, BVA sum max-1 (299),     150, 100, 49"
        })
        void validDimensionsAreAccepted(String id, String technique, int l, int w, int h) {
            assertDoesNotThrow(() -> validator.validate(Shipment.builder().weight(1.0).dimensions(l, w, h).build()));
        }

        @ParameterizedTest(name = "{0} [{1}] dimensions {2}x{3}x{4} cm are rejected")
        @CsvSource({
                "TC-D-07, BVA length below min,    0,   20,  10,  Length",
                "TC-D-08, BVA length above max,    151, 20,  10,  Length",
                "TC-D-09, BVA width below min,     30,  0,   10,  Width",
                "TC-D-10, BVA width above max,     30,  151, 10,  Width",
                "TC-D-11, BVA height below min,    30,  20,  0,   Height",
                "TC-D-12, BVA height above max,    30,  20,  151, Height",
                "TC-D-13, EP  negative side,       -5,  20,  10,  Length",
                "TC-D-14, BVA sum max+1 (301),     150, 100, 51,  Sum of dimensions"
        })
        void invalidDimensionsAreRejected(String id, String technique, int l, int w, int h, String field) {
            assertRejected(Shipment.builder().dimensions(l, w, h).build(), field);
        }
    }

    @Nested
    @DisplayName("FR-03 Distance")
    class Distance {

        @ParameterizedTest(name = "{0} [{1}] distance {2} km is accepted")
        @CsvSource({
                "TC-K-01, EP  valid class,   750",
                "TC-K-02, BVA min,           1",
                "TC-K-03, BVA min+1,         2",
                "TC-K-04, BVA max-1,         1999",
                "TC-K-05, BVA max,           2000"
        })
        void validDistancesAreAccepted(String id, String technique, int km) {
            assertDoesNotThrow(() -> validator.validate(Shipment.builder().distance(km).build()));
        }

        @ParameterizedTest(name = "{0} [{1}] distance {2} km is rejected")
        @CsvSource({
                "TC-K-06, BVA min-1,             0",
                "TC-K-07, BVA max+1,             2001",
                "TC-K-08, EP  invalid negative,  -10",
                "TC-K-09, EP  invalid too far,   5000"
        })
        void invalidDistancesAreRejected(String id, String technique, int km) {
            assertRejected(Shipment.builder().distance(km).build(), "Distance");
        }
    }

    @Nested
    @DisplayName("FR-04 Required fields")
    class RequiredFields {

        @Test
        @DisplayName("TC-N-01 [NEG] null shipment is rejected")
        void nullShipment() {
            assertThrows(InvalidShipmentException.class, () -> validator.validate(null));
        }

        @Test
        @DisplayName("TC-N-02 [NEG] missing service type is rejected")
        void nullService() {
            assertRejected(Shipment.builder().service(null).build(), "Service type");
        }

        @Test
        @DisplayName("TC-N-03 [NEG] missing customer type is rejected")
        void nullCustomer() {
            assertRejected(Shipment.builder().customer(null).build(), "Customer type");
        }

        @Test
        @DisplayName("TC-N-04 [EP] every service/customer combination with valid data is accepted")
        void allEnumValuesAccepted() {
            for (ServiceType s : ServiceType.values()) {
                for (CustomerType c : CustomerType.values()) {
                    assertDoesNotThrow(() -> validator.validate(Shipment.builder().service(s).customer(c).build()),
                            s + "/" + c);
                }
            }
        }
    }

    @Nested
    @DisplayName("FR-05 Declared value and insurance")
    class DeclaredValue {

        @ParameterizedTest(name = "{0} [{1}] declared value {2} TL (insured={3}) is accepted")
        @CsvSource({
                "TC-V-01, BVA min not insured,      0,        false",
                "TC-V-02, BVA min insured (0.01),   0.01,     true",
                "TC-V-03, EP  valid insured,        2500,     true",
                "TC-V-04, BVA max-0.01,             49999.99, true",
                "TC-V-05, BVA max,                  50000.00, true"
        })
        void validValuesAreAccepted(String id, String technique, String value, boolean insured) {
            assertDoesNotThrow(() -> validator.validate(
                    Shipment.builder().declaredValue(value).insured(insured).build()));
        }

        @ParameterizedTest(name = "{0} [{1}] declared value {2} TL (insured={3}) is rejected")
        @CsvSource({
                "TC-V-06, BVA below min,              -0.01,    false, negative",
                "TC-V-07, BVA above max,              50000.01, false, exceed",
                "TC-V-08, NEG insured without value,  0,        true,  Insured",
                "TC-V-09, EP  invalid negative,       -100,     true,  negative"
        })
        void invalidValuesAreRejected(String id, String technique, String value, boolean insured, String msg) {
            assertRejected(Shipment.builder().declaredValue(value).insured(insured).build(), msg);
        }

        @Test
        @DisplayName("TC-V-10 [NEG] missing declared value is rejected")
        void nullDeclaredValue() {
            assertRejected(Shipment.builder().declaredValue(null).build(), "Declared value");
        }
    }
}
