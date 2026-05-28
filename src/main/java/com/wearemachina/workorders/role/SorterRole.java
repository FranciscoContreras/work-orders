package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.Hauling;
import com.wearemachina.workorders.model.BindTarget;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sorts one source container's items across several destination chests by what each already holds — the
 * classic "seed each chest with one of the item" sorter, but golem-driven. It only ever picks up items
 * that have a home (a destination already containing that type), so unsortable items simply pile up in
 * the source. Built to stay out of native sorting's lane: bind a regular chest/barrel as the source, not
 * a copper chest. Dupe/void-safe — homeless or undeliverable cargo is returned to the source, never lost.
 */
public final class SorterRole implements Role {

    @Override
    public RoleType type() {
        return RoleType.SORTER;
    }

    @Override
    public void tick(GolemContext ctx) {
        if (ctx.state.source() == null || ctx.state.sortDests().isEmpty()) {
            ctx.status("no-route");
            ctx.pose(CopperGolem.State.IDLE);
            return;
        }
        ContainerAccess.Resolution src = ctx.containers.resolve(ctx.state.source());
        if (src.status == ContainerAccess.Status.NOT_CONTAINER) {
            ctx.state.source(null);
            ctx.save();
            ctx.status("no-route");
            ctx.fx.stuck(ctx.golem);
            return;
        }
        if (!src.isOk()) {
            return; // source chunk/world not loaded — idle, retry later
        }
        if (ctx.state.carrying()) {
            depositPhase(ctx, src);
        } else {
            pullPhase(ctx, src);
        }
    }

    private void pullPhase(GolemContext ctx, ContainerAccess.Resolution src) {
        Location source = ctx.state.source().toLocation();
        if (!ctx.inReach(source)) {
            ctx.status("hauling");
            ctx.moveTo(source);
            ctx.pose(CopperGolem.State.GETTING_ITEM);
            return;
        }
        ItemFilter filter = ctx.state.filter();
        // Only pull items that the filter allows AND that have a destination waiting for them.
        ItemStack carried = Hauling.pull(src.container.getInventory(),
                it -> filter.matches(it) && findDest(ctx, it.getType()) != null, ctx.cfg.pullBatch);
        if (carried == null) {
            ctx.status("idle");
            ctx.pose(CopperGolem.State.GETTING_NO_ITEM);
            return;
        }
        ctx.holdCargo(carried);
        ctx.state.carrying(true);
        ctx.save();
        ctx.status("hauling");
        ctx.pose(CopperGolem.State.GETTING_ITEM);
    }

    private void depositPhase(GolemContext ctx, ContainerAccess.Resolution src) {
        ItemStack cargo = ctx.cargo();
        if (cargo == null || cargo.getType().isAir()) {
            finish(ctx);
            return;
        }
        BindTarget destTarget = findDest(ctx, cargo.getType());
        if (destTarget == null) {
            returnToSource(ctx, src, cargo); // no home anymore — give it back
            return;
        }
        ContainerAccess.Resolution dst = ctx.containers.resolve(destTarget);
        if (!dst.isOk()) {
            returnToSource(ctx, src, cargo);
            return;
        }
        Location target = destTarget.toLocation();
        if (!ctx.inReach(target)) {
            ctx.status("hauling");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.DROPPING_ITEM);
            ctx.fx.carryTrail(ctx.golem);
            return;
        }
        int before = cargo.getAmount();
        ItemStack leftover = ctx.deposit.deposit(dst, cargo);
        int leftAmt = (leftover == null) ? 0 : leftover.getAmount();
        if (leftAmt == 0) {
            finish(ctx);
        } else if (leftAmt < before) {
            ctx.holdCargo(leftover); // partial — carry the rest onward
            ctx.save();
        } else {
            returnToSource(ctx, src, leftover); // that destination filled up — return to source
        }
    }

    private void returnToSource(GolemContext ctx, ContainerAccess.Resolution src, ItemStack cargo) {
        ItemStack back = ctx.deposit.depositGeneric(src.container.getInventory(), cargo);
        if (back == null || back.getAmount() == 0) {
            finish(ctx);
        } else {
            ctx.holdCargo(back); // source also full — idle holding, never drop
            ctx.status("dest-full");
            ctx.save();
        }
    }

    private void finish(GolemContext ctx) {
        ctx.state.carrying(false);
        ctx.showLabel();
        ctx.save();
        ctx.status("idle");
        ctx.pose(CopperGolem.State.IDLE);
    }

    /** First bound destination that already contains {@code type} (so it's the home for that item). */
    private BindTarget findDest(GolemContext ctx, Material type) {
        for (BindTarget t : ctx.state.sortDests()) {
            ContainerAccess.Resolution r = ctx.containers.resolve(t);
            if (r.isOk() && containsType(r.container.getInventory(), type)) {
                return t;
            }
        }
        return null;
    }

    private static boolean containsType(Inventory inv, Material type) {
        for (ItemStack it : inv.getContents()) {
            if (it != null && it.getType() == type) {
                return true;
            }
        }
        return false;
    }
}
