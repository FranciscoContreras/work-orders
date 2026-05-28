package com.wearemachina.workorders.persistence;

import com.wearemachina.workorders.model.GolemState;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

/** PDC adapter storing a {@link GolemState} as a single {@code byte[]} blob on the golem entity. */
public final class GolemStateType implements PersistentDataType<byte[], GolemState> {

    public static final GolemStateType INSTANCE = new GolemStateType();

    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Override
    public Class<GolemState> getComplexType() {
        return GolemState.class;
    }

    @Override
    public byte[] toPrimitive(GolemState state, PersistentDataAdapterContext context) {
        return GolemStateCodec.encode(state);
    }

    @Override
    public GolemState fromPrimitive(byte[] bytes, PersistentDataAdapterContext context) {
        return GolemStateCodec.decode(bytes);
    }
}
