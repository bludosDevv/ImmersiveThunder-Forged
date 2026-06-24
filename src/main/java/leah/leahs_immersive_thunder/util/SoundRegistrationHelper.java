package leah.leahs_immersive_thunder.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Cross-version SoundEvent utility that works across Minecraft 1.18.x - 1.20.1.
 * 
 * Minecraft changed how SoundEvent is created between versions:
 * - 1.18.x: public constructor new SoundEvent(ResourceLocation)
 * - 1.19.x: transitional (constructor still available in 1.19.2)
 * - 1.19.3+: factory method SoundEvent.createVariableRangeEvent(ResourceLocation)
 * - 1.20.1: factory method only (constructor made private)
 * 
 * This helper uses reflection to detect the available API at runtime.
 */
public class SoundRegistrationHelper {
    
    private static final Method CREATE_VARIABLE_RANGE_METHOD;
    private static final Constructor<SoundEvent> SOUND_EVENT_CONSTRUCTOR;
    
    static {
        Method factoryMethod = null;
        Constructor<SoundEvent> constructor = null;
        
        // Try the 1.19.3+ factory method first
        try {
            factoryMethod = SoundEvent.class.getMethod("createVariableRangeEvent", ResourceLocation.class);
        } catch (NoSuchMethodException e) {
            // Fall through
        }        if (factoryMethod == null) {
            // Try the 1.18.x / 1.19.x constructor (may be deprecated/package-private in some builds)
            try {
                Constructor<SoundEvent> ctor = SoundEvent.class.getDeclaredConstructor(ResourceLocation.class);
                ctor.setAccessible(true);
                constructor = ctor;
            } catch (NoSuchMethodException e) {
                // Try the float-arg constructor as last resort
                try {
                    Constructor<SoundEvent> ctor = SoundEvent.class.getDeclaredConstructor(ResourceLocation.class, float.class);
                    ctor.setAccessible(true);
                    constructor = ctor;
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException("Cannot find any SoundEvent creation method on this Minecraft version", ex);
                }
            }
        }
        
        CREATE_VARIABLE_RANGE_METHOD = factoryMethod;
        SOUND_EVENT_CONSTRUCTOR = constructor;
    }
    
    /**
     * Creates a variable-range SoundEvent compatible with the current Minecraft version.
     * Uses {@code createVariableRangeEvent} on 1.19.3+, falls back to constructor on 1.18.x/1.19.2.
     */
    public static SoundEvent createSoundEvent(ResourceLocation location) {
        if (CREATE_VARIABLE_RANGE_METHOD != null) {
            try {
                return (SoundEvent) CREATE_VARIABLE_RANGE_METHOD.invoke(null, location);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SoundEvent via createVariableRangeEvent", e);
            }
        }
        
        if (SOUND_EVENT_CONSTRUCTOR != null) {
            try {
                return SOUND_EVENT_CONSTRUCTOR.newInstance(location);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create SoundEvent via constructor", e);
            }
        }
        
        // Last resort: constructor with float range
        try {
            Constructor<SoundEvent> floatCtor = SoundEvent.class.getConstructor(ResourceLocation.class, float.class);
            return floatCtor.newInstance(location, Float.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException("All SoundEvent creation methods failed", e);
        }
    }
}
