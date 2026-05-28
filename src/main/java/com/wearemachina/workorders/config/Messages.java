package com.wearemachina.workorders.config;

import java.io.File;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads player-facing strings from {@code messages.yml} (editable by server owners, so the plugin is
 * fully re-brandable and translatable). Strings are MiniMessage; our own placeholders use {@code {braces}}
 * to avoid clashing with MiniMessage's {@code <tags>}.
 */
public final class Messages {

    private final FileConfiguration cfg;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Component prefix;

    private Messages(FileConfiguration cfg) {
        this.cfg = cfg;
        String p = cfg.getString("prefix", "");
        this.prefix = (p == null || p.isEmpty()) ? Component.empty() : mm.deserialize(p);
    }

    public static Messages load(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        return new Messages(YamlConfiguration.loadConfiguration(f));
    }

    /** Render a message key with {@code {placeholder}} substitutions (no prefix). */
    public Component render(String key, String... kv) {
        String raw = cfg.getString(key);
        if (raw == null) {
            raw = key;
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            raw = raw.replace("{" + kv[i] + "}", kv[i + 1]);
        }
        return mm.deserialize(raw);
    }

    /** Render with the configured prefix prepended (for chat lines, not action bars). */
    public Component prefixed(String key, String... kv) {
        return prefix.append(render(key, kv));
    }

    public String raw(String key, String def) {
        return cfg.getString(key, def);
    }
}
