package com.svv.shipping.validation;

import com.svv.shipping.model.Shipment;

import java.math.BigDecimal;

/**
 * Validates the raw input of a {@link Shipment} against requirements FR-01 .. FR-05.
 * The first violated rule is reported through an {@link InvalidShipmentException}.
 */
public class ShipmentValidator {

    public static final double MIN_WEIGHT_KG = 0.1;
    public static final double MAX_WEIGHT_KG = 30.0;
    public static final int MIN_DIMENSION_CM = 1;
    public static final int MAX_DIMENSION_CM = 150;
    public static final int MAX_DIMENSION_SUM_CM = 300;
    public static final int MIN_DISTANCE_KM = 1;
    public static final int MAX_DISTANCE_KM = 2000;
    public static final BigDecimal MAX_DECLARED_VALUE = new BigDecimal("50000.00");

    public void validate(Shipment shipment) {
        if (shipment == null) {
            throw new InvalidShipmentException("Shipment must not be null");
        }
        validateWeight(shipment.weightKg());
        validateDimensions(shipment.lengthCm(), shipment.widthCm(), shipment.heightCm());
        validateDistance(shipment.distanceKm());
        if (shipment.service() == null) {
            throw new InvalidShipmentException("Service type is required");
        }
        if (shipment.customer() == null) {
            throw new InvalidShipmentException("Customer type is required");
        }
        validateDeclaredValue(shipment.declaredValue(), shipment.insured());
    }

    /** FR-01: 0.1 kg &lt;= weight &lt;= 30.0 kg. NaN and infinity are rejected. */
    void validateWeight(double weightKg) {
        // Written as a negated range check so that NaN (for which every comparison is false) is rejected.
        if (!(weightKg >= MIN_WEIGHT_KG && weightKg <= MAX_WEIGHT_KG)) {
            throw new InvalidShipmentException(
                    "Weight must be between " + MIN_WEIGHT_KG + " and " + MAX_WEIGHT_KG + " kg: " + weightKg);
        }
    }

    /** FR-02: every side 1..150 cm and length + width + height &lt;= 300 cm. */
    void validateDimensions(int length, int width, int height) {
        checkSide("Length", length);
        checkSide("Width", width);
        checkSide("Height", height);
        int sum = length + width + height;
        if (sum > MAX_DIMENSION_SUM_CM) {
            throw new InvalidShipmentException(
                    "Sum of dimensions must not exceed " + MAX_DIMENSION_SUM_CM + " cm: " + sum);
        }
    }

    private void checkSide(String name, int value) {
        if (value < MIN_DIMENSION_CM || value > MAX_DIMENSION_CM) {
            throw new InvalidShipmentException(
                    name + " must be between " + MIN_DIMENSION_CM + " and " + MAX_DIMENSION_CM + " cm: " + value);
        }
    }

    /** FR-03: 1..2000 km. */
    void validateDistance(int distanceKm) {
        if (distanceKm < MIN_DISTANCE_KM || distanceKm > MAX_DISTANCE_KM) {
            throw new InvalidShipmentException(
                    "Distance must be between " + MIN_DISTANCE_KM + " and " + MAX_DISTANCE_KM + " km: " + distanceKm);
        }
    }

    /** FR-05: 0 &lt;= declared value &lt;= 50,000 TL; insured parcels need a value &gt; 0. */
    void validateDeclaredValue(BigDecimal value, boolean insured) {
        if (value == null) {
            throw new InvalidShipmentException("Declared value is required (use 0 if none)");
        }
        if (value.signum() < 0) {
            throw new InvalidShipmentException("Declared value must not be negative: " + value);
        }
        if (value.compareTo(MAX_DECLARED_VALUE) > 0) {
            throw new InvalidShipmentException("Declared value must not exceed " + MAX_DECLARED_VALUE + " TL: " + value);
        }
        if (insured && value.signum() == 0) {
            throw new InvalidShipmentException("Insured shipments must have a declared value greater than 0");
        }
    }
}
