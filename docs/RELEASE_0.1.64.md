# November 23, 2026 — Version 0.1.64 — Changelog

- **Badlands scorpions:** Increased normal Scorpion spawn selection weight so they are easier to encounter.
- **Wooded badlands:** Normal Scorpions spawn more commonly here than in the other badlands variants.

## Findings and change

The existing entries and native placement rules successfully produced normal Scorpions on red sand and terracotta in all three vanilla badlands biomes. No terrain or registration failure was reproduced. The user also confirmed vanilla monsters spawn in the same area. Their selection weight was only 3; the reported user's world and its mob-cap occupancy are not directly accessible.

The standard entry now covers only badlands and eroded badlands, at weight 12. A separate modifier covers wooded badlands at weight 30, without a duplicate standard entry. Wooded badlands therefore has 2.5 times the selection weight, with actual encounter rates still subject to its total spawn list, valid positions and native caps. Singleton packs, normal monster darkness requirements and night-only hostility remain unchanged. Desert and other biomes remain excluded. Lava Scorpion spawning is unchanged.

## Validation

All twenty-one focused Scorpion GameTests passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. Tests verify the live biome lists, exact weights, singleton packs, absence of duplicates and ineligible biomes. A native weighted category-spawn test produces normal Scorpions on both red sand and terracotta at Y=64 in each badlands variant at night, with a registered Creative player forty blocks away. The bounded seeded test filters unrelated species and bypasses the global population cap for isolation; it verifies the native spawn pipeline and terrain eligibility, not a guaranteed encounter rate in a populated server. [Native observations](scorpion-0.1.64/native-spawn-observations.txt) · [Test result](scorpion-0.1.64/focused-tests.txt).

The native spawning test was additionally extended to grass blocks and coarse dirt in wooded badlands; the targeted run passed on both, alongside the six previous biome/ground combinations. No ground whitelist change was needed. [Additional surface test](scorpion-0.1.64/wooded-ground-tests.txt).

Release and multiplayer-harness builds passed. The release JAR contains the two exclusive biome modifiers and excludes test classes. The pending 0.1.63 walking-tail refinement is included and was previously checked in-game; rendering code was unchanged by this spawn adjustment. Unrelated suites and graphical checks were not repeated.

Find the preferred biome with `/locate biome minecraft:wooded_badlands`. Natural spawning requires non-Peaceful difficulty, mob spawning enabled, available native monster capacity, and sufficient player distance; the user confirmed night and more than twenty-four blocks away.

No commit or push was made.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.64.jar`

SHA-256: `db1b59458f85420d384ec0c89dd25a74bb96b244464a1c36e3fbd8f72e78df13`
