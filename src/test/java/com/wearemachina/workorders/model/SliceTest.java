package com.wearemachina.workorders.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The tick scheduler's correctness rides entirely on this math: get it wrong and golems are either
 * serviced twice a cycle (double work / dupes) or never (silently dead). Pure, so fully unit-testable.
 */
class SliceTest {

    @Test
    void slicesOfOneOrLessAlwaysService() {
        for (int i = 0; i < 16; i++) {
            assertTrue(Slice.shouldService(i, 0L, 1), "slices=1 must always service");
            assertTrue(Slice.shouldService(i, 5L, 0), "slices=0 must always service");
            assertTrue(Slice.shouldService(i, 5L, -3), "negative slices must always service");
        }
    }

    @Test
    void everyIndexServicedExactlyOncePerFullCycle() {
        int slices = 4;
        int golems = 25;
        for (int idx = 0; idx < golems; idx++) {
            int hits = 0;
            for (long fire = 0; fire < slices; fire++) {
                if (Slice.shouldService(idx, fire, slices)) {
                    hits++;
                }
            }
            assertEquals(1, hits, "index " + idx + " must be serviced exactly once per " + slices + " fires");
        }
    }

    @Test
    void eachFireServicesAnEvenPartition() {
        int slices = 4;
        int golems = 40; // evenly divisible -> exactly 10 per fire
        for (long fire = 0; fire < slices; fire++) {
            int serviced = 0;
            for (int idx = 0; idx < golems; idx++) {
                if (Slice.shouldService(idx, fire, slices)) {
                    serviced++;
                }
            }
            assertEquals(golems / slices, serviced, "fire " + fire + " should service an even slice");
        }
    }

    @Test
    void negativeFireCounterDoesNotThrowOrBreakRotation() {
        // floorMod keeps the rotation correct even if the fire counter were ever negative.
        assertDoesNotThrow(() -> Slice.shouldService(3, -7L, 4));
        int hits = 0;
        for (long fire = -4; fire < 0; fire++) {
            if (Slice.shouldService(2, fire, 4)) {
                hits++;
            }
        }
        assertEquals(1, hits, "rotation must still hit exactly once across a negative window");
    }

    @Test
    void withinRadiusHandlesBoundaryZeroAndDisabled() {
        assertTrue(Slice.withinRadius(0, 0, 0, 3, 0, 0, 3), "exactly on the boundary counts as within");
        assertFalse(Slice.withinRadius(0, 0, 0, 3.001, 0, 0, 3), "just outside is excluded");
        assertTrue(Slice.withinRadius(0, 0, 0, 0, 0, 0, 0), "zero radius, same point");
        assertFalse(Slice.withinRadius(0, 0, 0, 0.001, 0, 0, 0), "zero radius, any distance");
        assertTrue(Slice.withinRadius(0, 0, 0, 1000, 1000, 1000, -1), "negative radius disables gating");
        assertTrue(Slice.withinRadius(1, 2, 3, 2, 3, 4, 5), "3D distance under radius");
        assertFalse(Slice.withinRadius(0, 0, 0, 10, 0, 0, 5), "3D distance over radius");
    }
}
