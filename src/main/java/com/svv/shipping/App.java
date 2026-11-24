package com.svv.shipping;

import com.svv.shipping.model.CustomerType;
import com.svv.shipping.model.ServiceType;
import com.svv.shipping.model.Shipment;
import com.svv.shipping.service.ShippingCostCalculator;

/**
 * Console demo. Run with: java -jar target/shipping-cost-calculator-1.1.0.jar
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        ShippingCostCalculator calculator = new ShippingCostCalculator();

        Shipment[] samples = {
            Shipment.builder().weight(2.0).dimensions(30, 20, 10).distance(30).build(),
            Shipment.builder().weight(12.0).dimensions(60, 40, 40).distance(450)
                    .service(ServiceType.EXPRESS).customer(CustomerType.GOLD)
                    .fragile(true).insured(true).declaredValue("8000").build(),
            Shipment.builder().weight(2.0).dimensions(60, 40, 30).distance(20)
                    .service(ServiceType.SAME_DAY).build(),
            Shipment.builder().weight(0.0).build()
        };

        for (Shipment s : samples) {
            try {
                System.out.println(s);
                System.out.println("  -> " + calculator.calculate(s));
            } catch (RuntimeException e) {
                System.out.println("  -> REJECTED: " + e.getMessage());
            }
        }
    }
}
