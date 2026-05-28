package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.Hauling;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.function.Predicate;
import org.bukkit.inventory.ItemStack;

/** Keeps the destination topped up to a threshold of the filtered item(s), pulling from the source. */
public final class RestockerRole extends AbstractHaulRole {

    @Override
    public RoleType type() {
        return RoleType.RESTOCKER;
    }

    @Override
    protected Predicate<ItemStack> intake(GolemContext ctx, ContainerAccess.Resolution dst) {
        ItemFilter filter = ctx.state.filter();
        return filter::matches;
    }

    @Override
    protected boolean canStart(GolemContext ctx, ContainerAccess.Resolution dst) {
        return have(ctx, dst) < threshold(ctx);
    }

    @Override
    protected int intakeAmount(GolemContext ctx, ContainerAccess.Resolution dst) {
        int need = threshold(ctx) - have(ctx, dst);
        return Math.max(0, Math.min(ctx.cfg.pullBatch, need));
    }

    private int threshold(GolemContext ctx) {
        int t = ctx.state.threshold();
        return t > 0 ? t : ctx.cfg.restockerDefaultThreshold;
    }

    private int have(GolemContext ctx, ContainerAccess.Resolution dst) {
        ItemFilter filter = ctx.state.filter();
        return Hauling.count(dst.container.getInventory(), filter::matches);
    }
}
