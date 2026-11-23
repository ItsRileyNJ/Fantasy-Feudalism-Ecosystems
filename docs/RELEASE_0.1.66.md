# November 23, 2026 — Version 0.1.66 — Changelog

- **Crystapids:** Lava Scorpions now have an amethyst appearance and spawn in underground geodes or on amethyst blocks. They no longer hurt players on contact.
- **Scorpion movement:** Increased the smooth tail sway while walking.
- **Large Biomes:** Verified biome spawn entries and native Crystapid spawning in a fresh Large Biomes world.

Crystapids retain quarter size, fire immunity, player passivity and native groups of 3–5. Their cave habitat accepts amethyst or budding-amethyst floors directly; other valid ground requires an amethyst shell in at least two directions and nearby calcite. The former lava proximity and Y<0 restrictions are removed, allowing above-zero underground geodes. Geodes are generated features rather than tracked structures, so enclosed crystal/calcite geometry identifies their interiors. Covered, sufficiently underground positions and native ground, collision, difficulty, cap and player-distance rules still apply. Crystal light does not prevent spawning.

Display names, egg colors, textures and subtitles now use Crystapid. The saved entity and egg registry IDs remain `fantasy_feudalism_mobs:lava_scorpion` and `fantasy_feudalism_mobs:lava_scorpion_spawn_egg`, preserving existing mobs, eggs and external references.

All 26 focused scorpion/Crystapid and arthropod water-avoidance GameTests passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4. They cover habitat acceptance/rejection, above-zero caves, native amethyst/geode spawning, fire immunity, player passivity, repeated contact without damage, save/reload, eggs, normal scorpion combat and water escape. The spawn fixture filters competing species and isolates population caps to exercise the native category-spawn pipeline; it does not measure ordinary encounter frequency. Unrelated gameplay suites were not rerun.

A separate dedicated server created a fresh `minecraft:large_biomes` world with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The harness verified the actual noise settings, 47 expected spawn entries across 19 relevant surface/cave biomes, and retained vanilla geode generation. Native category spawning produced four Crystapids on an amethyst cave fixture in that world, within 15 seeded attempts. That fixture also isolates competing species and caps. The source audit found biome tags/local temperature, weather, height, village and habitat checks rather than assumptions about biome size; no generation override was needed. These checks establish support for the vanilla Large Biomes preset, not arbitrary world-generation replacements.

Native client rendering confirms the new purple/gray appearance and both scales. Walking yaw/root roll increase from 0.035 to 0.08 radians and bob from 0.018 to 0.035; the slow shared clock remains. Observed unscaled tip travel reached about 0.20 blocks versus the preceding 0.084–0.112 range. Attack-tail yaw remains zero during the controlled stroke; the stinger reaches past the head. All observed transforms were finite and no render errors were recorded.

[Walking preview](scorpion-0.1.66/walking.gif) · [Crystapid closeup](scorpion-0.1.66/closeup.png) · [Scale comparison](scorpion-0.1.66/comparison.png)

[Focused tests](scorpion-0.1.66/focused-tests.txt) · [Large Biomes audit](scorpion-0.1.66/large-biomes-audit.json) · [Native spawning](scorpion-0.1.66/large-biomes-spawning.json) · [Render observations](scorpion-0.1.66/render-observations.json)

Release and multiplayer-harness builds passed. Test classes are excluded from the release JAR. The user authorized committing and pushing this release.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.66.jar`

SHA-256: `ddef583f0a0cabf0ecffe09240caa203e98134cdb7447c64a274ca1f1044384c`
