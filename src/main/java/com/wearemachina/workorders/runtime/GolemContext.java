package com.wearemachina.workorders.runtime;

import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.DepositPolicy;
import com.wearemachina.workorders.interact.Feedback;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.persistence.GolemStore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

/**
 * Per-service-tick handle a role uses to act on one golem: its live state, the services it needs, and
 * the small set of physical actions (move, reach test, hold cargo vs show the role tool, set the vanilla
 * work pose, report status). Built fresh each tick by {@link GolemTickService}; never stored.
 */
public final class GolemContext {

    private static final double REACH_SQ = 2.6 * 2.6;

    public final CopperGolem golem;
    public final GolemState state;
    public final PluginConfig cfg;
    public final ContainerAccess containers;
    public final DepositPolicy deposit;
    public final Feedback fx;
    private final GolemStore store;
    private final StatusBoard statusBoard;
    private boolean pathFailed;

    public GolemContext(CopperGolem golem, GolemState state, PluginConfig cfg, ContainerAccess containers,
                        DepositPolicy deposit, Feedback fx, GolemStore store, StatusBoard statusBoard) {
        this.golem = golem;
        this.state = state;
        this.cfg = cfg;
        this.containers = containers;
        this.deposit = deposit;
        this.fx = fx;
        this.store = store;
        this.statusBoard = statusBoard;
    }

    public void save() {
        store.write(golem, state);
    }

    public Location golemLocation() {
        return golem.getLocation();
    }

    /** Put REAL cargo in the hand (drop chance 1 so it drops on death — never lost). */
    public void holdCargo(ItemStack cargo) {
        EntityEquipment eq = golem.getEquipment();
        if (eq != null) {
            eq.setItemInMainHand(cargo);
            eq.setItemInMainHandDropChance(1f);
        }
    }

    public ItemStack cargo() {
        EntityEquipment eq = golem.getEquipment();
        return eq == null ? null : eq.getItemInMainHand();
    }

    /** Restore the cosmetic role tool (drop chance 0 — a copy, never dropped, never duped). */
    public void showLabel() {
        EntityEquipment eq = golem.getEquipment();
        if (eq != null) {
            Material label = state.labelItem();
            eq.setItemInMainHand(label == null ? null : new ItemStack(label));
            eq.setItemInMainHandDropChance(0f);
        }
    }

    public boolean inReach(Location target) {
        if (target == null) {
            return false;
        }
        Location g = golem.getLocation();
        return g.getWorld() != null && g.getWorld().equals(target.getWorld())
                && g.distanceSquared(target) <= REACH_SQ;
    }

    /** Drive the golem toward a target; records whether pathfinding failed (for stuck detection). */
    public void moveTo(Location target) {
        if (target != null) {
            pathFailed = !golem.getPathfinder().moveTo(target, cfg.moveSpeed);
        }
    }

    public boolean movedFailed() {
        return pathFailed;
    }

    public void pose(CopperGolem.State pose) {
        golem.setGolemState(pose);
    }

    /** Report a short live status key (e.g. "hauling", "dest-full") shown when the owner inspects. */
    public void status(String key) {
        statusBoard.set(golem.getUniqueId(), key);
    }

    public void lookAt(Entity target) {
        golem.lookAt(target);
    }
}
