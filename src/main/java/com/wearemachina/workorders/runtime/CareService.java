package com.wearemachina.workorders.runtime;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.interact.Feedback;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.persistence.GolemStore;
import io.papermc.paper.world.WeatheringCopperState;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The "alive" layer: glows a frozen (fully-oxidised) golem so you can find it, breathes sluggish FX as it
 * tarnishes, and has it greet its owner. Runs at a slow cadence and only near players, so it's cheap and
 * never spammy. It only ever turns off the glow it itself turned on (so it won't fight the whistle).
 */
public final class CareService {

    private static final int INTERVAL_TICKS = 40; // ~2s

    private final Plugin plugin;
    private final GolemRegistry registry;
    private final ConfigHolder cfg;
    private final GolemStore store;
    private final Feedback fx;

    private final Set<UUID> frozenGlowing = new HashSet<>();
    private int taskId = -1;

    public CareService(Plugin plugin, GolemRegistry registry, ConfigHolder cfg, GolemStore store, Feedback fx) {
        this.plugin = plugin;
        this.registry = registry;
        this.cfg = cfg;
        this.store = store;
        this.fx = fx;
    }

    public void start() {
        stop();
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::run, INTERVAL_TICKS, INTERVAL_TICKS).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void run() {
        PluginConfig c = cfg.get();
        for (UUID id : registry.snapshot()) {
            if (!(Bukkit.getEntity(id) instanceof CopperGolem golem) || golem.isDead()) {
                frozenGlowing.remove(id);
                continue;
            }
            WeatheringCopperState weather = golem.getWeatheringState();
            boolean frozen = weather == WeatheringCopperState.OXIDIZED;

            if (c.frozenGlow) {
                manageFrozenGlow(golem, id, frozen);
            }
            if (!playerNear(golem, c.activeRadius)) {
                continue; // only emit FX where someone can enjoy them
            }
            if (c.sluggishFx && !frozen && tarnished(weather) && roll(0.25)) {
                fx.sluggish(golem);
            }
            if (!frozen && roll(0.15)) {
                Player passer = nearestPlayer(golem, c.activeRadius);
                if (passer != null) {
                    golem.lookAt(passer); // idle glance toward someone passing by
                }
            }
            greetOwnerIfNear(golem, c);
        }
    }

    private void manageFrozenGlow(CopperGolem golem, UUID id, boolean frozen) {
        if (frozen) {
            if (!golem.isGlowing()) {
                golem.setGlowing(true);
            }
            frozenGlowing.add(id);
        } else if (frozenGlowing.remove(id)) {
            golem.setGlowing(false); // only clear glow WE set for freezing (leaves whistle glow alone)
        }
    }

    private void greetOwnerIfNear(CopperGolem golem, PluginConfig c) {
        GolemState state = store.read(golem);
        if (state.owner() == null) {
            return;
        }
        Player owner = Bukkit.getPlayer(state.owner());
        if (owner == null || !owner.getWorld().equals(golem.getWorld())
                || owner.getLocation().distanceSquared(golem.getLocation()) > 36.0) {
            return;
        }
        if (c.bringFlower && !state.carrying() && roll(0.05)) {
            golem.lookAt(owner);
            fx.bringsFlower(golem); // a little gift gesture for its owner (cosmetic — no real item)
        } else if (roll(0.15)) {
            fx.greetOwner(golem);
        }
    }

    private static Player nearestPlayer(CopperGolem golem, double radius) {
        Location gl = golem.getLocation();
        double bestSq = radius * radius;
        Player best = null;
        for (Player p : golem.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(gl);
            if (d <= bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    private static boolean tarnished(WeatheringCopperState weather) {
        return weather == WeatheringCopperState.EXPOSED || weather == WeatheringCopperState.WEATHERED;
    }

    private static boolean roll(double chance) {
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static boolean playerNear(CopperGolem golem, double radius) {
        Location gl = golem.getLocation();
        double r2 = radius * radius;
        for (Player p : golem.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(gl) <= r2) {
                return true;
            }
        }
        return false;
    }
}
