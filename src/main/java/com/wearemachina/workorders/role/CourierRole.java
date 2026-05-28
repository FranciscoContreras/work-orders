package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.function.Predicate;
import org.bukkit.inventory.ItemStack;

/** Moves items from the source to the destination, honouring the golem's filter (empty filter = move all). */
public final class CourierRole extends AbstractHaulRole {

    @Override
    public RoleType type() {
        return RoleType.COURIER;
    }

    @Override
    protected Predicate<ItemStack> intake(GolemContext ctx, ContainerAccess.Resolution dst) {
        ItemFilter filter = ctx.state.filter();
        return filter::matches;
    }
}
