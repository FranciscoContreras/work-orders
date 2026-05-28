package com.wearemachina.workorders.haul;

import com.wearemachina.workorders.model.BindTarget;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;

/**
 * Resolves a {@link BindTarget} to a live container and classifies it, without ever force-loading chunks.
 * The {@link Status} lets roles distinguish "transiently unavailable" (idle quietly) from "no longer a
 * container" (the block was broken/replaced — unbind it).
 */
public final class ContainerAccess {

    public enum Kind { GENERIC, FURNACE, BREWING }

    public enum Status { OK, WORLD_UNLOADED, CHUNK_UNLOADED, NOT_CONTAINER }

    public static final class Resolution {
        public final Status status;
        public final Container container;
        public final Kind kind;

        private Resolution(Status status, Container container, Kind kind) {
            this.status = status;
            this.container = container;
            this.kind = kind;
        }

        static Resolution fail(Status status) {
            return new Resolution(status, null, null);
        }

        static Resolution ok(Container container, Kind kind) {
            return new Resolution(Status.OK, container, kind);
        }

        public boolean isOk() {
            return status == Status.OK;
        }
    }

    public Resolution resolve(BindTarget target) {
        if (target == null) {
            return Resolution.fail(Status.NOT_CONTAINER);
        }
        World world = target.resolveWorld();
        if (world == null) {
            return Resolution.fail(Status.WORLD_UNLOADED);
        }
        if (!world.isChunkLoaded(target.x() >> 4, target.z() >> 4)) {
            return Resolution.fail(Status.CHUNK_UNLOADED); // never force-load
        }
        BlockState state = world.getBlockAt(target.x(), target.y(), target.z()).getState();
        if (state instanceof Furnace furnace) {
            return Resolution.ok(furnace, Kind.FURNACE);
        }
        if (state instanceof BrewingStand stand) {
            return Resolution.ok(stand, Kind.BREWING);
        }
        if (state instanceof Container container) {
            return Resolution.ok(container, Kind.GENERIC);
        }
        return Resolution.fail(Status.NOT_CONTAINER);
    }
}
