package com.wearemachina.workorders.config;

import com.wearemachina.workorders.model.RoleType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Maps a held {@link Material} to the {@link RoleType} it assigns. Built by inverting the config's
 * {@code roles: { courier: [...], ... }} section. An item mapped to two roles is a config error
 * (first wins, warning logged). Pure lookup at runtime.
 */
public final class RoleItemMap {

    private final Map<Material, RoleType> itemToRole;

    private RoleItemMap(Map<Material, RoleType> itemToRole) {
        this.itemToRole = itemToRole;
    }

    public RoleType roleFor(Material material) {
        return material == null ? null : itemToRole.get(material);
    }

    public boolean isRoleItem(Material material) {
        return material != null && itemToRole.containsKey(material);
    }

    public static RoleItemMap from(ConfigurationSection roles, Logger log) {
        Map<Material, RoleType> map = new EnumMap<>(Material.class);
        if (roles != null) {
            for (RoleType role : RoleType.values()) {
                List<String> items = roles.getStringList(role.key());
                for (String name : items) {
                    Material mat = Material.matchMaterial(name);
                    if (mat == null) {
                        log.warning("roles." + role.key() + ": unknown item '" + name + "' (ignored)");
                        continue;
                    }
                    RoleType prev = map.putIfAbsent(mat, role);
                    if (prev != null && prev != role) {
                        log.warning("item " + mat + " is mapped to both " + prev + " and " + role
                                + "; keeping " + prev);
                    }
                }
            }
        }
        return new RoleItemMap(map);
    }
}
