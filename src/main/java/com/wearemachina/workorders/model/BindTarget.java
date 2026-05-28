package com.wearemachina.workorders.model;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * An immutable reference to a block position: a world UUID plus integer coordinates.
 * Never holds a live {@link org.bukkit.block.Block} or {@link World} reference (no leaks across unloads).
 */
public final class BindTarget {

    private final UUID world;
    private final int x;
    private final int y;
    private final int z;

    public BindTarget(UUID world, int x, int y, int z) {
        this.world = Objects.requireNonNull(world, "world");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BindTarget of(Location loc) {
        return new BindTarget(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public UUID world() {
        return world;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    /** @return the world if currently loaded, else {@code null}. */
    public World resolveWorld() {
        return Bukkit.getWorld(world);
    }

    /** @return a Location at the block centre if the world is loaded, else {@code null}. */
    public Location toLocation() {
        World w = resolveWorld();
        return (w == null) ? null : new Location(w, x + 0.5, y, z + 0.5);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BindTarget b)) {
            return false;
        }
        return x == b.x && y == b.y && z == b.z && world.equals(b.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }

    @Override
    public String toString() {
        return "BindTarget(" + x + ", " + y + ", " + z + ")";
    }
}
