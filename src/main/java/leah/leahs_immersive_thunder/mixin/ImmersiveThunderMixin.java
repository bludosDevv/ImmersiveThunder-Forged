package leah.leahs_immersive_thunder.mixin;

import leah.leahs_immersive_thunder.ImmersiveThunderClient;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Realistic thunder replacement mixin for LightningBolt.
 *
 * Simulates real-world thunder acoustics:
 * ┌────────────────────┬──────────────────┬────────────────────┬─────────────────────┐
 * │ Category           │ Distance (blocks)│ Sound Character   │ Delay Simulation     │
 * ├────────────────────┼──────────────────┼────────────────────┼─────────────────────┤
 * │ Close Thunder      │ 0 - 64           │ Sharp crack       │ Immediate            │
 * │ Medium Thunder     │ 64 - 192         │ Rolling rumble    │ Speed-of-sound delay │
 * │ Far Thunder        │ 192+             │ Deep distant boom │ Notable delay        │
 * └────────────────────┴──────────────────┴────────────────────┴─────────────────────┘
 *
 * Additional realistic features:
 * - Pitch variation scales with distance (close = sharp, far = deep)
 * - Impact sound volume scales with proximity
 * - Smooth transitions with boundary randomization
 * - Cancels vanilla impact sounds to prevent duplication
 */
@Mixin(LightningBolt.class)
public class ImmersiveThunderMixin {

    // ── Distance Thresholds (in blocks / meters) ──────────────────────────
    /** Maximum distance for "close" thunder category */
    @Unique
    private static final double CLOSE_DISTANCE = 64.0;
    /** Maximum distance for "medium" thunder category */
    @Unique
    private static final double MEDIUM_DISTANCE = 192.0;
    /** Maximum distance to play the sharp impact crack sound */
    @Unique
    private static final double IMPACT_MAX_DISTANCE = 128.0;
    /** Random offset range (blocks) to blend between categories */
    @Unique
    private static final double BLEND_RANGE = 16.0;

    // ── Volume Settings ───────────────────────────────────────────────────
    // Minecraft sound range = 16 * volume. These ensure the sound carries
    // across the entire range of its distance category.
    @Unique
    private static final float CLOSE_VOLUME = 5.0f;      // 80 block range
    @Unique
    private static final float MEDIUM_VOLUME = 14.0f;    // 224 block range
    @Unique
    private static final float FAR_VOLUME = 24.0f;       // 384 block range
    @Unique
    private static final float IMPACT_VOLUME = 2.0f;

    /**
     * Redirects ALL {@code Level.playLocalSound} calls within
     * {@code LightningBolt.tick()}.
     *
     * Vanilla LightningBolt.tick() issues two playLocalSound calls at life == 2:
     *  1. SoundEvents.LIGHTNING_BOLT_THUNDER (global range, 10000.0F vol)
     *  2. SoundEvents.LIGHTNING_BOLT_IMPACT (local, 2.0F vol)
     *
     * Because @Redirect replaces every matching INVOKE, both calls pass
     * through here. We handle ONLY the thunder call; impact calls are silently
     * suppressed (our own impact logic runs at the appropriate distance).
     */
    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
        )
    )
    private void immersivethunder$redirectPlayLocalSound(
            Level level, double x, double y, double z,
            SoundEvent sound, SoundSource category,
            float volume, float pitch, boolean distanceDelay) {

        LightningBolt bolt = (LightningBolt) (Object) this;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Only handle the thunder sound event — silently cancel impact events
        // since we play our own impact sound at appropriate distances below.
        if (sound != SoundEvents.LIGHTNING_BOLT_THUNDER) return;

        double distance = player.distanceTo(bolt);

        // Determine distance category with boundary blending
        SoundEvent soundEvent;
        float categoryVolume;
        boolean useDistanceDelay;

        if (distance <= CLOSE_DISTANCE - BLEND_RANGE) {
            // Solidly close
            soundEvent = ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_CLOSE.get();
            categoryVolume = CLOSE_VOLUME;
            useDistanceDelay = false;
        } else if (distance <= CLOSE_DISTANCE + BLEND_RANGE) {
            // Blend zone: close → medium
            // Randomize which sound we play near the boundary to prevent
            // jarring transitions when the player moves.
            boolean playClose = level.random.nextBoolean();
            soundEvent = playClose
                ? ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_CLOSE.get()
                : ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_MEDIUM.get();
            categoryVolume = playClose ? CLOSE_VOLUME : MEDIUM_VOLUME;
            useDistanceDelay = !playClose;
        } else if (distance <= MEDIUM_DISTANCE - BLEND_RANGE) {
            // Solidly medium
            soundEvent = ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_MEDIUM.get();
            categoryVolume = MEDIUM_VOLUME;
            useDistanceDelay = true;
        } else if (distance <= MEDIUM_DISTANCE + BLEND_RANGE) {
            // Blend zone: medium → far
            boolean playMedium = level.random.nextBoolean();
            soundEvent = playMedium
                ? ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_MEDIUM.get()
                : ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_FAR.get();
            categoryVolume = playMedium ? MEDIUM_VOLUME : FAR_VOLUME;
            useDistanceDelay = true;
        } else {
            // Solidly far
            soundEvent = ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_FAR.get();
            categoryVolume = FAR_VOLUME;
            useDistanceDelay = true;
        }

        playThunderSound(level, bolt, soundEvent, categoryVolume, useDistanceDelay, distance);
    }

    /**
     * Plays the thunder sound with realistic pitch modulation and optionally
     * the impact crack sound for close-to-medium strikes.
     *
     * Realistic pitch behaviour:
     * ───────────────────────────
     * Real thunder loses high frequencies over distance (atmospheric absorption).
     * We simulate this by lowering and tightening the pitch range as distance
     * increases:
     *
     *   Close  → 0.85 – 1.15  (sharp, crackling, wide variation)
     *   Medium → 0.75 – 1.0   (muted highs, moderate variation)
     *   Far    → 0.65 – 0.85  (deep rumble, narrow variation)
     *
     * Distance delay:
     * ───────────────
     * Speed of sound ≈ 343 m/s → ~3 seconds / km.
     * Minecraft uses this natively when distanceDelay=true in playLocalSound,
     * adding a delay proportional to distance from the player.
     */
    @Unique
    private void playThunderSound(
            Level level, LightningBolt bolt,
            SoundEvent soundEvent, float categoryVolume,
            boolean useDistanceDelay, double distance) {

        // ── Realistic pitch calculation ──
        float pitch;
        if (distance <= CLOSE_DISTANCE) {
            // Close: sharp, crackling sound with wide pitch variation
            pitch = 0.85f + level.random.nextFloat() * 0.3f;
        } else if (distance <= MEDIUM_DISTANCE) {
            // Medium: moderate rumble with some high-frequency loss
            pitch = 0.75f + level.random.nextFloat() * 0.25f;
        } else {
            // Far: deep, low rumble — minimal variation
            pitch = 0.65f + level.random.nextFloat() * 0.2f;
        }

        // ── Play the thunder sound ──
        level.playLocalSound(
            bolt.getX(), bolt.getY(), bolt.getZ(),
            soundEvent, SoundSource.WEATHER,
            categoryVolume, pitch, useDistanceDelay
        );

        // ── Impact sound (closer = louder, farther = quieter / silent) ──
        if (distance <= IMPACT_MAX_DISTANCE) {
            // Scale impact volume inversely with distance
            float proximityFactor = (float) (1.0 - (distance / IMPACT_MAX_DISTANCE) * 0.6);
            float scaledVolume = IMPACT_VOLUME * Math.max(0.4f, proximityFactor);
            float impactPitch = 0.5f + level.random.nextFloat() * 0.2f;

            level.playLocalSound(
                bolt.getX(), bolt.getY(), bolt.getZ(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER,
                scaledVolume, impactPitch, false
            );
        }
    }
}
