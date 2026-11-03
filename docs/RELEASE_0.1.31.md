# November 2, 2026 — Version 0.1.31 — Changelog

- **Rain growth:** Rain speeds up exposed crop growth by 15%.
- **Creeper explosions:** Nearby crops grow by up to two stages. Air spaces stay untouched.

## Behavior

Rain only helps crops actually exposed to it. Roofs and rainless biomes retain ordinary growth. Native light, soil, space, maturity and random growth checks still apply, including other mods' growth-permission hooks. The bonus works during existing random ticks rather than scanning farms.

For random growth gates with a native bound of `n`, the winning chance increases from `1/n` to `1.15/n`. Only a small fraction of failed native rolls become successes. Beetroot and torchflower keep their separate native outer gates. Cane and cactus receive a 15% chance of one extra native age tick, guarded against recursion and subject to native height and obstruction checks.

Creeper explosions add up to two stages to existing supported crops, capped at maturity and limited to the explosion's radius. They retain properties such as cocoa facing. Growth respects mobGriefing and entity-griefing permissions; the moss-conversion toggle only controls moss conversion.

Supported rain crops are wheat, carrots, potatoes, beetroot, torchflowers, pitchers, melon/pumpkin stems, nether wart, cocoa, sweet berries, cane and cactus. CropBlock subclasses using native growth gates inherit rain behavior. Explosion growth additionally recognizes tagged crops with integer age properties.

To preserve air, explosions do not place fruit, extend cane/cactus columns or create a missing pitcher upper half. A single-block pitcher stops at age two; an existing two-block pitcher updates both halves consistently. Native rain-driven growth may still form new blocks normally when a crop's own growth rules allow it.

## Validation

All **243 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed. Ten new tests cover 30,000 dry and 30,000 wet native wheat growth attempts; exact dry sequences under a glass roof and in a rainless desert; growth denial and maturity; native subtype gates; extra cane/cactus age ticks and recursion bounds; two-stage explosion growth and capping; radius and air preservation; moss-toggle independence; mobGriefing; pitcher halves and cocoa facing.

Final automated log: `run/compatibility-k8mns825/ecosystems.log`. Release and optional multiplayer test-harness builds passed. This run does not claim a full rerun of Mercenaries' own suite or arbitrary modpack coverage.


Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.31.jar`

SHA-256: `fcc2d31d5d663248ce7ba9be17861f44a39311a435003e4c4adfe0c5e6a1091c`
