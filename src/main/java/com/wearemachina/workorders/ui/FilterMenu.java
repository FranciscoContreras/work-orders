package com.wearemachina.workorders.ui;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.Messages;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.persistence.GolemStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Opens and renders the 9-slot filter editor. Reads/writes the golem's {@link ItemFilter}. */
public final class FilterMenu {

    private final GolemStore store;
    private final ConfigHolder cfg;

    public FilterMenu(GolemStore store, ConfigHolder cfg) {
        this.store = store;
        this.cfg = cfg;
    }

    public void open(Player player, CopperGolem golem) {
        Messages m = cfg.get().messages;
        GolemState state = store.read(golem);
        ItemFilter filter = state.filter();

        FilterMenuHolder holder = new FilterMenuHolder(golem.getUniqueId(), filter.mode());
        Inventory inv = Bukkit.createInventory(holder, FilterMenuHolder.SLOTS, m.render("filter.title"));
        holder.setInventory(inv);

        int slot = FilterMenuHolder.FIRST_FILTER_SLOT;
        for (Material mat : filter.materials()) {
            if (slot > FilterMenuHolder.LAST_FILTER_SLOT) {
                break;
            }
            inv.setItem(slot++, named(mat, m.render("filter.remove-hint")));
        }
        inv.setItem(FilterMenuHolder.TOGGLE_SLOT, toggleItem(holder, m));
        inv.setItem(FilterMenuHolder.INFO_SLOT, named(Material.COPPER_INGOT, m.render("filter.done")));

        player.openInventory(inv);
    }

    static ItemStack toggleItem(FilterMenuHolder holder, Messages m) {
        boolean whitelist = holder.mode() == ItemFilter.Mode.WHITELIST;
        Material mat = whitelist ? Material.LIME_DYE : Material.RED_DYE;
        return named(mat, m.render(whitelist ? "filter.toggle-whitelist" : "filter.toggle-blacklist"));
    }

    private static ItemStack named(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
