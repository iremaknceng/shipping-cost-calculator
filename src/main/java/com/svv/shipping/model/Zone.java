package com.svv.shipping.model;

import java.math.BigDecimal;

/**
 * Distance zones (FR-08) and their delivery-time table (FR-15).
 */
public enum Zone {
    LOCAL(1, 50, new BigDecimal("1.0"), 1, 1),
    REGIONAL(51, 300, new BigDecimal("1.3"), 2, 1),
    NATIONAL(301, 2000, new BigDecimal("1.6"), 3, 2);

    private final int minKm;
    private final int maxKm;
    private final BigDecimal multiplier;
    private final int standardDays;
    private final int expressDays;

    Zone(int minKm, int maxKm, BigDecimal multiplier, int standardDays, int expressDays) {
        this.minKm = minKm;
        this.maxKm = maxKm;
        this.multiplier = multiplier;
        this.standardDays = standardDays;
        this.expressDays = expressDays;
    }

    /**
     * Maps a distance in kilometres to its zone.
     *
     * @throws IllegalArgumentException if the distance is outside 1..2000 km
     */
    public static Zone fromDistance(int distanceKm) {
        for (Zone zone : values()) {
            if (distanceKm >= zone.minKm && distanceKm <= zone.maxKm) {
                return zone;
            }
        }
        throw new IllegalArgumentException("Distance must be between 1 and 2000 km: " + distanceKm);
    }

    public BigDecimal multiplier() {
        return multiplier;
    }

    /** Estimated delivery time in working days for the given service in this zone. */
    public int deliveryDays(ServiceType service) {
        switch (service) {
            case SAME_DAY:
                return 0;
            case EXPRESS:
                return expressDays;
            default:
                return standardDays;
        }
    }
}
