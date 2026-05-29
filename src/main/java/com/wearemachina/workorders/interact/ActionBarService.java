package com.wearemachina.workorders.interact;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Keeps a Work Orders action-bar line on screen for a few seconds by re-sending it every tick, so it wins
 * the action bar against other sources (e.g. a mob-health display) that also write there. The newest line
 * replaces any earlier one for that player and resets its timer; when nothing is held the re-send task
 * stops itself, so there is zero idle overhead. Main-thread confined (Bukkit scheduler + event handlers).
 */
public final class ActionBarService {

    private record Held(Component line, int remaining) {
    }

    private final Plugin plugin;
    private final Map<UUID, Held> held = new HashMap<>();
    private BukkitTask task;

    public ActionBarService(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Show {@code line} now and keep re-sending it for {@code durationTicks} so it stays readable instead of
     * being overwritten the next tick. A duration of 1 or less is a plain one-shot send.
     */
    public void hold(Player player, Component line, int durationTicks) {
        if (player == null || line == null) {
            return;
        }
        player.sendActionBar(line); // immediate — no one-tick gap before the loop picks it up
        if (durationTicks <= 1) {
            held.remove(player.getUniqueId()); // a fresh one-shot supersedes any earlier hold
            return;
        }
        held.put(player.getUniqueId(), new Held(line, durationTicks));
        ensureRunning();
    }

    private void ensureRunning() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::pump, 1L, 1L);
        }
    }

    private void pump() {
        Iterator<Map.Entry<UUID, Held>> it = held.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Held> entry = it.next();
            Held h = entry.getValue();
            int left = h.remaining() - 1;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline() || left <= 0) {
                it.remove();
                continue;
            }
            p.sendActionBar(h.line());
            entry.setValue(new Held(h.line(), left));
        }
        if (held.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Cancel the loop and forget all holds (called on plugin disable). */
    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        held.clear();
    }
}
