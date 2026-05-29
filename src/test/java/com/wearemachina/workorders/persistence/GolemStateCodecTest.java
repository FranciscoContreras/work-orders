package com.wearemachina.workorders.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wearemachina.workorders.model.BindTarget;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.model.RoleType;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * The codec is the player-data layer: a bug here silently loses or corrupts golem routes/ownership on the
 * next save. These cover the full round-trip, the documented fail-safe behaviours (null/empty/truncated
 * blobs must never throw out of a PDC read), and the schema guard. Pure logic; only {@link Material} (an
 * enum) is referenced, so no server is needed.
 */
class GolemStateCodecTest {

    @Test
    void roundTripPreservesEveryField() {
        UUID world = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        GolemState s = GolemState.empty();
        s.role(RoleType.SORTER);
        s.follow(true);
        s.carrying(true);
        s.source(new BindTarget(world, 10, 64, -20));
        s.dest(new BindTarget(world, 11, 65, -21));
        s.owner(UUID.randomUUID());
        s.threshold(42);
        s.labelItem(Material.HOPPER);
        s.filter(new ItemFilter(EnumSet.of(Material.WHEAT, Material.IRON_INGOT), ItemFilter.Mode.BLACKLIST));
        s.addSortDest(new BindTarget(world, 1, 2, 3));
        s.addSortDest(new BindTarget(world, 4, 5, 6));
        s.trusted().add(friend);
        s.baseName("Pebble");

        GolemState d = GolemStateCodec.decode(GolemStateCodec.encode(s));

        assertEquals(RoleType.SORTER, d.role());
        assertTrue(d.follow());
        assertTrue(d.carrying());
        assertEquals(s.source(), d.source());
        assertEquals(s.dest(), d.dest());
        assertEquals(s.owner(), d.owner());
        assertEquals(42, d.threshold());
        assertEquals(Material.HOPPER, d.labelItem());
        assertEquals(ItemFilter.Mode.BLACKLIST, d.filter().mode());
        assertEquals(EnumSet.of(Material.WHEAT, Material.IRON_INGOT), d.filter().materials());
        assertEquals(s.sortDests(), d.sortDests());
        assertEquals(1, d.trusted().size());
        assertEquals(friend, d.trusted().get(0));
        assertEquals("Pebble", d.baseName());
    }

    @Test
    void emptyStateRoundTripsToEmpty() {
        GolemState d = GolemStateCodec.decode(GolemStateCodec.encode(GolemState.empty()));
        assertNull(d.role());
        assertFalse(d.follow());
        assertFalse(d.carrying());
        assertNull(d.source());
        assertNull(d.dest());
        assertNull(d.owner());
        assertNull(d.labelItem());
        assertNull(d.baseName());
        assertTrue(d.filter().isEmpty());
        assertTrue(d.sortDests().isEmpty());
        assertTrue(d.trusted().isEmpty());
    }

    @Test
    void nullAndEmptyBlobsDecodeToEmptyWithoutThrowing() {
        assertNull(GolemStateCodec.decode(null).role());
        assertNull(GolemStateCodec.decode(new byte[0]).role());
    }

    @Test
    void truncatedBlobNeverThrowsAndDecodesUsableState() {
        byte[] full = GolemStateCodec.encode(richState());
        // A shorter blob is exactly what an older plugin version (fewer trailing fields) looks like, and
        // what a corrupt NBT read can hand us. Every prefix length must fail safe, not throw.
        for (int len = 0; len <= full.length; len++) {
            byte[] chopped = Arrays.copyOf(full, len);
            assertDoesNotThrow(() -> GolemStateCodec.decode(chopped), "decode must not throw at length " + len);
        }
    }

    @Test
    void schemaBelowOneDecodesToEmpty() {
        byte[] schemaZero = {0, 0, 0, 0}; // writeInt(0): an unrecognised/older schema
        assertNull(GolemStateCodec.decode(schemaZero).role(), "an unreadable schema must fail safe to empty");
    }

    @Test
    void forwardCompatTrailingFieldDefaultsWhenAbsent() {
        // A blob from before baseName existed simply ends earlier; decode must default baseName to null
        // rather than throw. We approximate that "older blob" by re-decoding a state that never set it.
        GolemState noName = GolemState.empty();
        noName.role(RoleType.JANITOR);
        GolemState d = GolemStateCodec.decode(GolemStateCodec.encode(noName));
        assertEquals(RoleType.JANITOR, d.role());
        assertNull(d.baseName());
    }

    private static GolemState richState() {
        UUID world = UUID.randomUUID();
        GolemState s = GolemState.empty();
        s.role(RoleType.COURIER);
        s.follow(true);
        s.source(new BindTarget(world, 1, 2, 3));
        s.dest(new BindTarget(world, 4, 5, 6));
        s.owner(UUID.randomUUID());
        s.threshold(7);
        s.labelItem(Material.CHEST);
        s.filter(new ItemFilter(EnumSet.of(Material.STONE), ItemFilter.Mode.WHITELIST));
        s.addSortDest(new BindTarget(world, 9, 9, 9));
        s.trusted().add(UUID.randomUUID());
        s.baseName("Rusty");
        return s;
    }
}
