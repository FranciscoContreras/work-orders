package com.wearemachina.workorders.role;

import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.CopperGolem;
import org.bukkit.inventory.ItemStack;

/**
 * Tends a patch of farmland around the golem: walks to the nearest mature crop, harvests it, and replants
 * it in place. The produce drops on the ground (exactly like a player breaking it) for a Janitor or hopper
 * to collect — which keeps this dupe/void-safe and avoids carrying mixed item types. No binding needed:
 * stand the golem in the field and it tends the radius around itself.
 */
public final class FarmhandRole implements Role {

    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);

    // Transient per-golem "currently heading to this crop" cache. A full radius scan is O(r^3) and r can
    // be up to 48, so rescanning every tick while a golem walks to a crop it already found is wasteful.
    // Instead we remember the target and re-validate it cheaply (one block read), only falling back to a
    // full scan when there's no longer a valid target (harvested / matured away / out of range). Main-
    // thread only (the tick service is single-threaded), so a plain HashMap is safe; misses drop the entry.
    private final Map<UUID, Location> targets = new HashMap<>();

    @Override
    public RoleType type() {
        return RoleType.FARMHAND;
    }

    @Override
    public void tick(GolemContext ctx) {
        UUID id = ctx.golem.getUniqueId();
        Block crop = reuseOrFindCrop(ctx, id);
        if (crop == null) {
            targets.remove(id);
            ctx.status("idle");
            ctx.pose(CopperGolem.State.GETTING_NO_ITEM);
            return;
        }
        Location target = crop.getLocation().add(0.5, 0.0, 0.5);
        if (!ctx.inReach(target)) {
            targets.put(id, crop.getLocation()); // keep this target so we skip the scan while walking to it
            ctx.status("tending");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.GETTING_ITEM);
            return;
        }
        harvestAndReplant(crop);
        targets.remove(id); // consumed — next tick rescans for the next nearest crop
        ctx.status("tending");
        ctx.pose(CopperGolem.State.DROPPING_ITEM);
        ctx.fx.carryTrail(ctx.golem);
    }

    /**
     * Reuse the golem's current crop target if it's still mature and in range (one cheap block read);
     * otherwise fall back to the full radius scan for the nearest mature crop. This keeps the expensive
     * scan to roughly once per crop harvested rather than once per service tick while the golem walks.
     */
    private Block reuseOrFindCrop(GolemContext ctx, UUID id) {
        Location cached = targets.get(id);
        if (cached != null) {
            Block b = cached.getBlock();
            if (isMature(b) && inRange(ctx, b)) {
                return b;
            }
            targets.remove(id);
        }
        return findMatureCrop(ctx);
    }

    /** Cheap re-validation that a cached crop is still within the golem's work box (matches the scan bounds). */
    private static boolean inRange(GolemContext ctx, Block b) {
        Location gl = ctx.golem.getLocation();
        int r = ctx.cfg.farmhandRadius;
        return b.getWorld().equals(gl.getWorld())
                && Math.abs(b.getX() - gl.getBlockX()) <= r
                && Math.abs(b.getY() - gl.getBlockY()) <= 1
                && Math.abs(b.getZ() - gl.getBlockZ()) <= r;
    }

    private Block findMatureCrop(GolemContext ctx) {
        CopperGolem golem = ctx.golem;
        World world = golem.getWorld();
        int r = ctx.cfg.farmhandRadius;
        Location gl = golem.getLocation();
        int bx = gl.getBlockX();
        int by = gl.getBlockY();
        int bz = gl.getBlockZ();
        Block best = null;
        double bestSq = Double.MAX_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (!isMature(b)) {
                        continue;
                    }
                    double d = (dx * dx) + (dy * dy) + (dz * dz);
                    if (d < bestSq) {
                        bestSq = d;
                        best = b;
                    }
                }
            }
        }
        return best;
    }

    private static boolean isMature(Block b) {
        if (!CROPS.contains(b.getType())) {
            return false;
        }
        return b.getBlockData() instanceof Ageable age && age.getAge() == age.getMaximumAge();
    }

    /** Harvest the mature crop (drops on the ground) and replant it at age 0. No item is created or lost. */
    private static void harvestAndReplant(Block b) {
        World world = b.getWorld();
        Material crop = b.getType();
        Collection<ItemStack> drops = b.getDrops(); // the legitimate harvest (produce + any seeds)
        BlockData fresh = Bukkit.createBlockData(crop);
        if (fresh instanceof Ageable age) {
            age.setAge(0);
            fresh = age;
        }
        b.setBlockData(fresh); // replant in place — the golem keeps the patch growing
        Location at = b.getLocation().add(0.5, 0.2, 0.5);
        for (ItemStack drop : drops) {
            world.dropItemNaturally(at, drop);
        }
    }
}
