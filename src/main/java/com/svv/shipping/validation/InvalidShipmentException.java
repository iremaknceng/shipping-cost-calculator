package com.svv.shipping.validation;

/**
 * Thrown when a shipment violates an input-validation rule (FR-01 .. FR-05, FR-07a).
 */
public class InvalidShipmentException extends IllegalArgumentException {
    public InvalidShipmentException(String message) {
        super(message);
    }
}
