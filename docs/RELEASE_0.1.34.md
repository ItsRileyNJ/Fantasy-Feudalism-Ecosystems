# November 8, 2026 — Version 0.1.34 — Changelog

- **Forest birds:** Added robins, blue jays, goldfinches and crows using the supplied Blockbench models, with matching colors and spawn eggs.
- **Natural spawning:** Robins and goldfinches spawn in flocks of 3–6 in temperate forests. Blue jays spawn alone in those forests, while crows spawn alone in spruce forests.
- **Bird movement:** Birds flap their wings, look around quickly and take short walks after landing. They prefer flying to new areas and stay within 15 blocks of ground level, ignoring trees.
- **Fleeing:** Birds fly away when Survival or Adventure players approach within four blocks. Creative and Spectator players do not frighten them.

## Validation

All **256 required native GameTests passed with Mercenaries 1.15.7 installed**. The 13 new bird tests cover spawn eggs, biome/group rules, player modes, saving, flight and landing, short walks, quick looks, fall safety and the terrain-relative flight ceiling. Full log: `run/compatibility-0e5vibke/ecosystems.log`.

Six additional checks passed on a dedicated server with Mercenaries and two graphical clients. Both clients rendered every species, observed wing flaps, head turns and walking legs, and reloaded resources without renderer errors. A nearby Survival player triggered takeoff; sampled flight stayed below the ceiling; the dedicated server saved successfully. Results and screenshots: [multiplayer evidence](birds-0.1.34/multiplayer-results.json), [first client](birds-0.1.34/lineup-owner.png), [second client](birds-0.1.34/lineup-guest.png).

The graphical check caught grounded birds incorrectly flapping when a client lacked a reliable ground flag. Flight animation now uses the server's synchronized flight state. The final build passed both the automated suite and the repeated graphical checks. The animation observation timeout allows several short-walk cycles, since an individual landing can skip a random walk.

Resource JSON validation, deterministic Blockbench conversion, Python syntax checks and release JAR inspection passed. The release includes all four spawn eggs, biome modifiers and bird textures, and excludes test code. Original model files are preserved unchanged in [model sources](model-sources/birds/README.md).

## Implementation and limits

Bird movement and threat decisions run on the server. Searches use bounded candidates and loaded chunks, with a per-tick terrain-column cache. Flock checks run every five seconds, and threat checks every five ticks. Natural creature caps and native spawn conditions still apply; the requested 3–6 is the configured flock range, and available space or caps can reduce an actual spawn group.

Birds currently use vanilla parrot sounds as placeholders. No breeding, taming, feeding or drops were added. The supplied files contained geometry but no textures or animation tracks; custom textures and native animations were added without another animation library.

The multiplayer session used the previously authorized disposable offline test identities. This cloud environment could not resolve Mojang authentication endpoints and had no OpenAL audio device, so online authentication and audible playback were not verified. Those environment errors were distinct from bird rendering and server behavior. This is a functional compatibility check with Mercenaries 1.15.7, not a large-modpack stress benchmark.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.34.jar`

SHA-256: `fc848a135a944dd3db2678b949e1c24a6db402229db94bbad278f31ef51e5790`
