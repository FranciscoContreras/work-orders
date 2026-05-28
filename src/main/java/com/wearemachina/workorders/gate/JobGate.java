package com.wearemachina.workorders.gate;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.model.RoleType;
import org.bukkit.entity.Player;

/**
 * Decides whether a player may assign a given job. v1 gates on a per-job permission
 * ({@code workorders.job.<role>}) which a server maps to its ranks; servers can disable gating entirely.
 * (An optional material/economy cost is a natural extension point here.)
 */
public final class JobGate {

    private final ConfigHolder cfg;

    public JobGate(ConfigHolder cfg) {
        this.cfg = cfg;
    }

    public boolean canAssign(Player player, RoleType role) {
        if (!cfg.get().gatingRequirePermission) {
            return true;
        }
        if (player.hasPermission("workorders.admin")) {
            return true;
        }
        return player.hasPermission("workorders.job." + role.key());
    }
}
