package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.function.Predicate;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps a furnace / smoker / blast furnace fed: pulls only items the bound furnace accepts (fuel or
 * smeltable for <i>that</i> furnace type) and the {@link com.wearemachina.workorders.haul.DepositPolicy}
 * routes them to the right slot.
 */
public final class StokerRole extends AbstractHaulRole {

    @Override
    public RoleType type() {
        return RoleType.STOKER;
    }

    @Override
    protected Predicate<ItemStack> intake(GolemContext ctx, ContainerAccess.Resolution dst) {
        if (dst.kind != ContainerAccess.Kind.FURNACE) {
            return it -> false; // destination isn't a furnace — nothing sensible to carry
        }
        FurnaceInventory fi = (FurnaceInventory) dst.container.getInventory();
        return it -> fi.isFuel(it) || fi.canSmelt(it);
    }
}
