package com.wearemachina.workorders.haul;

import java.util.function.Predicate;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Low-level, dupe-safe item movement out of a source inventory. The defining safety property: a pull
 * <i>removes from the source and returns the carried stack in the same synchronous call</i> — there is
 * never a moment where the items exist in neither place.
 */
public final class Hauling {

    private Hauling() {
    }

    /**
     * Remove up to {@code max} items matching {@code accept} from the first matching slot of {@code inv}.
     *
     * @return the carried stack, or {@code null} if nothing matched.
     */
    public static ItemStack pull(Inventory inv, Predicate<ItemStack> accept, int max) {
        if (inv == null || max <= 0) {
            return null;
        }
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it == null || it.getType().isAir() || !accept.test(it)) {
                continue;
            }
            int take = Math.min(it.getAmount(), max);
            ItemStack carried = it.clone();
            carried.setAmount(take);
            if (it.getAmount() == take) {
                inv.setItem(i, null);
            } else {
                ItemStack remaining = it.clone();
                remaining.setAmount(it.getAmount() - take);
                inv.setItem(i, remaining);
            }
            return carried;
        }
        return null;
    }

    /** Total count of items in {@code inv} matching {@code accept}. */
    public static int count(Inventory inv, Predicate<ItemStack> accept) {
        if (inv == null) {
            return 0;
        }
        int n = 0;
        for (ItemStack it : inv.getContents()) {
            if (it != null && !it.getType().isAir() && accept.test(it)) {
                n += it.getAmount();
            }
        }
        return n;
    }
}
