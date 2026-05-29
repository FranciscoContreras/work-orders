package com.wearemachina.workorders.interact;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.gate.JobGate;
import com.wearemachina.workorders.item.JobItems;
import com.wearemachina.workorders.model.BindTarget;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.persistence.GolemStore;
import com.wearemachina.workorders.runtime.GolemRegistry;
import com.wearemachina.workorders.runtime.StatusBoard;
import com.wearemachina.workorders.ui.FilterMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * The whole player-facing control scheme — no commands, one optional UI. Right-clicking a golem assigns
 * a job (by held item), inspects it, toggles follow/stay, or opens the filter; tapping containers binds
 * its route via a short two-tap session; sneak + right-click air whistles. Vanilla interactions (name
 * tag, lead, shears, axe scrape, honeycomb wax) are never intercepted.
 */
public final class InteractionListener implements Listener {

    private final Plugin plugin;
    private final GolemStore store;
    private final GolemRegistry registry;
    private final ConfigHolder cfg;
    private final Feedback fx;
    private final BindSessionManager sessions;
    private final JobGate gate;
    private final FilterMenu filterMenu;
    private final JobItems jobItems;
    private final StatusBoard board;

    public InteractionListener(Plugin plugin, GolemStore store, GolemRegistry registry, ConfigHolder cfg,
                               Feedback fx, BindSessionManager sessions, JobGate gate, FilterMenu filterMenu,
                               JobItems jobItems, StatusBoard board) {
        this.plugin = plugin;
        this.store = store;
        this.registry = registry;
        this.cfg = cfg;
        this.fx = fx;
        this.sessions = sessions;
        this.gate = gate;
        this.filterMenu = filterMenu;
        this.jobItems = jobItems;
        this.board = board;
    }

    // ----- right-click a copper golem -----------------------------------------------------------

    @EventHandler
    public void onInteractGolem(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // only the main hand; avoids the off-hand double-fire
        }
        if (!(event.getRightClicked() instanceof CopperGolem golem)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material type = hand.getType();

        // Never touch vanilla interactions — but if a name tag renames a managed golem, fold the new name
        // into its job nameplate ("<name> · <job>") a tick later, once vanilla has applied the rename.
        if (isVanillaInteraction(type)) {
            if (type == Material.NAME_TAG) {
                captureNameTag(golem, hand);
            }
            return;
        }

        PluginConfig c = cfg.get();
        GolemState state = store.read(golem);
        RoleType role = jobItems.roleOf(hand); // a Work Order (recognised by tag) takes priority
        if (role == null && !type.isAir()) {
            role = c.roleFor(type); // optional legacy plain-item mapping
        }

        if (role != null) {
            event.setCancelled(true);
            if (!Ownership.isTrusted(player, state, c)) {
                fx.actionBar(player, "ownership.not-yours");
                fx.reject(golem);
                return;
            }
            boolean filterable = role == RoleType.COURIER || role == RoleType.RESTOCKER;
            if (player.isSneaking() && filterable && state.role() == role) {
                filterMenu.open(player, golem);
            } else {
                assignRole(player, golem, state, role, hand, c);
            }
            return;
        }

        if (type == Material.PLAYER_HEAD) {
            event.setCancelled(true);
            handleTrust(player, golem, state, hand);
            return;
        }

        if (type.isAir()) {
            if (player.isSneaking()) {
                event.setCancelled(true);
                toggleFollow(player, golem, state, c);
            } else {
                BindSession active = sessions.get(player.getUniqueId());
                if (active != null && active.golemId().equals(golem.getUniqueId())) {
                    event.setCancelled(true);
                    sessions.clear(player.getUniqueId());
                    if (state.role() == RoleType.SORTER && !state.sortDests().isEmpty()) {
                        fx.happy(golem); // tap the golem to FINISH collecting sorter destinations
                        fx.actionBar(player, "bind.sort-done", "count", String.valueOf(state.sortDests().size()));
                    } else {
                        fx.actionBar(player, "bind.cancelled");
                    }
                } else {
                    inspect(player, golem, state); // no cancel — vanilla does nothing on empty-hand golem click
                }
            }
            return;
        }

        // Any other item: gentle inspect, no reject spam.
        inspect(player, golem, state);
    }

    private void assignRole(Player player, CopperGolem golem, GolemState state, RoleType role,
                            ItemStack hand, PluginConfig c) {
        if (!gate.canAssign(player, role)) {
            fx.actionBar(player, "assign.no-permission", "role", pretty(role));
            fx.reject(golem);
            return;
        }
        if (state.owner() == null) {
            state.owner(player.getUniqueId());
        }
        boolean firstAssign = !state.assigned();
        state.role(role);
        state.carrying(false);
        state.source(null);
        state.dest(null);
        state.clearSortDests();
        state.labelItem(JobItems.labelMaterial(role));
        // On the first assignment, adopt any name the player had already given it as the nameplate base.
        if (firstAssign && state.baseName() == null) {
            String existing = stripName(golem.getCustomName());
            if (existing != null && !existing.isBlank()) {
                state.baseName(existing);
            }
        }
        store.write(golem, state);
        registry.add(golem.getUniqueId());
        fx.applyNameplate(golem, role, state.baseName());

        // Cosmetic role tool in the hand (drop chance 0 — a copy, never dropped, never duped).
        EntityEquipment eq = golem.getEquipment();
        if (eq != null) {
            eq.setItemInMainHand(new ItemStack(JobItems.labelMaterial(role)));
            eq.setItemInMainHandDropChance(0f);
        }
        // A Work Order is spent on assignment; the legacy plain-item path honours consume-role-item.
        boolean workOrder = jobItems.roleOf(hand) != null;
        if ((workOrder || c.consumeRoleItem) && hand.getAmount() > 0) {
            hand.setAmount(hand.getAmount() - 1);
        }

        if (role != RoleType.FARMHAND) { // Farmhand needs no binding — it tends the area around itself
            BindSession.Stage first = (role == RoleType.JANITOR)
                    ? BindSession.Stage.AWAIT_DEST : BindSession.Stage.AWAIT_SOURCE;
            long expires = System.currentTimeMillis() + c.bindSessionTtlTicks * 50L;
            sessions.start(player.getUniqueId(), new BindSession(golem.getUniqueId(), first, expires));
        }

        fx.happy(golem);
        hop(golem);
        String successKey = switch (role) {
            case JANITOR -> "assign.success-janitor";
            case SORTER -> "assign.success-sorter";
            case FARMHAND -> "assign.success-farmhand";
            default -> "assign.success";
        };
        fx.actionBar(player, successKey, "role", pretty(role));
    }

    private void toggleFollow(Player player, CopperGolem golem, GolemState state, PluginConfig c) {
        if (!Ownership.isTrusted(player, state, c)) {
            fx.actionBar(player, "ownership.not-yours");
            fx.reject(golem);
            return;
        }
        state.follow(!state.follow());
        store.write(golem, state);
        fx.actionBar(player, state.follow() ? "follow.on" : "follow.off");
        fx.greetOwner(golem);
    }

    /** Owner holds a player's head and right-clicks the golem to trust (or un-trust) that player. */
    private void handleTrust(Player player, CopperGolem golem, GolemState state, ItemStack head) {
        if (!Ownership.isOwner(player, state)) {
            fx.actionBar(player, "trust.not-owner");
            fx.reject(golem);
            return;
        }
        OfflinePlayer target = headOwner(head);
        if (target == null || target.getUniqueId().equals(player.getUniqueId())) {
            fx.actionBar(player, "trust.head-needed");
            return;
        }
        boolean added = state.toggleTrusted(target.getUniqueId());
        store.write(golem, state);
        String name = (target.getName() != null) ? target.getName() : "that player";
        fx.actionBar(player, added ? "trust.added" : "trust.removed", "name", name);
        if (added) {
            fx.happy(golem);
        } else {
            fx.greetOwner(golem);
        }
    }

    private static OfflinePlayer headOwner(ItemStack head) {
        return (head.getItemMeta() instanceof SkullMeta skull) ? skull.getOwningPlayer() : null;
    }

    private void inspect(Player player, CopperGolem golem, GolemState state) {
        if (!state.assigned()) {
            fx.actionBar(player, "inspect.unassigned");
            return;
        }
        String stateKey = board.get(golem.getUniqueId());
        String stateText = cfg.get().messages.raw("state." + stateKey, stateKey);
        fx.actionBar(player, "inspect.status",
                "role", pretty(state.role()),
                "state", stateText,
                "oxidation", pretty(golem.getWeatheringState().name()));
    }

    /**
     * A renamed name tag was used on a managed golem. Vanilla applies the raw name this tick; one tick
     * later we fold it back into the job nameplate as "{@code <name> · <job>}". A blank name tag (no
     * custom name) does nothing in vanilla, so we ignore it and leave the existing nameplate alone.
     */
    private void captureNameTag(CopperGolem golem, ItemStack nameTag) {
        if (!cfg.get().nameplate || !store.isManaged(golem)) {
            return;
        }
        ItemMeta meta = nameTag.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        String newBase = stripName(meta.getDisplayName());
        if (newBase == null || newBase.isBlank()) {
            return;
        }
        UUID id = golem.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!(Bukkit.getEntity(id) instanceof CopperGolem g) || !store.isManaged(g)) {
                return;
            }
            GolemState st = store.read(g);
            if (st.role() == null) {
                return;
            }
            st.baseName(newBase);
            store.write(g, st);
            fx.applyNameplate(g, st.role(), newBase);
        }, 1L);
    }

    // ----- tap a container (binding) / whistle --------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Action action = event.getAction();
        PluginConfig c = cfg.get();

        // Whistle: sneak + right-click air holding the whistle item.
        if (action == Action.RIGHT_CLICK_AIR && c.whistleEnabled && player.isSneaking()
                && event.getMaterial() == c.whistleItem) {
            whistle(player, c);
            return;
        }

        // Container tap during an active bind session.
        if (action == Action.RIGHT_CLICK_BLOCK) {
            BindSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                return;
            }
            Block block = event.getClickedBlock();
            if (block == null) {
                return;
            }
            if (!(block.getState() instanceof Container)) {
                fx.actionBar(player, "bind.not-a-container"); // try a real container; don't cancel vanilla use
                return;
            }
            event.setCancelled(true); // consume the tap as a bind
            handleBind(player, session, block, c);
        }
    }

    private void handleBind(Player player, BindSession session, Block block, PluginConfig c) {
        Entity ent = Bukkit.getEntity(session.golemId());
        if (!(ent instanceof CopperGolem golem) || !store.isManaged(golem)) {
            sessions.clear(player.getUniqueId());
            return;
        }
        GolemState state = store.read(golem);
        if (!Ownership.isTrusted(player, state, c)) {
            fx.actionBar(player, "ownership.not-yours");
            sessions.clear(player.getUniqueId());
            return;
        }
        Location gl = golem.getLocation();
        Location bl = block.getLocation();
        if (gl.getWorld() == null || bl.getWorld() == null || !gl.getWorld().equals(bl.getWorld())) {
            fx.actionBar(player, "bind.cross-world");
            return;
        }
        if (bl.distance(gl) > c.rangeCap) {
            fx.actionBar(player, "bind.too-far", "max", String.valueOf((int) c.rangeCap));
            fx.reject(golem);
            return;
        }
        BindTarget target = BindTarget.of(bl);

        if (session.stage() == BindSession.Stage.AWAIT_SOURCE) {
            state.source(target);
            store.write(golem, state);
            session.stage(BindSession.Stage.AWAIT_DEST);
            fx.tapBlock(bl);
            fx.actionBar(player, "bind.source-set", "target", pretty(block.getType().name()));
        } else if (state.role() == RoleType.SORTER) {
            state.addSortDest(target);
            store.write(golem, state);
            fx.tapBlock(bl);
            fx.actionBar(player, "bind.sort-added",
                    "dest", pretty(block.getType().name()),
                    "count", String.valueOf(state.sortDests().size()));
            // session stays open — keep tapping destination chests, then tap the golem to finish
        } else {
            state.dest(target);
            store.write(golem, state);
            sessions.clear(player.getUniqueId());
            fx.routeConfirmed(golem, gl, bl.clone().add(0.5, 0.5, 0.5));
            String sourceName = describeTarget(state.source(), "items");
            fx.actionBar(player, "bind.dest-set", "source", sourceName, "dest", pretty(block.getType().name()));
        }
    }

    private void whistle(Player player, PluginConfig c) {
        UUID me = player.getUniqueId();
        int radius = (int) Math.ceil(c.whistleRadius);
        List<CopperGolem> mine = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (!(e instanceof CopperGolem golem) || !store.isManaged(golem)) {
                continue;
            }
            GolemState st = store.read(golem);
            if (st.owner() != null && !st.owner().equals(me)) {
                continue;
            }
            mine.add(golem);
        }
        if (mine.isEmpty()) {
            fx.actionBar(player, "whistle.none", "count", "0");
            return;
        }
        // Toggle the group: if any are already heeling, dismiss them all; otherwise call them all in.
        boolean recall = mine.stream().noneMatch(g -> store.read(g).follow());
        for (CopperGolem golem : mine) {
            GolemState st = store.read(golem);
            st.follow(recall);
            store.write(golem, st);
            if (recall) {
                golem.setGlowing(true);
                fx.whistlePing(golem);
                UUID id = golem.getUniqueId();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (Bukkit.getEntity(id) instanceof CopperGolem g) {
                        g.setGlowing(false);
                    }
                }, c.whistleGlowSeconds * 20L);
            }
        }
        fx.actionBar(player, recall ? "whistle.recalled" : "whistle.dismissed",
                "count", String.valueOf(mine.size()));
    }

    // ----- helpers ------------------------------------------------------------------------------

    private static void hop(CopperGolem golem) {
        golem.setVelocity(golem.getVelocity().add(new Vector(0.0, 0.32, 0.0)));
    }

    private static boolean isVanillaInteraction(Material type) {
        return type == Material.NAME_TAG
                || type == Material.LEAD
                || type == Material.SHEARS
                || type == Material.HONEYCOMB
                || type.name().endsWith("_AXE");
    }

    private String describeTarget(BindTarget t, String fallback) {
        if (t == null) {
            return fallback;
        }
        Location loc = t.toLocation();
        if (loc == null) {
            return fallback;
        }
        return pretty(loc.getBlock().getType().name());
    }

    /** Strip legacy colour codes from a name so it renders cleanly as plain text in the nameplate. */
    private static String stripName(String legacy) {
        return legacy == null ? null : ChatColor.stripColor(legacy);
    }

    private static String pretty(RoleType role) {
        return pretty(role.name());
    }

    /** Turn an ENUM_NAME into "Enum Name". */
    private static String pretty(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
