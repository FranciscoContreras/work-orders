package com.wearemachina.workorders;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.PluginConfig;
import com.wearemachina.workorders.gate.JobGate;
import com.wearemachina.workorders.haul.ContainerAccess;
import com.wearemachina.workorders.haul.DepositPolicy;
import com.wearemachina.workorders.interact.ActionBarService;
import com.wearemachina.workorders.interact.BindSessionManager;
import com.wearemachina.workorders.interact.Feedback;
import com.wearemachina.workorders.interact.InteractionListener;
import com.wearemachina.workorders.item.JobItems;
import com.wearemachina.workorders.lifecycle.GolemLifecycleListener;
import com.wearemachina.workorders.model.GolemState;
import com.wearemachina.workorders.persistence.GolemStore;
import com.wearemachina.workorders.persistence.Keys;
import com.wearemachina.workorders.role.RoleRegistry;
import com.wearemachina.workorders.runtime.CareService;
import com.wearemachina.workorders.runtime.GolemRegistry;
import com.wearemachina.workorders.runtime.StatusBoard;
import com.wearemachina.workorders.runtime.GolemTickService;
import com.wearemachina.workorders.ui.FilterMenu;
import com.wearemachina.workorders.ui.FilterMenuListener;
import org.bukkit.World;
import org.bukkit.entity.CopperGolem;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Work Orders — utility-AI roles for vanilla Copper Golems.
 *
 * <p>Composition root: wires every service with manual constructor injection (no static singleton) and
 * owns the plugin lifecycle.
 */
public final class WorkOrdersPlugin extends JavaPlugin {

    private ConfigHolder configHolder;
    private Keys keys;
    private GolemStore store;
    private GolemRegistry registry;
    private ActionBarService actionBar;
    private Feedback feedback;
    private GolemTickService tickService;
    private CareService careService;
    private JobItems jobItems;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configHolder = new ConfigHolder(PluginConfig.load(this));
        this.keys = new Keys(this);
        this.store = new GolemStore(keys);
        this.registry = new GolemRegistry();
        this.actionBar = new ActionBarService(this);
        this.feedback = new Feedback(configHolder, actionBar);

        BindSessionManager sessions = new BindSessionManager();
        JobGate gate = new JobGate(configHolder);
        FilterMenu filterMenu = new FilterMenu(store, configHolder);
        JobItems jobItems = this.jobItems = new JobItems(this, configHolder, keys.itemRole);
        jobItems.registerRecipes();
        RoleRegistry roles = new RoleRegistry();
        ContainerAccess containers = new ContainerAccess();
        DepositPolicy deposit = new DepositPolicy();
        StatusBoard board = new StatusBoard();

        this.tickService = new GolemTickService(
                this, registry, roles, configHolder, store, containers, deposit, feedback, board);
        this.careService = new CareService(this, registry, configHolder, store, feedback);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new GolemLifecycleListener(store, registry), this);
        pm.registerEvents(new FilterMenuListener(store, configHolder, feedback), this);
        pm.registerEvents(jobItems, this);
        pm.registerEvents(
                new InteractionListener(this, store, registry, configHolder, feedback, sessions, gate,
                        filterMenu, jobItems, board),
                this);

        WorkOrdersCommand command = new WorkOrdersCommand(this);
        var pluginCommand = getCommand("workorders");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        int found = registerLoadedGolems();
        tickService.start();
        careService.start();
        getLogger().info("Work Orders enabled — tracking " + found + " managed copper golem(s).");
    }

    @Override
    public void onDisable() {
        if (tickService != null) {
            tickService.stop();
        }
        if (careService != null) {
            careService.stop();
        }
        if (actionBar != null) {
            actionBar.shutdown();
        }
        getLogger().info("Work Orders disabled.");
    }

    /** Rebuild the config snapshot from disk, swap it in atomically, and restart the timers. */
    public void reload() {
        configHolder.set(PluginConfig.load(this));
        jobItems.registerRecipes(); // pick up edited work-order costs/model
        tickService.start();
        careService.start();
        getLogger().info("Work Orders configuration reloaded.");
    }

    /** Scan already-loaded worlds for managed golems (covers /reload and enable-while-loaded). */
    private int registerLoadedGolems() {
        int found = 0;
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof CopperGolem golem && store.isManaged(golem)) {
                    registry.add(golem.getUniqueId());
                    GolemState state = store.read(golem);
                    if (state.role() != null) {
                        feedback.applyNameplate(golem, state.role(), state.baseName());
                    }
                    found++;
                }
            }
        }
        return found;
    }

    public ConfigHolder configHolder() {
        return configHolder;
    }

    public Keys keys() {
        return keys;
    }

    public GolemStore store() {
        return store;
    }

    public GolemRegistry registry() {
        return registry;
    }

    public Feedback feedback() {
        return feedback;
    }
}
