package com.wearemachina.workorders.model;

/**
 * Mutable per-golem state. Persisted as a single PDC blob on the golem entity and mutated in place
 * (read &rarr; mutate &rarr; write eagerly on each change). Lives in the entity's NBT, so it survives
 * chunk unload and server restart for free.
 */
public final class GolemState {

    public static final int CURRENT_SCHEMA = 1;

    private RoleType role;             // null = unassigned
    private BindTarget source;         // null = unset
    private BindTarget dest;           // null = unset
    private ItemFilter filter = ItemFilter.empty();
    private int threshold;             // restocker target count (0 = use config default)
    private boolean follow;
    private boolean carrying;          // true => the main-hand item is REAL cargo, not the cosmetic role label
    private java.util.UUID owner;      // the player who first tasked this golem (ownership for re-tasking)
    private org.bukkit.Material labelItem; // item shown in the golem's hand when idle (the role label)
    private final java.util.List<BindTarget> sortDests = new java.util.ArrayList<>(); // Sorter: destination chests
    private final java.util.List<java.util.UUID> trusted = new java.util.ArrayList<>(); // friends allowed to re-task

    public static GolemState empty() {
        return new GolemState();
    }

    /** Sorter destinations (mutable list); each is a chest seeded with the item type it should receive. */
    public java.util.List<BindTarget> sortDests() {
        return sortDests;
    }

    public void addSortDest(BindTarget t) {
        if (t != null && !sortDests.contains(t)) {
            sortDests.add(t);
        }
    }

    public void clearSortDests() {
        sortDests.clear();
    }

    /** Players (besides the owner) allowed to re-task this golem. */
    public java.util.List<java.util.UUID> trusted() {
        return trusted;
    }

    /** @return true if newly added, false if it was already present (i.e. toggled off — removed). */
    public boolean toggleTrusted(java.util.UUID id) {
        if (trusted.remove(id)) {
            return false;
        }
        trusted.add(id);
        return true;
    }

    public RoleType role() {
        return role;
    }

    public void role(RoleType r) {
        this.role = r;
    }

    public BindTarget source() {
        return source;
    }

    public void source(BindTarget s) {
        this.source = s;
    }

    public BindTarget dest() {
        return dest;
    }

    public void dest(BindTarget d) {
        this.dest = d;
    }

    public ItemFilter filter() {
        return filter;
    }

    public void filter(ItemFilter f) {
        this.filter = (f == null) ? ItemFilter.empty() : f;
    }

    public int threshold() {
        return threshold;
    }

    public void threshold(int t) {
        this.threshold = Math.max(0, t);
    }

    public boolean follow() {
        return follow;
    }

    public void follow(boolean f) {
        this.follow = f;
    }

    public boolean carrying() {
        return carrying;
    }

    public void carrying(boolean c) {
        this.carrying = c;
    }

    public java.util.UUID owner() {
        return owner;
    }

    public void owner(java.util.UUID o) {
        this.owner = o;
    }

    public org.bukkit.Material labelItem() {
        return labelItem;
    }

    public void labelItem(org.bukkit.Material m) {
        this.labelItem = m;
    }

    public boolean assigned() {
        return role != null;
    }

    public boolean hasRoute() {
        return source != null && dest != null;
    }
}
