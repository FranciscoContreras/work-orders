package com.wearemachina.workorders.role;

import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;

/** A job a golem can perform. Advanced once per <i>service</i> tick (not every game tick). Must be dupe-safe. */
public interface Role {

    RoleType type();

    void tick(GolemContext ctx);
}
