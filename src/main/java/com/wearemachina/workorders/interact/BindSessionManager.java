package com.wearemachina.workorders.interact;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player {@link BindSession} store with lazy expiry. Main-thread confined. */
public final class BindSessionManager {

    private final Map<UUID, BindSession> byPlayer = new HashMap<>();

    public void start(UUID player, BindSession session) {
        byPlayer.put(player, session);
    }

    /** @return the player's active session, or {@code null} if absent/expired (expired ones are evicted). */
    public BindSession get(UUID player) {
        BindSession s = byPlayer.get(player);
        if (s == null) {
            return null;
        }
        if (s.expired(System.currentTimeMillis())) {
            byPlayer.remove(player);
            return null;
        }
        return s;
    }

    public void clear(UUID player) {
        byPlayer.remove(player);
    }
}
