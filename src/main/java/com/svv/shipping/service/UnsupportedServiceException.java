package com.svv.shipping.service;

/**
 * Thrown when a valid shipment requests a service it is not eligible for (FR-10).
 */
public class UnsupportedServiceException extends RuntimeException {
    public UnsupportedServiceException(String message) {
        super(message);
    }
}
