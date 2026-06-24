# Changelog

## Version 1.3.0 — Realistic Thunder Overhaul

### New Realistic Thunder System

The thunder system has been completely reworked to simulate real-world thunder acoustics based on physics principles:

#### Distance-Based Sound Categories (Refined)

| Category | Distance (blocks) | Sound Character | Real-World Analogy |
|----------|------------------|-----------------|-------------------|
| **Close** | 0 – 64 | Sharp, loud crack with immediate attack | Nearby lightning strike (~200m) |
| **Medium** | 64 – 192 | Rolling rumble with moderate depth | Strike several blocks away (~600m) |
| **Far** | 192+ | Deep, distant booming rumble | Strike far in the distance (600m+) |

#### Realistic Pitch Modulation
Pitch variation now scales naturally with distance to simulate **atmospheric high-frequency absorption**:
- **Close (0–64 blocks):** Pitch range 0.85–1.15 — wide variation creates sharp, crackling transients
- **Medium (64–192 blocks):** Pitch range 0.75–1.0 — moderate variation with some high-frequency loss
- **Far (192+ blocks):** Pitch range 0.65–0.85 — narrow, deep rumble mimicking low-pass atmospheric filtering

#### Speed of Sound Simulation
- **`distanceDelay=true`** is now enabled for medium and far categories, causing Minecraft's native sound engine to apply a delay proportional to distance (≈1 second per 100 blocks, simulating 343 m/s)
- Close thunder plays **immediately** (`distanceDelay=false`) for that instantaneous crack

#### Smooth Category Transitions
- **16-block blend zones** at category boundaries (48–80 blocks, 176–208 blocks)
- Random sound selection within blend zones prevents jarring audible transitions when the player walks through a boundary
- No more sudden switches between sound sets

#### Distance-Scaled Impact Sounds
- Impact crack sound now scales in volume based on proximity:
  - **Very close (0–16 blocks):** Full impact volume (2.0)
  - **Medium (64 blocks):** ~70% volume
  - **Far (120 blocks):** ~40% volume
  - **128+ blocks:** No impact sound (realistic — you wouldn't hear the crack beyond this distance)

#### Volume Ranges Optimized
Fixed absurd volume values (10000.0f → reasonable values):
- **Close:** 5.0f (80-block range — fills the full close category)
- **Medium:** 14.0f (224-block range — covers medium category)
- **Far:** 24.0f (384-block range — ensures far sounds carry)
- **Impact:** 2.0f (scaled by proximity)

### Bug Fixes

#### Fixed: Dual Impact Sound Bug
**Before:** The mixin's `@Redirect` intercepted ALL `playLocalSound` calls in `LightningBolt.tick()`. Vanilla calls this method twice (once for thunder, once for impact), causing impact to play **twice** — once from each redirect.

**After:** The mixin now checks `SoundEvent` identity. It only handles `SoundEvents.LIGHTNING_BOLT_THUNDER` calls; impact calls are silently suppressed since our own proper impact logic runs at the correct distance.

#### Removed: Duplicate Namespace
- **Deleted** entire `assets/leahs-immersive-thunder/` directory (duplicate sounds, sounds.json, lang files)
- **Deleted** `leahs-immersive-thunder.mixins.json` (duplicate mixin config loading the same mixin)
- All assets consolidated under single `immersivethunder` namespace

#### Removed: Unused ThunderSoundInterface
- Deleted `util/ThunderSoundInterface.java` — the mixin no longer implements this interface since the `playThunderSound` method was refactored into a private mixin method

#### Enabled: Sound Streaming
- All thunder sounds now use `"stream": true` in `sounds.json`
- This streams OGG files from disk rather than loading them entirely into memory, which is essential for several-second-long thunder audio clips

### Multi-Version Build System

#### New: Gradle Build Matrix (1.18.2 – 1.20.1)

The mod now officially supports **Minecraft Forge 1.18.2, 1.19.2, and 1.20.1** through a unified build system:

| Minecraft | Forge | Parchment Mappings |
|-----------|-------|--------------------|
| 1.20.1 | 47.3.0 | 2023.09.03 |
| 1.19.2 | 43.5.0 | 2022.08.14 |
| 1.18.2 | 40.3.0 | 2022.08.07 |

#### Version-Specific Properties Files
- `gradle-mc1.20.1.properties` — Build config for 1.20.1
- `gradle-mc1.19.2.properties` — Build config for 1.19.2
- `gradle-mc1.18.2.properties` — Build config for 1.18.2

#### Cross-Version SoundEvent Compatibility
- **New `SoundRegistrationHelper` class** uses reflection to detect the available `SoundEvent` API at runtime
- Automatically selects `SoundEvent.createVariableRangeEvent()` (1.19.3+) or `new SoundEvent()` constructor (1.18.x/1.19.2)
- No code changes needed between versions — same JAR logic works everywhere



### Expected Experience

With ImmersiveThunder 1.3.0, thunderstorms feel dramatically more realistic:

1. **Lightning strikes at close range** (within ~64 blocks): You hear a sharp, loud crack that hits instantly — like a whip crack nearby. The impact sound is powerful and clear.

2. **Medium-range strikes** (64–192 blocks): A rolling rumble arrives with a slight delay after the flash. The sound has less high-frequency content, mimicking real atmospheric absorption. The impact is still audible but softer.

3. **Distant storms** (192+ blocks): Deep, low-frequency rumbles that roll across the landscape. There's a noticeable delay between flash and thunder. The impact crack is gone — just pure, distant thunder.

4. **Moving through a storm**: As you walk toward or away from strike locations, category transitions are smooth with randomized blending at boundaries — no jarring sound switches.

5. **Performance**: Sound streaming means long audio clips don't consume excessive memory. The mod remains lightweight and client-side only.
