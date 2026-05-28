package com.wearemachina.workorders.haul;

import org.bukkit.Material;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Slot-aware, non-voiding deposit. Every method returns the <b>leftover</b> (what didn't fit) so the
 * caller keeps it in the golem's hand — items are never overwritten or destroyed. {@code Inventory#addItem}
 * is the backbone for generic containers (it inherently returns leftovers); furnaces/brewing stands get
 * explicit per-slot routing so fuel/ingredients never land in the wrong slot.
 */
public final class DepositPolicy {

    /** Route {@code cargo} into the resolved destination by kind. @return leftover, or {@code null} if all fit. */
    public ItemStack deposit(ContainerAccess.Resolution dst, ItemStack cargo) {
        if (dst == null || !dst.isOk() || cargo == null || cargo.getType().isAir()) {
            return cargo;
        }
        Inventory inv = dst.container.getInventory();
        return switch (dst.kind) {
            case GENERIC -> depositGeneric(inv, cargo);
            case FURNACE -> depositFurnace((FurnaceInventory) inv, cargo);
            case BREWING -> depositBrewing((BrewerInventory) inv, cargo);
        };
    }

    public ItemStack depositGeneric(Inventory inv, ItemStack cargo) {
        var leftover = inv.addItem(cargo.clone()); // addItem never voids; returns what didn't fit
        return leftover.isEmpty() ? null : leftover.values().iterator().next();
    }

    private ItemStack depositFurnace(FurnaceInventory inv, ItemStack cargo) {
        if (inv.isFuel(cargo)) {
            ItemStack[] r = merge(inv.getFuel(), cargo);
            inv.setFuel(r[0]);
            return r[1];
        }
        if (inv.canSmelt(cargo)) {
            ItemStack[] r = merge(inv.getSmelting(), cargo);
            inv.setSmelting(r[0]);
            return r[1];
        }
        return cargo; // neither fuel nor smeltable here — return to sender, never force into a slot
    }

    private ItemStack depositBrewing(BrewerInventory inv, ItemStack cargo) {
        if (cargo.getType() == Material.BLAZE_POWDER) {
            ItemStack[] r = merge(inv.getFuel(), cargo);
            inv.setFuel(r[0]);
            return r[1];
        }
        ItemStack[] r = merge(inv.getIngredient(), cargo);
        inv.setIngredient(r[0]);
        return r[1];
    }

    /**
     * Merge {@code incoming} into a single target slot holding {@code slot}, respecting type-match and
     * max stack size.
     *
     * @return [newSlotContents, leftover] — leftover is {@code null} if everything fit.
     */
    private static ItemStack[] merge(ItemStack slot, ItemStack incoming) {
        int max = incoming.getMaxStackSize();
        if (slot == null || slot.getType().isAir()) {
            int put = Math.min(incoming.getAmount(), max);
            ItemStack newSlot = incoming.clone();
            newSlot.setAmount(put);
            return new ItemStack[]{newSlot, leftoverOf(incoming, incoming.getAmount() - put)};
        }
        if (!slot.isSimilar(incoming)) {
            return new ItemStack[]{slot, incoming}; // different item — don't overwrite; all leftover
        }
        int space = max - slot.getAmount();
        int put = Math.max(0, Math.min(space, incoming.getAmount()));
        ItemStack newSlot = slot.clone();
        newSlot.setAmount(slot.getAmount() + put);
        return new ItemStack[]{newSlot, leftoverOf(incoming, incoming.getAmount() - put)};
    }

    private static ItemStack leftoverOf(ItemStack incoming, int amount) {
        if (amount <= 0) {
            return null;
        }
        ItemStack left = incoming.clone();
        left.setAmount(amount);
        return left;
    }
}
