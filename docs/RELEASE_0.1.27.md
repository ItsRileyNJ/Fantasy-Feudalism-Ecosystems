# Version 0.1.27 — Visible sheep shivering

Fixed the renderer hook for cold sheared sheep. The previous mixin added an `isShaking(Sheep)` overload, but native `setupRotations` dispatches through `isShaking(LivingEntity)`. The original multiplayer probe called the unused overload directly, so its positive result did not establish that native rendering actually shivered. The new hook targets the exact native method; the probe now invokes that same signature.

The two-client diagnostic reproduced the problem before the fix: the synchronized cold flag and Sheep overload were true while the native renderer predicate was false. This explains the missing shiver in cold biomes and rain. The model also now adds an interpolated head, body and leg tremble at roughly three cycles per second, making the effect clearer than the native whole-entity yaw of at most about 1.26 degrees.

Snowy-taiga and actual warm-biome rain exposure still drive the server-owned cold flag. Shelter works in every biome, and cold sheep retain the slowdown and grazing restriction for 30 seconds of recovery. Model offsets reset before each render, preventing a cold sheep from leaving a pose on a warm sheep sharing the renderer. Cold causes no freezing damage.

The earlier adult grazing adjustment is included: 30 winning rolls in 1,000; lambs retain 3 in 50. The three-meal wool requirement remains unchanged.

## Validation

All **232 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed, including the new snowy-taiga regression with all eight snow-layer depths and real spruce-leaf shelter. The pre-animation run also passed this regression, so snow cover itself did not reproduce a cold-detection failure.

Final automated log: `run/compatibility-5no18obx/ecosystems.log`.

All **six two-client checks passed**: actual head/body movement in snowy taiga, normal poses on neighbouring woolled sheep, rain-triggered shivering in warm plains, 30-second recovery and full pose reset under glass, resource reload on both clients, and no leftover offsets or render errors. [Native two-client pose measurements](sheep-shivering-0.1.27-live-evidence.json) record the results. The test checks the native erased renderer predicate and samples the actual head translation and body roll across frames, in addition to the synchronized cold flag. It includes a woolled sheep alongside a cold sheep to check that shared models reset correctly.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.27.jar`

SHA-256: `0b98730c6b8c623e363ad5b83ff536b06d9b4fa341f10556501a61b68e2e57c8`

This validation uses a dedicated server and two graphical clients with Mercenaries 1.15.7. The disposable environment has no working OpenAL audio device or Minecraft authentication DNS; rendering tests use offline fixture identities. It does not claim full Mercenaries-suite or arbitrary modpack coverage.
