package com.wearemachina.workorders.persistence;

import com.wearemachina.workorders.model.BindTarget;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.model.ItemFilter;
import com.wearemachina.workorders.model.RoleType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;

/**
 * Versioned (de)serialisation of {@link GolemState} to a compact byte[] blob.
 *
 * <p>Materials are stored by <b>name</b> (enum ordinals are not stable across Minecraft versions);
 * unknown names are silently dropped on read (forward-compatible). Decoding a corrupt/truncated blob
 * fails safe to a less-configured state rather than throwing. Pure logic — unit-testable (round-trip
 * and schema migration).
 */
public final class GolemStateCodec {

    private GolemStateCodec() {
    }

    public static byte[] encode(GolemState s) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeInt(GolemState.CURRENT_SCHEMA);
            out.writeByte(s.role() == null ? -1 : s.role().ordinal());
            int flags = 0;
            if (s.follow()) {
                flags |= 0x1;
            }
            if (s.filter().mode() == ItemFilter.Mode.BLACKLIST) {
                flags |= 0x2;
            }
            if (s.carrying()) {
                flags |= 0x4;
            }
            out.writeByte(flags);
            writeTarget(out, s.source());
            writeTarget(out, s.dest());
            writeUuid(out, s.owner());
            out.writeInt(s.threshold());
            writeMaterial(out, s.labelItem());
            Set<Material> mats = s.filter().materials();
            out.writeInt(mats.size());
            for (Material m : mats) {
                out.writeUTF(m.name());
            }
            out.writeInt(s.sortDests().size());
            for (BindTarget t : s.sortDests()) {
                writeTarget(out, t);
            }
            out.writeInt(s.trusted().size());
            for (UUID id : s.trusted()) {
                writeUuid(out, id);
            }
            writeNullableString(out, s.baseName()); // trailing field — old readers ignore it, old blobs EOF to null
        } catch (IOException e) {
            throw new UncheckedIOException(e); // ByteArrayOutputStream never actually throws
        }
        return bos.toByteArray();
    }

    public static GolemState decode(byte[] bytes) {
        GolemState s = GolemState.empty();
        if (bytes == null || bytes.length == 0) {
            return s;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int schema = in.readInt();
            // Only schema 1 exists today. Future versions branch on `schema` here, reading the old
            // layout then upgrading in memory; the next eager write persists the new layout.
            if (schema < 1) {
                return s;
            }
            s.role(RoleType.fromByte(in.readByte()));
            int flags = in.readUnsignedByte();
            s.follow((flags & 0x1) != 0);
            boolean blacklist = (flags & 0x2) != 0;
            s.carrying((flags & 0x4) != 0);
            s.source(readTarget(in));
            s.dest(readTarget(in));
            s.owner(readUuid(in));
            s.threshold(in.readInt());
            s.labelItem(matchMaterial(readNullableString(in)));
            int count = in.readInt();
            EnumSet<Material> mats = EnumSet.noneOf(Material.class);
            for (int i = 0; i < count; i++) {
                Material m = matchMaterial(in.readUTF());
                if (m != null) {
                    mats.add(m);
                }
            }
            s.filter(new ItemFilter(mats, blacklist ? ItemFilter.Mode.BLACKLIST : ItemFilter.Mode.WHITELIST));
            int sortCount = in.readInt(); // absent in pre-Sorter blobs -> EOF -> caught below -> empty (fine)
            for (int i = 0; i < sortCount; i++) {
                s.addSortDest(readTarget(in));
            }
            int trustCount = in.readInt(); // absent in older blobs -> EOF -> caught below -> empty (fine)
            for (int i = 0; i < trustCount; i++) {
                UUID id = readUuid(in);
                if (id != null) {
                    s.trusted().add(id);
                }
            }
            s.baseName(readNullableString(in)); // absent in pre-nameplate blobs -> EOF -> caught below -> null (fine)
        } catch (IOException e) {
            // Truncated/corrupt: keep whatever parsed; never throw out of a PDC read.
            return s;
        }
        return s;
    }

    private static void writeTarget(DataOutputStream out, BindTarget t) throws IOException {
        if (t == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        UUID w = t.world();
        out.writeLong(w.getMostSignificantBits());
        out.writeLong(w.getLeastSignificantBits());
        out.writeInt(t.x());
        out.writeInt(t.y());
        out.writeInt(t.z());
    }

    private static BindTarget readTarget(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        UUID w = new UUID(in.readLong(), in.readLong());
        return new BindTarget(w, in.readInt(), in.readInt(), in.readInt());
    }

    private static void writeUuid(DataOutputStream out, UUID id) throws IOException {
        if (id == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeMaterial(DataOutputStream out, Material m) throws IOException {
        if (m == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeUTF(m.name());
    }

    private static void writeNullableString(DataOutputStream out, String v) throws IOException {
        if (v == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeUTF(v);
    }

    private static String readNullableString(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    private static Material matchMaterial(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
