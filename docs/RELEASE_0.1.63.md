# November 23, 2026 — Version 0.1.63 — Changelog

- **Walking tails:** Both scorpions now have a small, visible whole-tail sway and gentle bob while walking.

The slow shared animation clock remains unchanged. A two-degree root lean reinforces the arched tail's movement rather than letting its joint rotations cancel the visible sway; walking pitch increases from 0.012 to 0.018 radians. The tail remains attached and the existing attack/feeding envelope suppresses this secondary movement.

Release and multiplayer-harness builds passed. A graphical client on the dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi observed both variants. The native probe recorded approximately 0.05 radians of root-roll variation and 0.084–0.112 blocks of lateral stinger-tip movement in unscaled model space. The Lava Scorpion renders that movement at quarter scale. All eight legs were present, transforms stayed finite and no render errors were reported. Test classes are excluded from the release JAR. Unrelated gameplay suites were not repeated for this visual edit.

[Walking preview](scorpion-0.1.63/walking.gif) · [Native observations](scorpion-0.1.63/render-results.json)

Normal scorpion habitat can be found with `/locate biome minecraft:badlands`. Eroded badlands and wooded badlands are also eligible.

No commit or push was made.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.63.jar`

SHA-256: `9b9c3e6f7b0b23c9b23c7e8e6dfd80d310c61314e41bdeef11228545a917ae62`
