package com.wearemachina.workorders.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The set of currently-loaded, Work Orders-managed golem UUIDs. Main-thread confined (all access is from
 * Bukkit listeners and the scheduler, which run on the main thread). Stores UUIDs only — never Entity
 * references — to avoid leaking entities across chunk unloads.
 */
public final class GolemRegistry {

    private final Set<UUID> ids = new LinkedHashSet<>();

    public void add(UUID id) {
        ids.add(id);
    }

    public void remove(UUID id) {
        ids.remove(id);
    }

    public boolean contains(UUID id) {
        return ids.contains(id);
    }

    /** A stable-order copy safe to iterate while the registry mutates. */
    public List<UUID> snapshot() {
        return new ArrayList<>(ids);
    }

    public int size() {
        return ids.size();
    }
}
