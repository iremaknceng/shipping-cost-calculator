package com.svv.shipping.model;

import java.math.BigDecimal;

/**
 * Result of a price calculation. All money values are in TL with scale 2.
 */
public record PriceBreakdown(
        BigDecimal billableWeightKg,
        Zone zone,
        BigDecimal baseCost,
        BigDecimal transportCost,
        BigDecimal fragileSurcharge,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal insuranceFee,
        BigDecimal total,
        int estimatedDeliveryDays) {

    @Override
    public String toString() {
        return String.format(
                "billable=%s kg, zone=%s, base=%s, transport=%s, fragile=%s, subtotal=%s, "
                        + "discount=-%s, insurance=%s, TOTAL=%s TL, delivery=%d day(s)",
                billableWeightKg, zone, baseCost, transportCost, fragileSurcharge, subtotal,
                discount, insuranceFee, total, estimatedDeliveryDays);
    }
}
