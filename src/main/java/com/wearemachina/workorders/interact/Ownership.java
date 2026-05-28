package com.wearemachina.workorders.interact;

import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.model.GolemState;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Decides who may re-task a golem. Ownership is tracked in our own PDC ({@link GolemState#owner()}) — the
 * first player to task a golem becomes its owner — so it works on any Paper server (no dependency on
 * Purpur's vanilla-summoner method) and even when the golem has no recorded builder.
 */
public final class Ownership {

    public static final String ADMIN_PERMISSION = "workorders.admin";

    private Ownership() {
    }

    public static boolean isTrusted(Player player, GolemState state, PluginConfig cfg) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        if (!cfg.trustedOnlyRetask) {
            return true;
        }
        UUID owner = state.owner();
        // No owner yet (never tasked): anyone may claim it.
        if (owner == null || owner.equals(player.getUniqueId())) {
            return true;
        }
        return state.trusted().contains(player.getUniqueId()); // a friend the owner trusted
    }

    /** Stricter check for managing the golem itself (trust list, etc.): owner or admin only. */
    public static boolean isOwner(Player player, GolemState state) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        UUID owner = state.owner();
        return owner == null || owner.equals(player.getUniqueId());
    }
}
