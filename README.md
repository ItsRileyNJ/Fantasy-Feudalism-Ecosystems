# Fantasy Feudal Mobs

A NeoForge mod for **Minecraft 1.21.1**, **NeoForge 21.1.262**, and **Java 21**, implementing the mob changes in [the specification and confirmed answers](docs/SPECIFICATION.md).

This repository contains only the Fantasy Feudal Mobs project, its build tools, tests, and documentation. Mercenaries is a separate mod: its implementation and JAR are not included. Optional compatibility patches and integration tests here belong to Mobs; the separate multiplayer test harness is excluded from the released mod.

## Install

Build with `./gradlew build --max-workers=2` (Windows: `gradlew.bat build`), then put `build/libs/fantasy_feudalism_mobs-0.1.6.jar` in the `mods` folder of a NeoForge 21.1.262 instance. Install the mod on both the client and the server for multiplayer. The NeoForge installer installs the loader; it is not the mod JAR.

## Version 0.1.6 changes

- Horses spawning together keep persistent herds, using the same regrouping rules as cows. Foals join a parent’s herd.
- Horses eat short grass or turn grass blocks to dirt using their native grazing animation, respecting `mobGriefing`.
- After 14 Minecraft days (336,000 elapsed world ticks), an adult’s successful grazing can start automatic breeding with one nearby eligible horse, including wild horses. Both parents receive a fresh 14-day cycle after a birth. The first cycle starts when a horse is added; foals start their own timer at birth. Golden apple/carrot breeding remains unchanged.
- Adult horses drop 1–2 **Raw Horse**, in addition to normal loot. Foals drop no horse meat. Raw Horse cooks into **Cooked Horse** in a furnace (200 ticks) or smoker (100 ticks). Cooked Horse matches cooked mutton: six hunger points and 9.6 saturation. Both foods have their own sprites and appear in the mod creative tab.

See [the validation report](docs/RELEASE_0.1.6.md).

## Version 0.1.5 changes

Newly spawned cow groups keep a persistent herd identity. Separated cows walk back toward a shared leader, beginning beyond six blocks and settling within three. They elect another loaded herd member if the leader disappears. Panic, breeding, wheat temptation, calf following and leashes take precedence. Existing cows without a recorded spawn group are unchanged.

Adult and baby chickens flee Survival and Adventure players who come within three blocks. Fleeing takes priority over food temptation and uses 95% of normal player ground sprint speed (about 5.33 blocks/second on ordinary terrain). Normal wandering is unchanged; Creative and Spectator players do not scare them. See [the validation report](docs/RELEASE_0.1.5.md).

## Version 0.1.4 changes

Silverfish remain roaming until they personally land a killing blow on a zombie. Only that killer burrows into a nearby suitable block, then despawns after 30 seconds; the rest keep roaming until their own kill or death. Burrowing leaves no permanent infestation. Kill credit and the burrow deadline survive save/load. Silverfish prefer nearby zombies over players as prey. The existing mineshaft spawn weight 60, group attempts of 3–9, dark placement rules and mob caps remain unchanged. See [the validation report](docs/RELEASE_0.1.4.md).

## Version 0.1.3 changes

Sitting tamed regular and cave spiders tilt their rear abdomen downward by approximately ten degrees. Following restores the normal angle. This replaces the vertical offset in 0.1.2; see [the validation report](docs/RELEASE_0.1.3.md). A subsequent [two-client particle audit](docs/SPIDER_EFFECT_PARTICLES.md) verified the matching vanilla particles for all companion effects using this unchanged release.

## Version 0.1.2 changes

- A **Fantasy Feudal Mobs** creative tab contains the Bee Bottle. The bottle now shows a two-pixel black-and-yellow bee inside vanilla glass bottle artwork.
- Tamed regular spiders are slightly browner; tamed cave spiders are bluer. Their rear abdomen lowers slightly while sitting and returns to normal when following. Wild spiders retain vanilla textures.
- Spider attack leaps start only around three blocks away (2.75–3.25 blocks), use a low forward arc, and land short of the target. Three-block obstacle jumps remain available for navigation.
- Wild and tamed spiders ignore falls shorter than nine blocks. Larger falls retain normal damage, and surviving damage no longer clears a following companion's combat target.
- Killing a bee fully heals a wild spider. Nonfatal hits and tamed spider kills do not grant this healing.
- Crossbow skeletons use pillager-style two-handed holding and charging poses, including the idle hold. Bow and crossbow skeletons release Creative/Spectator players.
- Zombie pursuit stays active between short routes, retaining vanilla path recalculation timing and normal target validity checks.
- Natural spawn entries keep the identities required by Minecraft's final spawn validation. This fixes mineshaft silverfish and restores the intended spawn rules for the other modified mob families.

The six-second level I companion effects and their approved family exclusions are unchanged and have additional combat tests. See [the 0.1.2 validation report](docs/RELEASE_0.1.2.md) for that release's measured results and limitations.

## Mercenaries compatibility

Version **0.1.1** includes optional compatibility with the supplied **Fantasy Feudalism: Mercenaries 1.15.7** (`villagerscontract`). Install Mobs 0.1.6 and Mercenaries 1.15.7 on the server and every client, replacing the older Mobs JAR. Mercenaries remains optional when using Mobs alone.

Guards recognize tamed spiders as allies, companions respect guard alliances, and mercenary combat spacing follows a skeleton's equipped weapon. Faster skeleton arrows retain the requested 40% speed increase with a corrected gravity arc and vanilla spread. Rejected or losing guard-menu transfers explicitly correct the hidden player inventory so two viewers cannot retain predicted ghost items.

See [the combined-mod validation report](docs/COMPATIBILITY.md) for exact JAR identities, successful checks, diagnostics, screenshots, and limits. To run the combined GameTests against your own copy of the uploaded Mercenaries JAR:

```sh
./gradlew runGameTestServer --max-workers=2 \
  -PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar \
  -PgameTestNamespaces=fantasy_feudalism_mobs \
  -PgameTestDirectory=/absolute/path/to/new-mobs-test-directory
./gradlew runGameTestServer --max-workers=2 \
  -PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar \
  -PgameTestNamespaces=villagerscontract \
  -PgameTestDirectory=/absolute/path/to/new-mercenary-test-directory
```

Choose a fresh test directory for each combined run: Mercenaries' built-in village tests use fixed saved-data identifiers. The external JAR is used only for testing and is not bundled in the release.

## Spiders and Bee Bottles

- Right-click a bee with a glass bottle to capture it. The Bee Bottle uses glass bottle artwork with a two-pixel black-and-yellow bee inside.
- Feed a Bee Bottle to a regular or cave spider for a **33%** taming chance. Feeding returns an empty bottle, including on a failed attempt.
- Newly tamed spiders start sitting, with their rear abdomen angled down slightly. The owner can right-click to switch between sitting and following. Following uses vanilla wolf distance thresholds and safe owner teleporting. Companions can be leashed and persist through saves and Peaceful difficulty.
- Tamed spiders have **30 health / 15 hearts** and **0.36 movement speed**. Raw meat, raw fish, and rotten flesh heal them using vanilla wolf food-healing amounts. Feeding shows hearts; spiders cannot breed.
- Spiders cannot swim. Tamed spiders avoid water, lava, and fire, take **25% extra fire damage**, and make their own sounds only when a hostile mob is within **16 blocks**.
- Both wild and tamed spiders use three-block jumps, including three-block edges in their path search. Combat pounces on level ground travel forward with a low arc, reserving high jumps for elevated targets or obstacles. Wild regular spiders hunt bees and fully heal after killing one; wild cave spiders hunt silverfish. Both wild and tamed spider types are immune to falls shorter than nine blocks.
- Right-click a tamed spider with **shears** for **1–4 string**, costing **1 shears durability**. Use a **glass bottle** for **one standard vanilla Poison potion**. Each resource has its own **48,000-game-tick** cooldown per spider, saved with the entity.
- Tamed spider hits apply **Slowness I and Weakness I for six seconds** to hostile mobs; cave spiders also apply **Poison I for six seconds**. The affected enemy shows the matching vanilla status-effect particles. Skeleton, zombie, and spider families are excluded from these effects.
- Wild spiders are neutral toward players who have a tamed spider following them.

## Other mob changes

| Mob | Changes |
| --- | --- |
| Horses | Persistent spawn-group herds, sheep-style grazing, automatic grass-triggered breeding every 14 days, and adult-only horse meat drops. Manual breeding stays vanilla. |
| Cows | Newly spawned groups stay together as persistent herds, walking back when separated. |
| Chickens | Flee Survival/Adventure players within three blocks, at 95% of normal player ground sprint speed. |
| Creepers | Natural spawn weights reduced by 60%; explosions retain entity damage but do not destroy blocks. They convert applicable blocks to mossy variants and plant vegetation with 75% eligible-position coverage, biased 50/35/15 toward moss carpet / ferns / tall grass. Vanilla survival rules restrict which plants fit each position. Killed creepers additionally drop 1–2 moss blocks. |
| Skeleton families | Detect and shoot up to 35 blocks; projectiles are 40% faster. Equipment: 80% bows, 15% crossbows, 5% swords. Sword users have melee AI and use 80% stone / 20% iron swords. Crossbows use vanilla charging/firing AI and pillager-style two-handed hold/charge poses, including the idle hold. Ranged skeletons retain valid targets unless a player is within five blocks; expired targets and players who switch to Creative or Spectator mode are released. |
| Endermen | Deal 35% more damage to players, disengage and teleport away after one successful hit, and can immediately be provoked again. Block pickup is disabled by default. |
| Zombie families | Natural groups of 4–9; independently randomized 80–120% speed and attack damage. Speed doubles below three hearts and returns to the individual baseline when healed. |
| Phantoms | Attack players only at four hearts or less. Those spawned on Hard receive infinite Swiftness I. |
| Armor-wearing undead | One eligibility roll per mob: Easy 20%, Normal 35%, Hard 60%. Material weights: leather 50%, chainmail 25%, iron 20%, gold 5%; vanilla armor-slot selection. Each piece has a 50% unenchanted / 35% level I / 15% level II enchantment distribution, using applicable vanilla enchanting-table enchantments. |
| Blazes | Alternate four-block sideways dashes over ten ticks, respecting collisions. After the five-second cooldown, roll a 20% chance each second while they have an attack target. |
| Ghasts | Natural spawn weights reduced by 20%, groups of three, exactly two ghast tears on death. Other vanilla drops remain. |
| Silverfish | Natural mineshaft spawn weight 60, groups of 3–9, with vanilla hostile-mob light rules. They prefer zombies as prey, deal double damage to them, and flee wild or tamed spiders of both types. Each keeps roaming until its own zombie killing blow; only that killer burrows, then despawns 600 game ticks later. |

Spider-family natural spawn weights are halved. Spawn changes use vanilla weighted spawn lists and vanilla placement checks; actual pack sizes remain subject to available space, mob caps, and valid positions. Family changes include variants such as strays, bogged, wither skeletons, husks, drowned, zombie villagers, and zombified piglins where applicable.

## Configuration

The server generates `config/fantasy_feudalism_mobs-server.toml`. It has exactly these three switches:

```toml
creeperBlockConversion = true
endermanBlockPickup = false
blazeAerialDashing = true
```

Disabling creeper conversion keeps the vegetation and the protection against block destruction. No commands, recipes, advancements, additional items, or custom sounds are added.

## Development and verification

A full **JDK 21**, including `javac`, is required. Import the checkout as a Gradle project in your IDE.

```sh
./gradlew runGameTestServer build --max-workers=2
./gradlew runClient
```

`runGameTestServer` exercises the actual NeoForge server, vanilla entities, mixins, registry state, interactions, combat, navigation, spawning, persistence, loot, and configuration. Its development test classes and test structure are excluded from the distributable mod JAR. The normal `test` task has no JUnit tests; use the GameTest task for gameplay checks.

In this cloud environment use `python /workspace/toolchains/gradle.py runGameTestServer build`. This helper selects the installed JDK and uses the configured network proxy with TLS verification. `runClient` requires a graphical desktop. Dedicated-server testing with two graphical clients, item conservation, persistence, resource reloads, and mob load is documented in [the validation report](docs/VALIDATION.md). The optional `multiplayerTestJar` task builds a separate development harness; keep it out of normal client/server installations. A dedicated `runServer` requires agreement to Minecraft's EULA.

Based on NeoForge's official MDK-1.21.1-ModDevGradle template at commit `00b49d3f81a257858ca425792d59b9e5ca0f7e56`. Its license is retained in `TEMPLATE_LICENSE.txt`.
