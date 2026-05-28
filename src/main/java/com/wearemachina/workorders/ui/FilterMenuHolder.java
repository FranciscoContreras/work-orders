package com.wearemachina.workorders.ui;

import com.wearemachina.workorders.model.ItemFilter;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as a Work Orders filter editor and carries the target golem's id plus the current
 * (live-toggled) filter mode. The inventory only ever holds amount-1 display clones, never real items.
 */
public final class FilterMenuHolder implements InventoryHolder {

    public static final int SLOTS = 9;
    public static final int FIRST_FILTER_SLOT = 0;
    public static final int LAST_FILTER_SLOT = 6;
    public static final int TOGGLE_SLOT = 7;
    public static final int INFO_SLOT = 8;

    private final UUID golemId;
    private ItemFilter.Mode mode;
    private Inventory inventory;

    public FilterMenuHolder(UUID golemId, ItemFilter.Mode mode) {
        this.golemId = golemId;
        this.mode = (mode == null) ? ItemFilter.Mode.WHITELIST : mode;
    }

    public UUID golemId() {
        return golemId;
    }

    public ItemFilter.Mode mode() {
        return mode;
    }

    public void flipMode() {
        this.mode = (mode == ItemFilter.Mode.WHITELIST) ? ItemFilter.Mode.BLACKLIST : ItemFilter.Mode.WHITELIST;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
