package com.wearemachina.workorders.interact;

import java.util.UUID;

/**
 * A short-lived, per-player "I'm pointing this golem at containers" session. Enables the two-tap
 * source &rarr; destination flow without any UI. Expires after a TTL so a forgotten session doesn't
 * hijack the player's next chest click.
 */
public final class BindSession {

    public enum Stage { AWAIT_SOURCE, AWAIT_DEST }

    private final UUID golemId;
    private Stage stage;
    private final long expiresAtMillis;

    public BindSession(UUID golemId, Stage stage, long expiresAtMillis) {
        this.golemId = golemId;
        this.stage = stage;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID golemId() {
        return golemId;
    }

    public Stage stage() {
        return stage;
    }

    public void stage(Stage stage) {
        this.stage = stage;
    }

    public boolean expired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
