package com.wearemachina.workorders.item;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.Messages;
import com.wearemachina.workorders.config.WorkOrderRecipe;
import com.wearemachina.workorders.model.RoleType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Defines the craftable "Work Order" items that assign jobs, registers their recipes, and recognises them
 * by a hidden item-PDC tag (so a Work Order — not a plain vanilla item — is what assigns a job, avoiding
 * any dual-use clash). The Work Order is consumed on assignment; the golem then wears a distinct cosmetic
 * tool per job so you can read its role at a glance.
 */
public final class JobItems implements Listener {

    private final ConfigHolder cfg;
    private final NamespacedKey roleTag;
    private final Map<RoleType, NamespacedKey> recipeKeys = new EnumMap<>(RoleType.class);

    public JobItems(Plugin plugin, ConfigHolder cfg, NamespacedKey roleTag) {
        this.cfg = cfg;
        this.roleTag = roleTag;
        for (RoleType role : RoleType.values()) {
            recipeKeys.put(role, new NamespacedKey(plugin, "work_order_" + role.key()));
        }
    }

    /** (Re)register a shaped crafting recipe for every job's Work Order, from the configured pattern. */
    public void registerRecipes() {
        for (RoleType role : RoleType.values()) {
            NamespacedKey key = recipeKeys.get(role);
            Bukkit.removeRecipe(key); // idempotent across reloads
            WorkOrderRecipe spec = cfg.get().workOrderRecipe(role);
            ShapedRecipe recipe = new ShapedRecipe(key, buildWorkOrder(role));
            recipe.shape(spec.shape().toArray(new String[0]));
            for (Map.Entry<Character, Material> ingredient : spec.ingredients().entrySet()) {
                recipe.setIngredient(ingredient.getKey(), ingredient.getValue());
            }
            Bukkit.addRecipe(recipe);
        }
    }

    /** Build a fresh Work Order item for a job (named paper carrying the role tag + optional model data). */
    public ItemStack buildWorkOrder(RoleType role) {
        Messages m = cfg.get().messages;
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(m.render("workorder." + role.key() + ".name").decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                m.render("workorder." + role.key() + ".lore").decoration(TextDecoration.ITALIC, false),
                m.render("brand.signature").decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(roleTag, PersistentDataType.STRING, role.name());
        int model = cfg.get().workOrderModel(role);
        if (model > 0) {
            meta.setCustomModelData(model);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** @return the role a Work Order assigns, or {@code null} if the item isn't a Work Order. */
    public RoleType roleOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        String name = meta.getPersistentDataContainer().get(roleTag, PersistentDataType.STRING);
        if (name == null) {
            return null;
        }
        try {
            return RoleType.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The distinct cosmetic tool a golem holds to advertise its job. */
    public static Material labelMaterial(RoleType role) {
        return switch (role) {
            case COURIER -> Material.HOPPER;
            case STOKER -> Material.FURNACE;
            case RESTOCKER -> Material.BARREL;
            case JANITOR -> Material.BRUSH;
            case SORTER -> Material.COMPARATOR;
            case FARMHAND -> Material.IRON_HOE;
        };
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(recipeKeys.values()); // surface them in the recipe book
    }
}
