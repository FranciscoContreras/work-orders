package com.wearemachina.workorders;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.MobGoals;
import com.wearemachina.workorders.config.Messages;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * The single (admin-only) command: reload config, dump status, and run the native-AI spike that reports
 * a nearby golem's registered goals — the ground-truth probe for whether vanilla sorting is a removable
 * {@link Goal} (and thus whether suppression is even possible).
 */
public final class WorkOrdersCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "status", "spike");

    private final WorkOrdersPlugin plugin;

    public WorkOrdersCommand(WorkOrdersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("workorders.admin")) {
            sender.sendMessage(plugin.configHolder().get().messages.prefixed("admin.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.configHolder().get().messages.prefixed("admin.usage"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.configHolder().get().messages.prefixed("admin.reloaded"));
            }
            case "status" -> sendStatus(sender);
            case "spike" -> runSpike(sender);
            default -> sender.sendMessage(plugin.configHolder().get().messages.prefixed("admin.usage"));
        }
        return true;
    }

    private void sendStatus(CommandSender sender) {
        var c = plugin.configHolder().get();
        Messages m = c.messages;
        sender.sendMessage(m.render("admin.status-header"));
        sender.sendMessage(m.render("admin.status-tracked", "count", String.valueOf(plugin.registry().size())));
        sender.sendMessage(m.render("admin.status-timing",
                "interval", String.valueOf(c.serviceIntervalTicks), "slices", String.valueOf(c.slices)));
        sender.sendMessage(m.render("admin.status-radius",
                "active", String.valueOf(c.activeRadius), "range", String.valueOf(c.rangeCap)));
        sender.sendMessage(m.render("admin.status-suppress", "mode", c.suppressNativeSort));
    }

    private void runSpike(CommandSender sender) {
        Messages m = plugin.configHolder().get().messages;
        CopperGolem nearest = findGolem(sender);
        if (nearest == null) {
            sender.sendMessage(m.render("admin.spike-none"));
            return;
        }
        MobGoals mobGoals = Bukkit.getMobGoals();
        Collection<Goal<CopperGolem>> goals = mobGoals.getAllGoals(nearest);
        sender.sendMessage(m.render("admin.spike-header", "count", String.valueOf(goals.size())));
        plugin.getLogger().info("[spike] copper golem " + nearest.getUniqueId() + " has " + goals.size() + " goals:");
        for (Goal<CopperGolem> goal : goals) {
            String key = goal.getKey().getNamespacedKey().toString();
            sender.sendMessage(m.render("admin.spike-line", "key", key));
            plugin.getLogger().info("[spike]   " + key + " types=" + goal.getTypes());
        }
        sender.sendMessage(m.render("admin.spike-footer"));
    }

    /** Player → nearest golem within 32 blocks; console → the first loaded copper golem in any world. */
    private CopperGolem findGolem(CommandSender sender) {
        if (sender instanceof Player player) {
            CopperGolem nearest = null;
            double bestSq = Double.MAX_VALUE;
            for (Entity e : player.getNearbyEntities(32, 32, 32)) {
                if (e instanceof CopperGolem golem) {
                    double d = e.getLocation().distanceSquared(player.getLocation());
                    if (d < bestSq) {
                        bestSq = d;
                        nearest = golem;
                    }
                }
            }
            return nearest;
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity e : world.getEntities()) {
                if (e instanceof CopperGolem golem) {
                    return golem;
                }
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
            return out;
        }
        return List.of();
    }
}
