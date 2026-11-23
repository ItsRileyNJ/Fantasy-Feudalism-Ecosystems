# November 22, 2026 — Version 0.1.47 — Changelog

- **Root Worms:** Added the supplied mob, with a spawn egg and natural spawning in all savanna biomes.
- **Worm behavior:** They listen for nearby activity, rear up to investigate and bite back when attacked. Fire and lava are especially dangerous to them.
- **Animations:** Added native movement, listening, blinking and bite animations, with matching body hitboxes. Listening now keeps the rear five segments grounded while only the front four rise and look around. No GeckoLib required.

## Validation

- Release build passed. All **24 focused Root Worm GameTests** passed with Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 installed. These cover hearing, search poses, neutral retaliation, attack range/cooldowns/obstructions, fire behavior, multipart damage and lifecycle, spawn egg and all three savanna biome lists.
- After shortening the listening arch, all **five focused listening-pose GameTests** passed with Mercenaries installed. They verify grounded rear segments, connected spacing and hitboxes, raised-head targeting, scan synchronization, transitions and combat interruption. The release build passed again.
- Before that pose refinement, a dedicated NeoForge 21.1.262 server and two graphical clients loaded the same companion mods. Both clients rendered the supplied texture and observed blinking, raised searching, opening jaws and movement. Render probes observed finite transforms, fully raised search amount, jaw opening above 0.5 radians and movement along the ground. Both resource reloads completed without render errors.
- After saving and restarting the server and both clients, the same single worm retained 16 health. All nine parts used consecutive IDs and appeared in the native multipart registry on server and both clients. [Saved observations](rootworm-0.1.47/rejoin-results.json) and [screenshots of the original taller pose](rootworm-0.1.47/owner-search.png) are included.
- Resource JSON, release exclusions, renderer/mixin inclusion and unchanged supplied texture bytes were checked. Test code and test arenas are excluded from the release JAR.
- Natural spawning uses weight 6, groups 2–4, native creature population limits and native ground-spawn conditions in Savanna, Savanna Plateau and Windswept Savanna. Biome-list tests verify registration; they do not establish a guaranteed encounter frequency.
- Multiplayer used a disposable offline-mode local server. This is a focused compatibility check, not a 150-mod performance benchmark or online authentication test.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.47.jar`

SHA-256: `0b4ba5f43d85712f0e2621e89459ff9746445cbb35d24d69babc50fb2824efb3`

Install the same version on the server and clients.
