package com.wearemachina.workorders.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Live, transient per-golem status (a short state key like "hauling" / "stuck" / "dest-full") that the
 * inspect interaction surfaces to the owner. Main-thread confined; not persisted (rebuilt as golems tick).
 */
public final class StatusBoard {

    private final Map<UUID, String> status = new HashMap<>();

    public void set(UUID id, String key) {
        status.put(id, key);
    }

    public String get(UUID id) {
        return status.getOrDefault(id, "idle");
    }

    public void clear(UUID id) {
        status.remove(id);
    }
}
