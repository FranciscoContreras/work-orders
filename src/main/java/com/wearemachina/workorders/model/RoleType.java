package com.wearemachina.workorders.model;

/** The jobs a copper golem can be assigned. Persisted in PDC by ordinal (-1 = unassigned). */
public enum RoleType {
    /** Move items from a source container to a destination container (optional filter). */
    COURIER,
    /** Keep a furnace / smoker / blast furnace fed (fuel + smeltables) from a source. */
    STOKER,
    /** Top a destination container up to a threshold of the filtered item(s), pulling from a source. */
    RESTOCKER,
    /** Vacuum dropped items within a small radius and bank them in a container. */
    JANITOR,
    /** Sort one source's items across several destination chests by what each already holds. */
    SORTER,
    /** Harvest mature crops in a radius and replant them (produce drops for collection). */
    FARMHAND;

    /** @return the role for an ordinal byte, or {@code null} for -1 / out-of-range. */
    public static RoleType fromByte(byte b) {
        RoleType[] vals = values();
        return (b < 0 || b >= vals.length) ? null : vals[b];
    }

    /** Lower-case key used in permission nodes and config (e.g. {@code workorders.job.courier}). */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
