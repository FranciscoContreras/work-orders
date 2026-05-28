package com.wearemachina.workorders.runtime;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.DepositPolicy;
import com.wearemachina.workorders.interact.Feedback;
import com.wearemachina.workorders.model.BindTarget;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.Slice;
import com.wearemachina.workorders.persistence.GolemStore;
import com.wearemachina.workorders.role.Role;
import com.wearemachina.workorders.role.RoleRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Drives assigned golems on a repeating timer. Per fire it processes a rotating <i>slice</i> of the
 * registry (bounding per-tick cost regardless of golem count) and skips any golem with no player nearby
 * — which is both the performance lever and the economy guard ("hibernate when unobserved", so nothing
 * flows while the area is unloaded/offline). All work is on the main thread.
 */
public final class GolemTickService {

    private final Plugin plugin;
    private final GolemRegistry registry;
    private final RoleRegistry roles;
    private final ConfigHolder cfg;
    private final GolemStore store;
    private final ContainerAccess containers;
    private final DepositPolicy deposit;
    private final Feedback fx;
    private final StatusBoard board;
    private final Map<UUID, Integer> pathFails = new HashMap<>();

    private int taskId = -1;
    private long fireCounter = 0L;

    public GolemTickService(Plugin plugin, GolemRegistry registry, RoleRegistry roles, ConfigHolder cfg,
                            GolemStore store, ContainerAccess containers, DepositPolicy deposit, Feedback fx,
                            StatusBoard board) {
        this.plugin = plugin;
        this.registry = registry;
        this.roles = roles;
        this.cfg = cfg;
        this.store = store;
        this.containers = containers;
        this.deposit = deposit;
        this.fx = fx;
        this.board = board;
    }

    public void start() {
        stop();
        int interval = cfg.get().serviceIntervalTicks;
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::run, interval, interval).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void run() {
        PluginConfig c = cfg.get();
        List<UUID> ids = registry.snapshot();
        int slices = c.slices;
        for (int i = 0; i < ids.size(); i++) {
            if (!Slice.shouldService(i, fireCounter, slices)) {
                continue;
            }
            serviceOne(ids.get(i), c);
        }
        fireCounter++;
    }

    private void serviceOne(UUID id, PluginConfig c) {
        if (!(Bukkit.getEntity(id) instanceof CopperGolem golem) || golem.isDead()) {
            return; // not loaded / gone — lifecycle listener keeps the registry honest
        }
        if (golem.getVehicle() != null || golem.isLeashed()) {
            return; // being transported — don't fight the player relocating it
        }
        GolemState state = store.read(golem);
        if (!state.assigned() || state.follow()) {
            return; // unassigned, or following the owner (hauling paused while following)
        }
        Role role = roles.get(state.role());
        if (role == null) {
            return;
        }
        if (!playerNear(golem, state, c.activeRadius)) {
            return; // hibernate when unobserved
        }
        if (c.efficiencyScalesWithOxidation && fireCounter % weatherFactor(golem) != 0L) {
            return; // a tarnished golem works more slowly
        }
        GolemContext ctx = new GolemContext(golem, state, c, containers, deposit, fx, store, board);
        try {
            role.tick(ctx);
        } catch (Throwable t) {
            plugin.getLogger().warning("Work Orders role tick failed for " + id + ": " + t);
            return;
        }
        trackStuck(id, golem, ctx, c);
    }

    /** Count consecutive cycles where pathfinding failed; surface "stuck" once the grace is exceeded. */
    private void trackStuck(UUID id, CopperGolem golem, GolemContext ctx, PluginConfig c) {
        if (ctx.movedFailed()) {
            int n = pathFails.merge(id, 1, Integer::sum);
            if (n >= c.pathFailGrace) {
                ctx.status("stuck");
                if (n == c.pathFailGrace) {
                    fx.stuck(golem); // a single frustrated reaction when it first gives up
                }
            }
        } else {
            pathFails.remove(id);
        }
    }

    private static long weatherFactor(CopperGolem golem) {
        return switch (golem.getWeatheringState()) {
            case EXPOSED -> 2L;
            case WEATHERED -> 3L;
            default -> 1L; // UNAFFECTED = full speed; OXIDIZED is frozen and no-ops anyway
        };
    }

    private boolean playerNear(CopperGolem golem, GolemState state, double radius) {
        Location gl = golem.getLocation();
        if (anyPlayerWithin(golem.getWorld(), gl.getX(), gl.getY(), gl.getZ(), radius)) {
            return true;
        }
        return targetHasPlayer(state.source(), radius) || targetHasPlayer(state.dest(), radius);
    }

    private boolean targetHasPlayer(BindTarget target, double radius) {
        if (target == null) {
            return false;
        }
        World w = target.resolveWorld();
        return w != null && anyPlayerWithin(w, target.x() + 0.5, target.y() + 0.5, target.z() + 0.5, radius);
    }

    private static boolean anyPlayerWithin(World world, double x, double y, double z, double radius) {
        if (world == null) {
            return false;
        }
        for (Player p : world.getPlayers()) {
            Location pl = p.getLocation();
            if (Slice.withinRadius(pl.getX(), pl.getY(), pl.getZ(), x, y, z, radius)) {
                return true;
            }
        }
        return false;
    }
}
