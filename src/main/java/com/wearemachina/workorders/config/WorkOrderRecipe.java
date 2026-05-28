package com.wearemachina.workorders.config;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/**
 * A configured shaped crafting recipe for a job's Work Order: up to 3 pattern rows (space = empty cell)
 * and a symbol&rarr;material map, plus an optional custom_model_data.
 */
public record WorkOrderRecipe(List<String> shape, Map<Character, Material> ingredients, int model) {
}
