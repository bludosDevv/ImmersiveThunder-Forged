package leah.leahs_immersive_thunder.mixin;

import leah.leahs_immersive_thunder.ImmersiveThunderClient;
import leah.leahs_immersive_thunder.util.ThunderSoundInterface;
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

import java.util.Objects;

@Mixin(LightningBolt.class)
public class ImmersiveThunderMixin implements ThunderSoundInterface {
    
    @Unique
    private static final double closeDistance = 64.0;
    @Unique
    private static final double mediumDistance = 192.0;
    @Unique
    private static final float thunderCloseVolume = 10000.0f;
    @Unique
    private static final float thunderMediumVolume = 10000.0f;
    @Unique
    private static final float thunderFarVolume = 10000.0f;
    @Unique
    private static final float impactSoundVolume = 2.0f;
    @Unique
    private static final boolean impactSound = true;

    @Redirect(method = "tick", at = @At(value = "INVOKE", 
        target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void playSound(Level world, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance) {
        LightningBolt lightning = (LightningBolt) (Object) this;
        Player player = Minecraft.getInstance().player;
        
        if (player == null) return;
        
        double distanceToEntity = player.distanceTo(lightning);
        
        if (distanceToEntity <= closeDistance) {
            playThunderSound(world, lightning, ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_CLOSE.get(), thunderCloseVolume, false);
        } else if (distanceToEntity <= mediumDistance) {
            playThunderSound(world, lightning, ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_MEDIUM.get(), thunderMediumVolume, true);
        } else {
            playThunderSound(world, lightning, ImmersiveThunderClient.ENTITY_LIGHTNING_BOLT_THUNDER_FAR.get(), thunderFarVolume, true);
        }
    }

    @Override
    public void playThunderSound(Level world, LightningBolt lightning, SoundEvent soundEvent, float volume, boolean useDistance) {
        world.playLocalSound(lightning.getX(), lightning.getY(), lightning.getZ(), soundEvent, SoundSource.WEATHER, 
            volume, 0.8f + world.random.nextFloat() * 0.2f, useDistance);
        
        if (impactSound) {
            world.playLocalSound(lightning.getX(), lightning.getY(), lightning.getZ(), 
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 
                impactSoundVolume, 0.5f + world.random.nextFloat() * 0.2f, false);
        }
    }
}
