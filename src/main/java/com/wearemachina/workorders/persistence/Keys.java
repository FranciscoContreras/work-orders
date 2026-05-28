package com.wearemachina.workorders.persistence;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Central registry of every {@link NamespacedKey} this plugin writes to entity PDC. */
public final class Keys {

    /** The full {@link com.wearemachina.workorders.model.GolemState} blob. */
    public final NamespacedKey state;

    /** Cheap byte marker: "this golem is managed by Work Orders" (avoids deserialising the blob to filter). */
    public final NamespacedKey managed;

    /** Item-PDC tag on a Work Order, holding the {@link com.wearemachina.workorders.model.RoleType} name. */
    public final NamespacedKey itemRole;

    public Keys(Plugin plugin) {
        this.state = new NamespacedKey(plugin, "state");
        this.managed = new NamespacedKey(plugin, "managed");
        this.itemRole = new NamespacedKey(plugin, "item_role");
    }
}
