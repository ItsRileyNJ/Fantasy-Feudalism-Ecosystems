# November 23, 2026 — Version 0.1.59 — Changelog

- **Lava Scorpion:** Added a separate quarter-sized scorpion with the same model and animations, a mottled charcoal shell, orange fissures and fiery red-orange claws with yellow highlights.
- **Separate eyes:** The Lava Scorpion's charcoal faceplate separates its orange eyes instead of forming an orange stripe.
- **Nocturnal scorpions:** Normal scorpions attack only at night and cancel attacks at dawn. Lava Scorpions remain hostile during the day.
- **Cave spawning:** Spawns rarely and singly in covered Overworld caves near lava. Includes its own spawn egg.

## Details

Lava Scorpion is a working name inferred from the confirmed habitat. Both variants use the exact same geometry builder and animation implementation. The new model layer changes material UV selection, and a dedicated Lava Scorpion atlas adds mottled charcoal plates, pits, highlights and sparse orange fissures. Uniform render scaling, shadow radius, collision box, eye height and culling margins are one-quarter of the original. The new collider is 0.35 by 0.225 blocks. Combat attributes, poisonous sting and sounds are shared with the original. The original variant retains its appearance and badlands spawn entries.

Natural spawning has weight 3, group size one, throughout Overworld biomes. Habitat checks require a covered position at least four blocks below the non-leaf surface, with lava within six blocks horizontally and three vertically. Existing any-light monster rules allow lava light while preserving difficulty and valid ground; the native spawner still handles population caps, player distances and collision/liquid checks. The bounded lava search runs only on spawn attempts and checks loaded chunks. No additional per-tick habitat scan is introduced.

## Validation

All eleven focused Scorpion GameTests passed with Mercenaries 1.15.7 installed. They cover the original variant, the new spawn egg, quarter-size collision, arthropod classification, saved variant identity and native spawn-placement rejection without lava or cave cover. The cave test waits for queued skylight updates after building/removing its roof. Release and multiplayer test-harness builds passed, with test classes excluded from the release JAR.

A graphical client on the dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi loaded both variants. [Size comparison](lava-scorpion-0.1.59/comparison.png), [new colors](lava-scorpion-0.1.59/closeup.png) and [native animation observations](lava-scorpion-0.1.59/render-results.json) are saved. Eight legs, finite model transforms, moving legs, a complete sting and opening jaws were observed without render errors. The walking fixture initially requested a path before its manually created entity had landed; repeating after landing exercised the walk normally. Unrelated suites were not rerun. The user authorized committing and pushing this release.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.59.jar`

SHA-256: `3bbc2fc4ea8d3649084bc06ee5ef732fab7487418489dc8ec3c7e06a8bf62331`


The subsequent body texture revision passed the mod build and was inspected in-game with the same dedicated-server mod set. [Updated textured body](lava-scorpion-0.1.59/textured-closeup.png). Geometry, animation and spawning were unchanged, so gameplay suites were not repeated for this asset edit.


The eye-mask material revision keeps the original geometry. Normal Scorpions use the existing world-clock NightCycle (13,000–22,999), reject daytime targets and damage, and cancel pursuit and pending sting damage at dawn. This also blocks daytime retaliation. Twelve focused Scorpion GameTests passed with Mercenaries installed, including day/night/dawn boundaries and unchanged daytime Lava Scorpion attacks.

[Latest face preview](lava-scorpion-0.1.59/separate-eyes-closeup.png), inspected on the dedicated server with a graphical client.


The final accent palette replaces amber/gold with lava red-orange and yellow highlights. The mod build passed; the dedicated-server graphical preview was inspected. [Latest lava coloring](lava-scorpion-0.1.59/fiery-closeup.png). No gameplay tests were repeated for this texture-only revision, and the user authorized committing and pushing this release.
