package com.wearemachina.workorders.model;

/** Pure scheduling math for the tick service: which golems run on a given fire, and distance gating. */
public final class Slice {

    private Slice() {
    }

    /**
     * Partition golems into {@code slices} groups by list index; one group runs per scheduler fire,
     * cycling. This bounds per-tick work regardless of how many golems exist.
     *
     * @return true if the golem at {@code index} should be serviced on fire number {@code fireCounter}.
     */
    public static boolean shouldService(int index, long fireCounter, int slices) {
        if (slices <= 1) {
            return true;
        }
        return Math.floorMod((long) index, slices) == Math.floorMod(fireCounter, slices);
    }

    /**
     * @return true if the squared distance between two points is within {@code radius}.
     *         A negative radius means "always" (gating disabled).
     */
    public static boolean withinRadius(double x1, double y1, double z1,
                                       double x2, double y2, double z2, double radius) {
        if (radius < 0) {
            return true;
        }
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    }
}
