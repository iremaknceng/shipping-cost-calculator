package com.svv.shipping.service;

import com.svv.shipping.model.CustomerType;
import com.svv.shipping.model.PriceBreakdown;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import com.svv.shipping.model.Zone;
import com.svv.shipping.validation.InvalidShipmentException;
import com.svv.shipping.validation.ShipmentValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the price of a shipment according to requirements FR-06 .. FR-15.
 */
public class ShippingCostCalculator {

    static final BigDecimal VOLUMETRIC_DIVISOR = new BigDecimal("3000");
    static final BigDecimal MAX_BILLABLE_WEIGHT_KG = new BigDecimal("50.0");
    static final BigDecimal SAME_DAY_MAX_WEIGHT_KG = new BigDecimal("10.0");
    static final BigDecimal FRAGILE_SURCHARGE = new BigDecimal("25.00");
    static final BigDecimal HIGH_VOLUME_THRESHOLD = new BigDecimal("500.00");
    static final BigDecimal INSURANCE_RATE = new BigDecimal("0.01");
    static final BigDecimal MIN_INSURANCE_FEE = new BigDecimal("15.00");

    private static final BigDecimal HALF = new BigDecimal("0.5");

    private final ShipmentValidator validator;

    public ShippingCostCalculator() {
        this(new ShipmentValidator());
    }

    public ShippingCostCalculator(ShipmentValidator validator) {
        this.validator = validator;
    }

    /**
     * Validates the shipment and returns its full price breakdown.
     *
     * @throws InvalidShipmentException     if an input rule is violated
     * @throws UnsupportedServiceException if SAME_DAY is requested for an ineligible parcel
     */
    public PriceBreakdown calculate(Shipment shipment) {
        validator.validate(shipment);

        BigDecimal billable = billableWeight(shipment);
        if (billable.compareTo(MAX_BILLABLE_WEIGHT_KG) > 0) {
            throw new InvalidShipmentException(
                    "Billable weight exceeds " + MAX_BILLABLE_WEIGHT_KG + " kg (oversized parcel): " + billable);
        }
        Zone zone = Zone.fromDistance(shipment.distanceKm());
        checkServiceEligibility(shipment, zone, billable);

        BigDecimal base = baseCost(billable);
        BigDecimal transport = money(base.multiply(zone.multiplier()).multiply(shipment.service().multiplier()));
        BigDecimal fragile = shipment.fragile() ? FRAGILE_SURCHARGE : money(BigDecimal.ZERO);
        BigDecimal subtotal = transport.add(fragile);
        BigDecimal discount = money(subtotal.multiply(discountRate(shipment.customer(), subtotal)));
        BigDecimal insurance = shipment.insured() ? insuranceFee(shipment.declaredValue()) : money(BigDecimal.ZERO);
        BigDecimal total = subtotal.subtract(discount).add(insurance);

        return new PriceBreakdown(billable, zone, base, transport, fragile, subtotal, discount, insurance,
                total, zone.deliveryDays(shipment.service()));
    }

    /**
     * FR-06: billable weight = max(actual, L*W*H / 3000), rounded UP to the next 0.5 kg.
     */
    BigDecimal billableWeight(Shipment s) {
        BigDecimal actual = BigDecimal.valueOf(s.weightKg());
        BigDecimal volume = BigDecimal.valueOf((long) s.lengthCm() * s.widthCm() * s.heightCm());
        BigDecimal volumetric = volume.divide(VOLUMETRIC_DIVISOR, 4, RoundingMode.HALF_UP);
        BigDecimal heavier = actual.max(volumetric);
        // number of half-kilogram steps, rounded up
        BigDecimal steps = heavier.divide(HALF, 0, RoundingMode.CEILING);
        return steps.multiply(HALF).setScale(1, RoundingMode.UNNECESSARY);
    }

    /**
     * FR-07: piecewise tariff on the billable weight.
     * <pre>
     *   w &lt;= 1          : 40
     *   1  &lt; w &lt;= 5     : 40  + (w - 1)  * 8
     *   5  &lt; w &lt;= 15    : 72  + (w - 5)  * 6
     *   w &gt; 15          : 132 + (w - 15) * 5
     * </pre>
     */
    BigDecimal baseCost(BigDecimal w) {
        BigDecimal cost;
        if (w.compareTo(BigDecimal.ONE) <= 0) {
            cost = new BigDecimal("40");
        } else if (w.compareTo(new BigDecimal("5")) <= 0) {
            cost = new BigDecimal("40").add(w.subtract(BigDecimal.ONE).multiply(new BigDecimal("8")));
        } else if (w.compareTo(new BigDecimal("15")) <= 0) {
            cost = new BigDecimal("72").add(w.subtract(new BigDecimal("5")).multiply(new BigDecimal("6")));
        } else {
            cost = new BigDecimal("132").add(w.subtract(new BigDecimal("15")).multiply(new BigDecimal("5")));
        }
        return money(cost);
    }

    /**
     * FR-10: SAME_DAY is allowed only when the zone is LOCAL, the billable weight
     * is at most 10 kg and the parcel is not fragile.
     */
    void checkServiceEligibility(Shipment s, Zone zone, BigDecimal billable) {
        if (s.service() != ServiceType.SAME_DAY) {
            return;
        }
        if (zone != Zone.LOCAL) {
            throw new UnsupportedServiceException("SAME_DAY is only available in the LOCAL zone (<= 50 km)");
        }
        if (billable.compareTo(SAME_DAY_MAX_WEIGHT_KG) > 0) {
            throw new UnsupportedServiceException("SAME_DAY is limited to a billable weight of 10 kg: " + billable);
        }
        if (s.fragile()) {
            throw new UnsupportedServiceException("SAME_DAY is not available for fragile parcels");
        }
    }

    /** FR-12: loyalty discount; the higher rate applies when the subtotal is at least 500 TL. */
    BigDecimal discountRate(CustomerType customer, BigDecimal subtotal) {
        return subtotal.compareTo(HIGH_VOLUME_THRESHOLD) >= 0
                ? customer.highVolumeRate()
                : customer.normalRate();
    }

    /** FR-13: 1% of the declared value, at least 15.00 TL. */
    BigDecimal insuranceFee(BigDecimal declaredValue) {
        return money(declaredValue.multiply(INSURANCE_RATE).max(MIN_INSURANCE_FEE));
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
