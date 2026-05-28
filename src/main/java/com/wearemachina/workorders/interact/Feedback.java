package com.wearemachina.workorders.interact;

import com.wearemachina.workorders.config.ConfigHolder;
import com.wearemachina.workorders.config.FeedbackIntensity;
import com.wearemachina.workorders.config.PluginConfig;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Centralised diegetic feedback: action-bar lines (via {@link com.wearemachina.workorders.config.Messages}),
 * sounds, and particles. Everything respects the configured {@code feedback.intensity} and toggles, so the
 * whole plugin can be made silent. This is the only place player-facing copy and FX are emitted.
 */
public final class Feedback {

    private final ConfigHolder cfg;

    public Feedback(ConfigHolder cfg) {
        this.cfg = cfg;
    }

    private PluginConfig c() {
        return cfg.get();
    }

    // ----- action bar ---------------------------------------------------------------------------

    public void actionBar(Player player, String key, String... placeholders) {
        if (player == null || !c().actionbar) {
            return;
        }
        player.sendActionBar(c().messages.render(key, placeholders));
    }

    // ----- raw primitives -----------------------------------------------------------------------

    public void sound(Entity at, Sound sound, float volume, float pitch) {
        if (at == null || !c().sounds) {
            return;
        }
        at.getWorld().playSound(at, sound, volume, pitch);
    }

    public void soundTo(Player player, Sound sound, float volume, float pitch) {
        if (player == null || !c().sounds) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void particles(Location centre, Particle particle, int baseCount, double spread) {
        if (centre == null) {
            return;
        }
        World w = centre.getWorld();
        int n = c().intensity.particles(baseCount);
        if (w == null || n <= 0) {
            return;
        }
        w.spawnParticle(particle, centre, n, spread, spread, spread, 0.0);
    }

    /** A short particle line between two points (e.g. golem → bound container). */
    public void sparkLine(Location from, Location to) {
        if (from == null || to == null || c().intensity == FeedbackIntensity.OFF) {
            return;
        }
        World w = from.getWorld();
        if (w == null || !w.equals(to.getWorld())) {
            return;
        }
        Vector delta = to.toVector().subtract(from.toVector());
        double len = delta.length();
        if (len < 0.01) {
            return;
        }
        Vector step = delta.normalize();
        int points = Math.min(24, Math.max(2, (int) Math.ceil(len)));
        for (int i = 0; i <= points; i++) {
            Location p = from.clone().add(step.clone().multiply(len * i / points));
            w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    // ----- composite reactions (used by interaction + care) -------------------------------------

    private static Location head(LivingEntity golem) {
        return golem.getLocation().add(0.0, 0.7, 0.0);
    }

    private static float wobble() {
        return 0.9f + ThreadLocalRandom.current().nextFloat() * 0.4f;
    }

    public void happy(LivingEntity golem) {
        particles(head(golem), Particle.HAPPY_VILLAGER, 6, 0.3);
        sound(golem, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, wobble() + 0.2f);
    }

    public void reject(LivingEntity golem) {
        particles(head(golem), Particle.ANGRY_VILLAGER, 3, 0.2);
        sound(golem, Sound.BLOCK_COPPER_BULB_TURN_OFF, 0.6f, 0.7f);
    }

    public void tapBlock(Location block) {
        Location c = block.clone().add(0.5, 0.6, 0.5);
        particles(c, Particle.ELECTRIC_SPARK, 6, 0.2);
        if (block.getWorld() != null && c().sounds) {
            block.getWorld().playSound(c, Sound.BLOCK_COPPER_HIT, 0.7f, 1.3f);
        }
    }

    public void routeConfirmed(LivingEntity golem, Location from, Location to) {
        sparkLine(from, to);
        particles(to.clone().add(0.5, 0.6, 0.5), Particle.HAPPY_VILLAGER, 6, 0.25);
        sound(golem, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
    }

    public void waxed(LivingEntity golem) {
        particles(head(golem), Particle.WAX_ON, 8, 0.35);
        sound(golem, Sound.BLOCK_COPPER_BULB_TURN_ON, 0.7f, 1.1f);
    }

    public void whistlePing(LivingEntity golem) {
        particles(head(golem), Particle.ENCHANTED_HIT, 5, 0.3);
        sound(golem, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.7f);
    }

    public void carryTrail(LivingEntity golem) {
        particles(golem.getLocation().add(0.0, 0.4, 0.0), Particle.ELECTRIC_SPARK, 2, 0.15);
    }

    public void sluggish(LivingEntity golem) {
        particles(head(golem), Particle.SMOKE, 3, 0.2);
        sound(golem, Sound.BLOCK_COPPER_STEP, 0.4f, 0.6f);
    }

    public void scrapedClean(LivingEntity golem) {
        particles(head(golem), Particle.HEART, 5, 0.3);
        sound(golem, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.4f);
    }

    public void greetOwner(LivingEntity golem) {
        particles(head(golem), Particle.HAPPY_VILLAGER, 1, 0.1);
        sound(golem, Sound.BLOCK_COPPER_BULB_TURN_ON, 0.3f, 1.6f);
    }

    public void stuck(LivingEntity golem) {
        particles(head(golem), Particle.ANGRY_VILLAGER, 1, 0.1);
        sound(golem, Sound.BLOCK_COPPER_FALL, 0.5f, 0.6f);
    }

    public void bringsFlower(LivingEntity golem) {
        particles(head(golem), Particle.HEART, 4, 0.3);
        particles(head(golem), Particle.HAPPY_VILLAGER, 4, 0.3);
        sound(golem, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.7f);
    }
}
