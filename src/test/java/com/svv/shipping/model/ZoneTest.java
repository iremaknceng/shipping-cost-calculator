package com.svv.shipping.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FR-08 distance zones (BVA on every zone edge) and FR-15 delivery-time table (decision table).
 */
@DisplayName("Zone - distance zones (FR-08) and delivery time (FR-15)")
class ZoneTest {

    @ParameterizedTest(name = "{0} [BVA] {1} km -> {2}")
    @CsvSource({
            "TC-Z-01, 1,    LOCAL",
            "TC-Z-02, 25,   LOCAL",
            "TC-Z-03, 49,   LOCAL",
            "TC-Z-04, 50,   LOCAL",
            "TC-Z-05, 51,   REGIONAL",
            "TC-Z-06, 52,   REGIONAL",
            "TC-Z-07, 299,  REGIONAL",
            "TC-Z-08, 300,  REGIONAL",
            "TC-Z-09, 301,  NATIONAL",
            "TC-Z-10, 2000, NATIONAL"
    })
    void distanceIsMappedToZone(String id, int km, Zone expected) {
        assertEquals(expected, Zone.fromDistance(km));
    }

    @ParameterizedTest(name = "TC-Z-11.{index} [NEG] {0} km has no zone")
    @ValueSource(ints = {0, -1, 2001})
    void outOfRangeDistanceHasNoZone(int km) {
        assertThrows(IllegalArgumentException.class, () -> Zone.fromDistance(km));
    }

    @ParameterizedTest(name = "{0} [DT] {1} + {2} -> {3} day(s)")
    @CsvSource({
            "TC-T-01, LOCAL,    STANDARD, 1",
            "TC-T-02, REGIONAL, STANDARD, 2",
            "TC-T-03, NATIONAL, STANDARD, 3",
            "TC-T-04, LOCAL,    EXPRESS,  1",
            "TC-T-05, REGIONAL, EXPRESS,  1",
            "TC-T-06, NATIONAL, EXPRESS,  2",
            "TC-T-07, LOCAL,    SAME_DAY, 0"
    })
    void deliveryDaysFollowTable(String id, Zone zone, ServiceType service, int expectedDays) {
        assertEquals(expectedDays, zone.deliveryDays(service));
    }
}
