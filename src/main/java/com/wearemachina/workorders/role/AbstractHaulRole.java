package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.Hauling;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.CopperGolem;
import org.bukkit.inventory.ItemStack;

/**
 * Shared carry cycle for roles that move items from one bound container to another (Courier, Stoker,
 * Restocker). The cycle is: seek source &rarr; pull &rarr; seek destination &rarr; deposit, with
 * return-to-sender if the destination can't take everything. Cargo lives only in the golem's hand and is
 * moved atomically, so items are never lost or duplicated. Subclasses supply what to pull and how much.
 */
public abstract class AbstractHaulRole implements Role {

    /** What the golem should pull from the source this cycle (the destination is available for context). */
    protected abstract Predicate<ItemStack> intake(GolemContext ctx, ContainerAccess.Resolution dst);

    /** How many items to pull this cycle (default: the configured batch). */
    protected int intakeAmount(GolemContext ctx, ContainerAccess.Resolution dst) {
        return ctx.cfg.pullBatch;
    }

    /** Whether the golem should bother pulling right now (e.g. Restocker stops when the destination is full). */
    protected boolean canStart(GolemContext ctx, ContainerAccess.Resolution dst) {
        return true;
    }

    @Override
    public void tick(GolemContext ctx) {
        GolemState st = ctx.state;
        if (st.source() == null || st.dest() == null) {
            ctx.status("no-route");
            ctx.pose(CopperGolem.State.IDLE);
            return;
        }
        ContainerAccess.Resolution dst = ctx.containers.resolve(st.dest());
        if (unavailable(ctx, dst, true)) {
            return;
        }
        ContainerAccess.Resolution src = ctx.containers.resolve(st.source());
        if (unavailable(ctx, src, false)) {
            return;
        }
        if (st.carrying()) {
            depositPhase(ctx, dst, src);
        } else {
            pullPhase(ctx, src, dst);
        }
    }

    private void pullPhase(GolemContext ctx, ContainerAccess.Resolution src, ContainerAccess.Resolution dst) {
        if (!canStart(ctx, dst)) {
            ctx.status("idle");
            ctx.pose(CopperGolem.State.IDLE);
            return;
        }
        Location target = ctx.state.source().toLocation();
        if (!ctx.inReach(target)) {
            ctx.status("hauling");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.GETTING_ITEM);
            return;
        }
        int amount = intakeAmount(ctx, dst);
        if (amount <= 0) {
            ctx.status("idle");
            ctx.pose(CopperGolem.State.IDLE);
            return;
        }
        ItemStack carried = Hauling.pull(src.container.getInventory(), intake(ctx, dst), amount);
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

    private void depositPhase(GolemContext ctx, ContainerAccess.Resolution dst, ContainerAccess.Resolution src) {
        Location target = ctx.state.dest().toLocation();
        if (!ctx.inReach(target)) {
            ctx.status("hauling");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.DROPPING_ITEM);
            ctx.fx.carryTrail(ctx.golem);
            return;
        }
        ItemStack cargo = ctx.cargo();
        if (cargo == null || cargo.getType().isAir()) {
            finishCarrying(ctx); // defensive: hand somehow empty
            return;
        }
        int before = cargo.getAmount();
        ItemStack leftover = ctx.deposit.deposit(dst, cargo);
        int leftAmt = (leftover == null) ? 0 : leftover.getAmount();

        if (leftAmt == 0) {
            finishCarrying(ctx);
        } else if (leftAmt < before) {
            ctx.holdCargo(leftover); // partial deposit; carry the remainder onward
            ctx.status("hauling");
            ctx.save();
        } else {
            // Destination couldn't take any: return what we carry back to the source. Never drop/delete.
            ItemStack back = ctx.deposit.depositGeneric(src.container.getInventory(), leftover);
            if (back == null || back.getAmount() == 0) {
                finishCarrying(ctx);
            } else {
                ctx.holdCargo(back); // both full — idle holding the stack
                ctx.status("dest-full");
                ctx.save();
            }
        }
    }

    private void finishCarrying(GolemContext ctx) {
        ctx.state.carrying(false);
        ctx.showLabel();
        ctx.save();
        ctx.status("idle");
        ctx.pose(CopperGolem.State.IDLE);
    }

    /** @return true if the role should stop this tick because a target is unavailable. */
    private boolean unavailable(GolemContext ctx, ContainerAccess.Resolution res, boolean isDest) {
        switch (res.status) {
            case OK:
                return false;
            case NOT_CONTAINER:
                // The bound block was broken/replaced — forget it and surface a small reaction.
                if (isDest) {
                    ctx.state.dest(null);
                } else {
                    ctx.state.source(null);
                }
                ctx.save();
                ctx.status("no-route");
                ctx.fx.stuck(ctx.golem);
                return true;
            default: // WORLD_UNLOADED / CHUNK_UNLOADED — idle quietly, retry when loaded
                return true;
        }
    }
}
