package leah.leahs_immersive_thunder.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;

public interface ThunderSoundInterface {
    /**
     * Plays a thunder sound at the location of a lightning bolt
     * @param world The world/level
     * @param lightning The lightning bolt entity
     * @param soundEvent The sound event to play
     * @param volume Volume of the sound
     * @param useDistance Whether to use distance attenuation
     */
    void playThunderSound(Level world, LightningBolt lightning, SoundEvent soundEvent, float volume, boolean useDistance);
}
