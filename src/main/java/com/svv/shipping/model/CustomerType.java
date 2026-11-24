package com.svv.shipping.model;

import java.math.BigDecimal;

/**
 * Loyalty level of the sender (FR-12).
 * Each level has a normal discount rate and a higher rate that applies when the
 * transport subtotal reaches the high-volume threshold.
 */
public enum CustomerType {
    REGULAR(new BigDecimal("0.00"), new BigDecimal("0.00")),
    SILVER(new BigDecimal("0.05"), new BigDecimal("0.08")),
    GOLD(new BigDecimal("0.10"), new BigDecimal("0.15"));

    private final BigDecimal normalRate;
    private final BigDecimal highVolumeRate;

    CustomerType(BigDecimal normalRate, BigDecimal highVolumeRate) {
        this.normalRate = normalRate;
        this.highVolumeRate = highVolumeRate;
    }

    public BigDecimal normalRate() {
        return normalRate;
    }

    public BigDecimal highVolumeRate() {
        return highVolumeRate;
    }
}
