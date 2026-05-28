package com.wearemachina.workorders.config;

/**
 * Holds the current {@link PluginConfig} behind a volatile reference so {@code /workorders reload}
 * can swap the whole snapshot atomically. Services depend on this holder and call {@link #get()} when
 * they need a value, rather than caching config across reloads.
 */
public final class ConfigHolder {

    private volatile PluginConfig config;

    public ConfigHolder(PluginConfig config) {
        this.config = config;
    }

    public PluginConfig get() {
        return config;
    }

    public void set(PluginConfig config) {
        this.config = config;
    }
}
