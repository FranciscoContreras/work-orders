package com.wearemachina.workorders.role;

import com.wearemachina.workorders.model.RoleType;
import com.wearemachina.workorders.runtime.GolemContext;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
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

    @Override
    public RoleType type() {
        return RoleType.FARMHAND;
    }

    @Override
    public void tick(GolemContext ctx) {
        Block crop = findMatureCrop(ctx);
        if (crop == null) {
            ctx.status("idle");
            ctx.pose(CopperGolem.State.GETTING_NO_ITEM);
            return;
        }
        Location target = crop.getLocation().add(0.5, 0.0, 0.5);
        if (!ctx.inReach(target)) {
            ctx.status("tending");
            ctx.moveTo(target);
            ctx.pose(CopperGolem.State.GETTING_ITEM);
            return;
        }
        harvestAndReplant(crop);
        ctx.status("tending");
        ctx.pose(CopperGolem.State.DROPPING_ITEM);
        ctx.fx.carryTrail(ctx.golem);
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
