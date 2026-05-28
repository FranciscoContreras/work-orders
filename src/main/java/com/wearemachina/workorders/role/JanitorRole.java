package com.wearemachina.workorders.role;

import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import org.bukkit.Location;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

/**
 * Vacuums dropped items within a small radius of the golem and banks them in the bound destination. This
 * is the only role that scans entities — it does so only on its own service tick, only when a player is
 * near (the tick service gates it), and within a small capped radius.
 */
public final class JanitorRole implements Role {

    @Override
    public RoleType type() {
        return RoleType.JANITOR;
    }

    @Override
    public void tick(GolemContext ctx) {
        if (ctx.state.dest() == null) {
            ctx.status("no-route");
            ctx.pose(CopperGolem.State.IDLE);
            return;
        }
        ContainerAccess.Resolution bank = ctx.containers.resolve(ctx.state.dest());
        if (bank.status == ContainerAccess.Status.NOT_CONTAINER) {
            ctx.state.dest(null);
            ctx.save();
            ctx.status("no-route");
            ctx.fx.stuck(ctx.golem);
            return;
        }
        if (!bank.isOk()) {
            return; // unloaded — idle, retry later
        }
        if (ctx.state.carrying()) {
            deposit(ctx, bank);
        } else {
            vacuum(ctx);
        }
    }

    private void vacuum(GolemContext ctx) {
        CopperGolem golem = ctx.golem;
        double r = ctx.cfg.janitorRadius;
        Item best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entity e : golem.getNearbyEntities(r, r, r)) {
            if (!(e instanceof Item item) || item.isDead()) {
                continue;
            }
            ItemStack s = item.getItemStack();
            if (s == null || s.getType().isAir()) {
                continue;
            }
            double d = e.getLocation().distanceSquared(golem.getLocation());
            if (d < bestSq) {
                bestSq = d;
                best = item;
            }
        }
        if (best == null) {
            ctx.status("idle");
            ctx.pose(CopperGolem.State.GETTING_NO_ITEM);
            return;
        }
        Location target = best.getLocation();
        if (!ctx.inReach(target)) {
            ctx.status("tidying");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.GETTING_ITEM);
            return;
        }
        ItemStack stack = best.getItemStack();
        int take = Math.min(stack.getAmount(), ctx.cfg.pullBatch);
        ItemStack carried = stack.clone();
        carried.setAmount(take);
        if (stack.getAmount() == take) {
            best.remove();
        } else {
            stack.setAmount(stack.getAmount() - take);
            best.setItemStack(stack);
        }
        ctx.holdCargo(carried);
        ctx.state.carrying(true);
        ctx.save();
        ctx.pose(CopperGolem.State.GETTING_ITEM);
    }

    private void deposit(GolemContext ctx, ContainerAccess.Resolution bank) {
        Location target = ctx.state.dest().toLocation();
        if (!ctx.inReach(target)) {
            ctx.status("tidying");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.DROPPING_ITEM);
            ctx.fx.carryTrail(ctx.golem);
            return;
        }
        ItemStack cargo = ctx.cargo();
        if (cargo == null || cargo.getType().isAir()) {
            ctx.state.carrying(false);
            ctx.showLabel();
            ctx.save();
            return;
        }
        ItemStack leftover = ctx.deposit.deposit(bank, cargo);
        if (leftover == null || leftover.getAmount() == 0) {
            ctx.state.carrying(false);
            ctx.showLabel();
            ctx.save();
            ctx.status("idle");
            ctx.pose(CopperGolem.State.IDLE);
        } else {
            ctx.holdCargo(leftover); // bank full — keep holding, never drop (no source to return to)
            ctx.status("dest-full");
            ctx.save();
        }
    }
}
