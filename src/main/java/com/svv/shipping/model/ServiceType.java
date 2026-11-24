package com.svv.shipping.model;

import java.math.BigDecimal;

/**
 * Delivery service levels offered by the courier (FR-09, FR-15).
 */
public enum ServiceType {
    STANDARD(new BigDecimal("1.0")),
    EXPRESS(new BigDecimal("1.5")),
    SAME_DAY(new BigDecimal("2.0"));

    private final BigDecimal multiplier;

    ServiceType(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    /** Price multiplier applied to the base tariff. */
    public BigDecimal multiplier() {
        return multiplier;
    }
}
