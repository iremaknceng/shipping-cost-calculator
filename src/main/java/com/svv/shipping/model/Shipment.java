package com.svv.shipping.model;

import java.math.BigDecimal;

/**
 * Immutable description of a parcel to be shipped. Values are NOT validated here;
 * validation is the responsibility of {@link com.svv.shipping.validation.ShipmentValidator}
 * so that invalid objects can be constructed in tests.
 *
 * @param weightKg      actual weight in kilograms
 * @param lengthCm      length in centimetres
 * @param widthCm       width in centimetres
 * @param heightCm      height in centimetres
 * @param distanceKm    road distance between origin and destination
 * @param service       requested service level
 * @param customer      loyalty level of the sender
 * @param fragile       true if the parcel needs fragile handling
 * @param insured       true if the sender buys insurance
 * @param declaredValue declared value of the contents in TL
 */
public record Shipment(
        double weightKg,
        int lengthCm,
        int widthCm,
        int heightCm,
        int distanceKm,
        ServiceType service,
        CustomerType customer,
        boolean fragile,
        boolean insured,
        BigDecimal declaredValue) {

    /** Convenience builder so tests only state the fields they care about. */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double weightKg = 2.0;
        private int lengthCm = 30;
        private int widthCm = 20;
        private int heightCm = 10;
        private int distanceKm = 30;
        private ServiceType service = ServiceType.STANDARD;
        private CustomerType customer = CustomerType.REGULAR;
        private boolean fragile = false;
        private boolean insured = false;
        private BigDecimal declaredValue = BigDecimal.ZERO;

        public Builder weight(double kg) { this.weightKg = kg; return this; }
        public Builder dimensions(int l, int w, int h) { this.lengthCm = l; this.widthCm = w; this.heightCm = h; return this; }
        public Builder distance(int km) { this.distanceKm = km; return this; }
        public Builder service(ServiceType s) { this.service = s; return this; }
        public Builder customer(CustomerType c) { this.customer = c; return this; }
        public Builder fragile(boolean f) { this.fragile = f; return this; }
        public Builder insured(boolean i) { this.insured = i; return this; }
        public Builder declaredValue(String v) { this.declaredValue = v == null ? null : new BigDecimal(v); return this; }

        public Shipment build() {
            return new Shipment(weightKg, lengthCm, widthCm, heightCm, distanceKm,
                    service, customer, fragile, insured, declaredValue);
        }
    }
}
