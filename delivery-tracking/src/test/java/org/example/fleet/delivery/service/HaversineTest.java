package org.example.fleet.delivery.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineTest {
    @Test
    void samePointHasZeroDistance() {
        assertEquals(0, Haversine.distanceMeters(-25.2967, -57.6359, -25.2967, -57.6359));
    }

    @Test
    void calculatesKnownApproximateDistance() {
        double distance = Haversine.distanceMeters(-25.2967, -57.6359, -25.2977, -57.6359);
        assertTrue(distance > 110 && distance < 112);
    }
}
