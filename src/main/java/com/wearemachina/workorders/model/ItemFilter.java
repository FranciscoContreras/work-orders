package com.wearemachina.workorders.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * An immutable item filter: a set of materials plus a whitelist/blacklist mode.
 * An empty material set matches everything (no restriction). Pure logic — unit-testable.
 */
public final class ItemFilter {

    public enum Mode { WHITELIST, BLACKLIST }

    private static final ItemFilter EMPTY = new ItemFilter(EnumSet.noneOf(Material.class), Mode.WHITELIST);

    private final Set<Material> materials;
    private final Mode mode;

    public ItemFilter(Set<Material> materials, Mode mode) {
        EnumSet<Material> copy = EnumSet.noneOf(Material.class);
        if (materials != null) {
            copy.addAll(materials);
        }
        this.materials = Collections.unmodifiableSet(copy);
        this.mode = (mode == null) ? Mode.WHITELIST : mode;
    }

    public static ItemFilter empty() {
        return EMPTY;
    }

    public Set<Material> materials() {
        return materials;
    }

    public Mode mode() {
        return mode;
    }

    public boolean isEmpty() {
        return materials.isEmpty();
    }

    /** @return true if an item of this material should be accepted. An empty filter accepts all. */
    public boolean matches(Material material) {
        if (material == null) {
            return false;
        }
        if (materials.isEmpty()) {
            return true;
        }
        boolean contains = materials.contains(material);
        return (mode == Mode.WHITELIST) == contains;
    }

    public boolean matches(ItemStack stack) {
        return stack != null && matches(stack.getType());
    }

    public ItemFilter withMode(Mode newMode) {
        return new ItemFilter(materials, newMode);
    }
}
