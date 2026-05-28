package com.wearemachina.workorders.lifecycle;

import com.wearemachina.workorders.persistence.GolemStore;
import com.wearemachina.workorders.runtime.GolemRegistry;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

/**
 * Keeps {@link GolemRegistry} in sync with the world: registers managed copper golems as their chunks
 * load, and deregisters them on removal/death. Cargo safety on death is handled by the equipment
 * drop-chance set during hauling (see the haul layer); this listener only maintains the registry.
 */
public final class GolemLifecycleListener implements Listener {

    private final GolemStore store;
    private final GolemRegistry registry;

    public GolemLifecycleListener(GolemStore store, GolemRegistry registry) {
        this.store = store;
        this.registry = registry;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof CopperGolem golem && store.isManaged(golem)) {
                registry.add(golem.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event) {
        // Fires for UNLOAD, PICKUP (spawn-egg capture), PLUGIN, DEATH, etc. PDC persists with the entity,
        // so we only drop the runtime reference; re-registered on the next EntitiesLoadEvent.
        if (event.getEntity() instanceof CopperGolem golem) {
            registry.remove(golem.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof CopperGolem golem) {
            registry.remove(golem.getUniqueId());
        }
    }
}
