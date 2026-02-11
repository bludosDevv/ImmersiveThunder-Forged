package leah.leahs_immersive_thunder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ImmersiveThunderClient.MOD_ID)
public class ImmersiveThunderClient {
    public static final String MOD_ID = "immersivethunder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Create DeferredRegister for sounds
    public static final DeferredRegister<SoundEvent> SOUNDS = 
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);

    // Register sound events
    public static final RegistryObject<SoundEvent> ENTITY_LIGHTNING_BOLT_THUNDER_CLOSE = 
        SOUNDS.register("thunder_close", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(MOD_ID, "thunder_close")));

    public static final RegistryObject<SoundEvent> ENTITY_LIGHTNING_BOLT_THUNDER_MEDIUM = 
        SOUNDS.register("thunder_medium", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(MOD_ID, "thunder_medium")));

    public static final RegistryObject<SoundEvent> ENTITY_LIGHTNING_BOLT_THUNDER_FAR = 
        SOUNDS.register("thunder_far", () -> SoundEvent.createVariableRangeEvent(
            new ResourceLocation(MOD_ID, "thunder_far")));

    public ImmersiveThunderClient() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register sounds
        SOUNDS.register(modEventBus);
        
        LOGGER.info("Immersive Thunder loaded.");
    }
}
