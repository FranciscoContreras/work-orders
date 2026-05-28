package com.wearemachina.workorders.persistence;

import com.wearemachina.workorders.model.GolemState;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads and writes {@link GolemState} on an entity's PersistentDataContainer. The entity's PDC is the
 * single source of truth (it travels with the entity through chunk unload, restart, and spawn-egg
 * capture), so there is no in-memory state to flush.
 */
public final class GolemStore {

    private final Keys keys;

    public GolemStore(Keys keys) {
        this.keys = keys;
    }

    public boolean isManaged(PersistentDataHolder holder) {
        return holder.getPersistentDataContainer().has(keys.managed, PersistentDataType.BYTE);
    }

    public GolemState read(PersistentDataHolder holder) {
        GolemState s = holder.getPersistentDataContainer().get(keys.state, GolemStateType.INSTANCE);
        return s == null ? GolemState.empty() : s;
    }

    /** Persist state and mark the entity managed. Call after every mutation (eager write). */
    public void write(PersistentDataHolder holder, GolemState state) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        pdc.set(keys.state, GolemStateType.INSTANCE, state);
        pdc.set(keys.managed, PersistentDataType.BYTE, (byte) 1);
    }

    /** Forget this golem entirely (un-manage). */
    public void clear(PersistentDataHolder holder) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        pdc.remove(keys.state);
        pdc.remove(keys.managed);
    }
}
