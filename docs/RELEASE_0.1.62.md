# November 23, 2026 — Version 0.1.62 — Changelog

- **Scorpion feelers:** Taller, thicker feelers curve gently backward and sway while walking. Each chelicera now has one fang.
- **Controlled stings:** The rear body rises and the tail sweeps forward past the head in one deliberate strike, without side-to-side tail movement during the main attack.
- **Steadier walking:** Slower, smaller tail movements on both scorpions.
- **Water avoidance:** Wild and pet spiders, cave spiders and both scorpion variants avoid water in their routes.
- **Lava Scorpions:** Deepslate-gray shells and a better chance of natural spawning near lava.

## Model and animation

Both variants share the same geometry and animation code. The feelers retain an upright base, with backward-curving middle and tip joints. Existing overlapping tapered sections stay connected; small walking movements use a shared smooth clock with delayed upper-segment motion. The chelicera side spur is removed, retaining one articulated fang per mouthpart.

Walking tail yaw is reduced to 0.035 radians at the root and pitch to 0.012, with smaller contributions down the chain. The strike hinges the abdomen around its front, carries the tail base and rear leg mounts with it, and moves the stinger about half a block beyond the unscaled model head. A smooth attack envelope suppresses secondary tail waves during both stings and pincer attacks. Damage timing, effects, arrow/fire immunity and player passivity are unchanged.

The Lava Scorpion material atlas now uses neutral deepslate grays while preserving fiery accents and sparse orange fissures. The normal Scorpion texture is unchanged.

## Water behavior

Water nodes are forbidden on land. Native pathfinding can use dry detours; spider pounces and the climbing-navigation straight-line fallback cannot shortcut through water. Companion teleport destinations must fit the whole body without liquid. Existing sitting orders, dry wall climbing and non-swimming spiders remain intact.

A bounded escape goal can seek a reachable dry bank after falling in. It checks only wet mobs, at most once every forty ticks between attempts, skips unloaded candidate positions, and attempts at most four native paths per search. It temporarily permits water/shore nodes for that escape and restores the original costs afterward. Searches prefer banks at the current depth or above rather than unreachable cavities beneath the pond. The destination clears the entire body, and the final approach accounts for native waypoint tolerance. Spiders retain their slow underwater movement and can use walkable low banks; the change does not let them swim out of deep water. Scorpions retain native floating. There are no terrain edits or separate spawning/tick systems.

## Natural spawning

Lava Scorpions still require covered Overworld caves below Y=0, valid ground and nearby lava, with pack attempts of three through five. Their selection weight increases from 3 to 12 and the lava window increases to eight blocks on each horizontal axis and four vertically. Lava light was already accepted; no additional light restriction was found. Normal badlands Scorpion rarity is unchanged.

The native monster cap, player-distance limits, difficulty and mob-spawning gamerule still apply. A non-spectator player must be more than 24 blocks away and within the normal active spawning range. This is a change to native spawn entries and their predicate, not an independent spawning loop.

## Validation

All forty-nine related Ecosystems GameTests passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. These include the prior combat, immunity, companion and daytime behavior checks; water detours for six variants; spider pounce and climbing-fallback prevention; and physical shallow-water exits with path costs restored. The spider-hunting test now observes feeding and the next hunt instead of assuming a fixed acquisition tick. [Saved test result](scorpion-0.1.62/focused-tests.txt).

The added native spawning test exercises registered, weighted biome selection and the native category-spawn pipeline with emissive lava, a Creative player forty blocks away and a covered deepslate cave fixture. It uses seeded bounded retries, filters unrelated species and bypasses the global population cap for isolation; it verifies spawn eligibility, not a real-server encounter rate or a guarantee that every pack attempt succeeds. The fixture checks lava emission directly because artificial GameTest chunk lighting can lag its block changes.

A graphical client on the dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi inspected both final models. Native render observations show approximately 0.19 radians of walking feeler variation and 0.049–0.058 radians of walking tail yaw variation. The strike arches the abdomen about 0.279 radians, carries the tip to model Z=-1.13 blocks (head front approximately -0.61), and records zero secondary tail yaw during the fully blended strike. All transforms remained finite, eight leg chains were present, and no render errors were reported. [Observations](scorpion-0.1.62/render-results.json).

[Walking preview](scorpion-0.1.62/walking.gif), [normal face](scorpion-0.1.62/normal.png), [deepslate shell](scorpion-0.1.62/lava.png), [normal sting](scorpion-0.1.62/strike.png), [Lava Scorpion sting](scorpion-0.1.62/lava-strike.png). Victims are hidden in strike screenshots so the pose remains visible. Release and test-harness builds passed; test classes remain excluded from the release JAR. Unrelated suites were not rerun.

The user authorized committing and pushing this release.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.62.jar`

SHA-256: `5f3215d5dc73b768d877e4f8fbb1d19d77e4c8560fcf905610baab3b62a3f747`
