package com.wearemachina.workorders.config;

import com.wearemachina.workorders.model.RoleType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Immutable snapshot of {@code config.yml} (plus the loaded {@link Messages} and {@link RoleItemMap}).
 * Rebuilt wholesale on reload and swapped atomically via {@link ConfigHolder}, so services always read a
 * consistent set of values.
 */
public final class PluginConfig {

    // settings
    public final int serviceIntervalTicks;
    public final int slices;
    public final double activeRadius;
    public final double rangeCap;
    public final double moveSpeed;
    public final int pullBatch;
    public final int pathFailGrace;
    public final int bindSessionTtlTicks;

    // ownership
    public final boolean trustedOnlyRetask;
    public final boolean waxAnyone;
    public final boolean whistleEnabled;
    public final Material whistleItem;
    public final double whistleRadius;
    public final int whistleGlowSeconds;
    public final boolean consumeRoleItem;

    // native AI
    public final String suppressNativeSort; // "auto" | "off"

    // gating
    public final boolean gatingRequirePermission;

    // restocker
    public final int restockerDefaultThreshold;

    // janitor
    public final int janitorRadius;

    // farmhand
    public final int farmhandRadius;

    // oxidation
    public final boolean efficiencyScalesWithOxidation;

    // feedback
    public final FeedbackIntensity intensity;
    public final boolean sounds;
    public final boolean actionbar;
    public final boolean frozenGlow;
    public final boolean sluggishFx;
    public final boolean bringFlower;

    public final boolean debug;

    public final RoleItemMap roles;
    public final Messages messages;

    private final Map<RoleType, WorkOrderRecipe> workOrderRecipes;

    private PluginConfig(FileConfiguration c, RoleItemMap roles, Messages messages,
                         Map<RoleType, WorkOrderRecipe> workOrderRecipes) {
        this.serviceIntervalTicks = Math.max(1, c.getInt("settings.service-interval-ticks", 10));
        this.slices = Math.max(1, c.getInt("settings.slices", 4));
        this.activeRadius = c.getDouble("settings.active-radius", 64.0);
        this.rangeCap = Math.min(64.0, Math.max(1.0, c.getDouble("settings.range-cap", 48.0)));
        this.moveSpeed = c.getDouble("settings.move-speed", 1.0);
        this.pullBatch = Math.max(1, Math.min(64, c.getInt("settings.pull-batch", 16)));
        this.pathFailGrace = Math.max(1, c.getInt("settings.path-fail-grace", 3));
        this.bindSessionTtlTicks = Math.max(20, c.getInt("settings.bind-session-ttl-ticks", 600));

        this.trustedOnlyRetask = c.getBoolean("ownership.trusted-only-retask", true);
        this.waxAnyone = c.getBoolean("ownership.wax-anyone", true);
        this.whistleEnabled = c.getBoolean("ownership.whistle-enabled", true);
        this.whistleItem = matchOr(c.getString("ownership.whistle-item", "COPPER_INGOT"), Material.COPPER_INGOT);
        this.whistleRadius = c.getDouble("ownership.whistle-radius", 48.0);
        this.whistleGlowSeconds = Math.max(1, c.getInt("ownership.whistle-glow-seconds", 4));
        this.consumeRoleItem = c.getBoolean("ownership.consume-role-item", false);

        this.suppressNativeSort = c.getString("native-ai.suppress-native-sort", "auto");

        this.gatingRequirePermission = c.getBoolean("gating.require-permission", true);

        this.restockerDefaultThreshold = Math.max(1, c.getInt("restocker.default-threshold", 64));

        this.janitorRadius = Math.min(16, Math.max(1, c.getInt("janitor.radius", 6)));
        this.farmhandRadius = Math.min(8, Math.max(1, c.getInt("farmhand.radius", 4)));

        this.efficiencyScalesWithOxidation = c.getBoolean("oxidation.efficiency-scales-with-oxidation", false);

        this.intensity = FeedbackIntensity.from(c.getString("feedback.intensity", "normal"));
        this.sounds = c.getBoolean("feedback.sounds", true);
        this.actionbar = c.getBoolean("feedback.actionbar", true);
        this.frozenGlow = c.getBoolean("feedback.frozen-glow", true);
        this.sluggishFx = c.getBoolean("feedback.sluggish-fx", true);
        this.bringFlower = c.getBoolean("feedback.bring-flower", true);

        this.debug = c.getBoolean("debug", false);

        this.roles = roles;
        this.messages = messages;
        this.workOrderRecipes = workOrderRecipes;
    }

    public static PluginConfig load(JavaPlugin plugin) {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        Logger log = plugin.getLogger();
        RoleItemMap roles = RoleItemMap.from(c.getConfigurationSection("roles"), log);
        Messages messages = Messages.load(plugin);

        Map<RoleType, WorkOrderRecipe> recipes = new EnumMap<>(RoleType.class);
        ConfigurationSection wo = c.getConfigurationSection("work-orders");
        for (RoleType role : RoleType.values()) {
            recipes.put(role, parseRecipe(wo, role, log));
        }
        return new PluginConfig(c, roles, messages, recipes);
    }

    public RoleType roleFor(Material material) {
        return roles.roleFor(material);
    }

    /** The configured shaped recipe for a job's Work Order. */
    public WorkOrderRecipe workOrderRecipe(RoleType role) {
        return workOrderRecipes.getOrDefault(role, defaultRecipe(role, 0));
    }

    /** Optional custom_model_data for a job's Work Order (0 = none). */
    public int workOrderModel(RoleType role) {
        return workOrderRecipe(role).model();
    }

    private static WorkOrderRecipe parseRecipe(ConfigurationSection wo, RoleType role, Logger log) {
        ConfigurationSection job = (wo == null) ? null : wo.getConfigurationSection(role.key());
        int model = (job == null) ? 0 : Math.max(0, job.getInt("model", 0));
        if (job == null) {
            return defaultRecipe(role, model);
        }
        List<String> shape = job.getStringList("shape");
        ConfigurationSection ingredients = job.getConfigurationSection("ingredients");
        if (shape.isEmpty() || ingredients == null) {
            return defaultRecipe(role, model);
        }
        if (shape.size() > 3) {
            log.warning("work-orders." + role.key() + ".shape has more than 3 rows; using default.");
            return defaultRecipe(role, model);
        }
        int width = shape.get(0).length();
        if (width < 1 || width > 3) {
            log.warning("work-orders." + role.key() + ".shape rows must be 1-3 chars; using default.");
            return defaultRecipe(role, model);
        }
        for (String row : shape) {
            if (row.length() != width) {
                log.warning("work-orders." + role.key() + ".shape rows must all be the same length; using default.");
                return defaultRecipe(role, model);
            }
        }
        Map<Character, Material> map = new HashMap<>();
        for (String symbol : ingredients.getKeys(false)) {
            if (symbol.length() != 1) {
                continue;
            }
            Material m = Material.matchMaterial(String.valueOf(ingredients.getString(symbol)));
            if (m != null && m.isItem()) {
                map.put(symbol.charAt(0), m);
            }
        }
        for (String row : shape) {
            for (char ch : row.toCharArray()) {
                if (ch != ' ' && !map.containsKey(ch)) {
                    log.warning("work-orders." + role.key() + ": shape symbol '" + ch
                            + "' has no ingredient mapped; using default.");
                    return defaultRecipe(role, model);
                }
            }
        }
        return new WorkOrderRecipe(List.copyOf(shape), map, model);
    }

    private static WorkOrderRecipe defaultRecipe(RoleType role, int model) {
        return new WorkOrderRecipe(
                List.of(" C ", "CPC", "CTC"),
                Map.of('C', Material.COPPER_INGOT, 'P', Material.PAPER, 'T', centerpiece(role)),
                model);
    }

    private static Material centerpiece(RoleType role) {
        return switch (role) {
            case COURIER -> Material.HOPPER;
            case STOKER -> Material.FURNACE;
            case RESTOCKER -> Material.BARREL;
            case JANITOR -> Material.BRUSH;
            case SORTER -> Material.COMPARATOR;
            case FARMHAND -> Material.IRON_HOE;
        };
    }

    private static Material matchOr(String name, Material fallback) {
        Material m = (name == null) ? null : Material.matchMaterial(name);
        return m == null ? fallback : m;
    }
}
