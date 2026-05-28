package com.wearemachina.workorders.role;

import com.wearemachina.workorders.model.RoleType;
import java.util.EnumMap;
import java.util.Map;

/** Holds one singleton {@link Role} per {@link RoleType}. Roles are stateless (per-golem state lives in PDC). */
public final class RoleRegistry {

    private final Map<RoleType, Role> roles = new EnumMap<>(RoleType.class);

    public RoleRegistry() {
        register(new CourierRole());
        register(new StokerRole());
        register(new RestockerRole());
        register(new JanitorRole());
        register(new SorterRole());
        register(new FarmhandRole());
    }

    private void register(Role role) {
        roles.put(role.type(), role);
    }

    public Role get(RoleType type) {
        return type == null ? null : roles.get(type);
    }
}
