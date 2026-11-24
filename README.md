# Shipping Cost Calculator (SVV Research Assignment)

A small Java 17 console application that validates a cargo shipment and calculates its price
(billable weight, distance zone, service level, fragile surcharge, loyalty discount, insurance)
and its estimated delivery time. The application is tested with **JUnit 5** and the coverage is
measured with **JaCoCo**. No GUI and no database are used.

## Requirements to build

- JDK 17 or newer (`java -version`)
- Apache Maven 3.8+ (`mvn -v`)

## Commands

| Goal | Command | Output |
|------|---------|--------|
| Compile and run all tests + coverage | `mvn clean test` | `target/surefire-reports/`, `target/site/jacoco/index.html` |
| Run only the regression suite | `mvn test -Dgroups=regression` | 8 regression tests |
| Build, test and enforce the 90 % branch-coverage rule | `mvn clean verify` | `target/shipping-cost-calculator-1.1.0.jar` |
| Run the console demo | `java -jar target/shipping-cost-calculator-1.1.0.jar` | sample price calculations |

## Project layout

```
pom.xml                                   Maven configuration (JUnit 5.11.3, Surefire, JaCoCo 0.8.12)
src/main/java/com/svv/shipping/
  App.java                                console demo (excluded from coverage, no logic)
  model/        Shipment, PriceBreakdown, Zone, ServiceType, CustomerType
  validation/   ShipmentValidator, InvalidShipmentException
  service/      ShippingCostCalculator, UnsupportedServiceException
src/test/java/com/svv/shipping/
  validation/ShipmentValidatorTest        EP, BVA, negative tests for FR-01..FR-05
  model/ZoneTest                          BVA on zone edges, delivery-time decision table
  service/BillableWeightAndTariffTest     BVA on weight rounding, tariff brackets, oversize limit
  service/DiscountDecisionTableTest       decision table for FR-12
  service/SameDayDecisionTableTest        8-rule decision table for FR-10
  service/ShippingCostCalculatorTest      end-to-end, insurance, invariants, negative tests
  service/DefectRegressionTest            @Tag("regression"), one test per defect D1..D8
docs/
  test-results/   console logs of the three test runs, JUnit XML reports, test-case catalogue (CSV)
  coverage/       JaCoCo HTML / CSV / XML report for v1.1.0
  defects/        diff of the fixes from v1.0.0 to v1.1.0
```

## Test runs included in `docs/test-results`

1. `run1-v1.0.0-initial.txt` – first version: 143 tests, **11 failed** (8 defects found)
1b. `run1b-v1.0.0-regression-suite-check.txt` – the new regression tests run against v1.0.0: **8 / 8 failed** (they reproduce D1–D8)
2. `run2-v1.1.0-full-suite.txt` – after the fixes and the new regression tests: **151 / 151 passed**
3. `run3-v1.1.0-regression-only.txt` – regression suite only: **8 / 8 passed**
