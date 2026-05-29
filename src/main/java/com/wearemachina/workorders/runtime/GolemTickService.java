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
import org.bukkit.block.Block;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

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
            UUID id = ids.get(i);
            if (!(Bukkit.getEntity(id) instanceof CopperGolem golem) || golem.isDead()) {
                continue; // not loaded / gone — lifecycle listener keeps the registry honest
            }
            if (golem.getVehicle() != null || golem.isLeashed()) {
                continue; // being transported — don't fight the player relocating it
            }
            GolemState state = store.read(golem);
            // Following golems heel every cycle (so the walk stays smooth) and skip job work; only the
            // sliced job path is rate-limited. We read state for every loaded golem each cycle to learn
            // who's following — fine at this server's golem counts; cache a follow-set if that grows huge.
            if (state.follow()) {
                tickFollow(golem, state, c);
                continue;
            }
            if (!Slice.shouldService(i, fireCounter, slices)) {
                continue;
            }
            serviceOne(id, golem, state, c);
        }
        fireCounter++;
    }

    private void serviceOne(UUID id, CopperGolem golem, GolemState state, PluginConfig c) {
        if (!state.assigned()) {
            return; // nothing bound yet
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

    /**
     * Walk a following golem toward its owner, teleporting it to catch up when it falls past the max
     * distance, has no walkable path, or is in another world. The teleport lands just inside the max
     * distance directly behind the owner — out of their view and a short walk away — so the owner sees
     * the golem stroll in rather than pop into existence (mirrors how a vanilla wolf rejoins its owner).
     */
    private void tickFollow(CopperGolem golem, GolemState state, PluginConfig c) {
        Player owner = state.owner() == null ? null : Bukkit.getPlayer(state.owner());
        if (owner == null || !owner.isOnline()) {
            return; // owner away — idle in place, but stay in follow mode for when they return
        }
        Location gl = golem.getLocation();
        Location pl = owner.getLocation();
        boolean sameWorld = gl.getWorld() != null && gl.getWorld().equals(pl.getWorld());
        double dist = sameWorld ? gl.distance(pl) : Double.MAX_VALUE;
        if (!sameWorld || dist > c.followMaxDistance) {
            teleportToHeel(golem, owner, c); // fell behind / crossed worlds — catch up behind them
            return;
        }
        if (dist <= c.followStopDistance) {
            golem.getPathfinder().stopPathfinding(); // close enough — heel without crowding or jitter
            golem.lookAt(owner);
            return;
        }
        if (!golem.getPathfinder().moveTo(pl, c.moveSpeed)) {
            teleportToHeel(golem, owner, c); // wall or gap in the way — catch up instead of stalling
        }
    }

    /** Place the golem just inside follow range, behind the owner's view, then send it walking in. */
    private void teleportToHeel(CopperGolem golem, Player owner, PluginConfig c) {
        Location spot = heelSpot(owner, c.followMaxDistance - 2.0);
        if (spot == null) {
            return; // nowhere safe to land right now — try again next cycle
        }
        golem.teleport(spot);
        golem.getPathfinder().moveTo(owner.getLocation(), c.moveSpeed);
    }

    /**
     * A standable spot {@code distance} blocks behind the owner (opposite their look direction), facing
     * back toward them. Searches inward in 2-block steps and a few blocks vertically for footing;
     * returns null if nothing safe is found.
     */
    private static Location heelSpot(Player owner, double distance) {
        Location base = owner.getLocation();
        World w = base.getWorld();
        if (w == null) {
            return null;
        }
        Vector behind = base.getDirection().setY(0);
        if (behind.lengthSquared() < 1.0e-6) {
            behind = new Vector(0, 0, 1);
        }
        behind.normalize().multiply(-1); // opposite of where the owner is looking
        for (double d = distance; d >= 2.0; d -= 2.0) {
            Location cand = base.clone().add(behind.clone().multiply(d));
            Location safe = standable(w, cand);
            if (safe != null) {
                safe.setDirection(base.toVector().subtract(safe.toVector()).setY(0)); // face the owner
                return safe;
            }
        }
        return null;
    }

    /** Nearest standable location to {@code loc} (feet + head clear, solid floor) within a few blocks of its Y. */
    private static Location standable(World w, Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int baseY = loc.getBlockY();
        for (int dy = 0; dy <= 3; dy++) {
            for (int dir = 1; dir >= -1; dir -= 2) {
                int y = baseY + dy * dir;
                Block below = w.getBlockAt(x, y - 1, z);
                if (w.getBlockAt(x, y, z).isPassable()
                        && w.getBlockAt(x, y + 1, z).isPassable()
                        && below.getType().isSolid()) {
                    return new Location(w, x + 0.5, y, z + 0.5);
                }
                if (dy == 0) {
                    break; // y + 0 only needs one pass
                }
            }
        }
        return null;
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
