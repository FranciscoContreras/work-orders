package com.wearemachina.workorders.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * The whitelist/blacklist match is a single boolean identity ({@code (mode==WHITELIST)==contains}) that's
 * easy to invert by accident; these lock the four truth-table corners plus the "empty matches all" rule.
 * Only the {@link Material} enum is referenced (no server is started).
 */
class ItemFilterTest {

    @Test
    void emptyFilterMatchesEverything() {
        ItemFilter f = ItemFilter.empty();
        assertTrue(f.isEmpty());
        assertTrue(f.matches(Material.STONE));
        assertTrue(f.matches(Material.DIAMOND));
    }

    @Test
    void whitelistAcceptsOnlyListedMaterials() {
        ItemFilter f = new ItemFilter(EnumSet.of(Material.WHEAT, Material.CARROT), ItemFilter.Mode.WHITELIST);
        assertTrue(f.matches(Material.WHEAT));
        assertTrue(f.matches(Material.CARROT));
        assertFalse(f.matches(Material.STONE));
    }

    @Test
    void blacklistRejectsOnlyListedMaterials() {
        ItemFilter f = new ItemFilter(EnumSet.of(Material.COBBLESTONE), ItemFilter.Mode.BLACKLIST);
        assertFalse(f.matches(Material.COBBLESTONE));
        assertTrue(f.matches(Material.DIAMOND));
    }

    @Test
    void emptyMaterialSetMatchesAllRegardlessOfMode() {
        ItemFilter blacklistOfNothing = new ItemFilter(EnumSet.noneOf(Material.class), ItemFilter.Mode.BLACKLIST);
        assertTrue(blacklistOfNothing.matches(Material.STONE), "an empty set short-circuits to match-all");
    }

    @Test
    void nullMaterialNeverMatches() {
        assertFalse(ItemFilter.empty().matches((Material) null));
        assertFalse(new ItemFilter(EnumSet.of(Material.STONE), ItemFilter.Mode.WHITELIST).matches((Material) null));
    }

    @Test
    void withModeFlipsAcceptanceButKeepsMaterials() {
        ItemFilter whitelist = new ItemFilter(EnumSet.of(Material.IRON_INGOT), ItemFilter.Mode.WHITELIST);
        ItemFilter blacklist = whitelist.withMode(ItemFilter.Mode.BLACKLIST);
        assertTrue(whitelist.matches(Material.IRON_INGOT));
        assertFalse(blacklist.matches(Material.IRON_INGOT));
        assertTrue(blacklist.matches(Material.GOLD_INGOT));
    }
}
