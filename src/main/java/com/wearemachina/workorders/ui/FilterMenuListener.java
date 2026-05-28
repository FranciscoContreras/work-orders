package com.wearemachina.workorders.ui;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.Messages;
import com.wearemachina.workorders.interact.Feedback;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.persistence.GolemStore;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Guards the filter editor so it can never lose or move a real item. Every click/drag is cancelled;
 * filter contents are mutated programmatically as amount-1 display clones (clicking an item in your own
 * inventory stamps its <i>type</i>; clicking a filled filter slot removes it). On close we read only the
 * {@link Material} set — the clones are discarded, never dropped.
 */
public final class FilterMenuListener implements Listener {

    private final GolemStore store;
    private final ConfigHolder cfg;
    private final Feedback fx;

    public FilterMenuListener(GolemStore store, ConfigHolder cfg, Feedback fx) {
        this.store = store;
        this.cfg = cfg;
        this.fx = fx;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FilterMenuHolder holder)) {
            return;
        }
        event.setCancelled(true); // nothing ever moves; we mutate the menu ourselves

        Inventory top = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        Messages m = cfg.get().messages;

        if (clicked.equals(top)) {
            int slot = event.getSlot();
            if (slot == FilterMenuHolder.TOGGLE_SLOT) {
                holder.flipMode();
                top.setItem(FilterMenuHolder.TOGGLE_SLOT, FilterMenu.toggleItem(holder, m));
            } else if (slot == FilterMenuHolder.INFO_SLOT) {
                event.getWhoClicked().closeInventory();
            } else if (slot >= FilterMenuHolder.FIRST_FILTER_SLOT && slot <= FilterMenuHolder.LAST_FILTER_SLOT) {
                top.setItem(slot, null); // remove this type from the filter
            }
        } else {
            // Clicked in the player's own inventory: stamp that item's TYPE into the filter (no item moves).
            ItemStack current = event.getCurrentItem();
            if (current != null && !current.getType().isAir()) {
                addType(top, current.getType());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FilterMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FilterMenuHolder holder)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        EnumSet<Material> mats = EnumSet.noneOf(Material.class);
        for (int i = FilterMenuHolder.FIRST_FILTER_SLOT; i <= FilterMenuHolder.LAST_FILTER_SLOT; i++) {
            ItemStack it = top.getItem(i);
            if (it != null && !it.getType().isAir()) {
                mats.add(it.getType());
            }
        }
        Entity ent = Bukkit.getEntity(holder.golemId());
        if (ent instanceof CopperGolem golem && store.isManaged(golem)) {
            GolemState state = store.read(golem);
            state.filter(new ItemFilter(mats, holder.mode()));
            store.write(golem, state);
            if (event.getPlayer() instanceof Player player) {
                fx.actionBar(player, "filter.saved",
                        "count", String.valueOf(mats.size()),
                        "mode", holder.mode() == ItemFilter.Mode.WHITELIST ? "whitelist" : "blacklist");
            }
        }
        // The display clones are discarded with the inventory — nothing is dropped to the world.
    }

    private void addType(Inventory top, Material type) {
        for (int i = FilterMenuHolder.FIRST_FILTER_SLOT; i <= FilterMenuHolder.LAST_FILTER_SLOT; i++) {
            ItemStack it = top.getItem(i);
            if (it != null && it.getType() == type) {
                return; // already in the filter
            }
        }
        for (int i = FilterMenuHolder.FIRST_FILTER_SLOT; i <= FilterMenuHolder.LAST_FILTER_SLOT; i++) {
            ItemStack it = top.getItem(i);
            if (it == null || it.getType().isAir()) {
                top.setItem(i, new ItemStack(type));
                return;
            }
        }
        // Filter full (7 types): silently ignore further additions.
    }
}
