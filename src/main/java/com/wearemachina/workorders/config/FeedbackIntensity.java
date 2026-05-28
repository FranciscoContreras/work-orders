package com.wearemachina.workorders.config;

import java.util.Locale;

/** How loud the diegetic feedback (particles/sounds) is. Scales particle counts; OFF mutes everything. */
public enum FeedbackIntensity {
    OFF,
    SUBTLE,
    NORMAL,
    LIVELY;

    public static FeedbackIntensity from(String s) {
        if (s == null) {
            return NORMAL;
        }
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }

    public boolean enabled() {
        return this != OFF;
    }

    public double scale() {
        return switch (this) {
            case OFF -> 0.0;
            case SUBTLE -> 0.4;
            case NORMAL -> 1.0;
            case LIVELY -> 2.0;
        };
    }

    /** Scale a base particle count by this intensity (at least 1 when enabled). */
    public int particles(int base) {
        if (this == OFF) {
            return 0;
        }
        return Math.max(1, (int) Math.round(base * scale()));
    }
}
