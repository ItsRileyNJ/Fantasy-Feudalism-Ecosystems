# Fantasy Feudalism - Ecosystems

A NeoForge mod for **Minecraft 1.21.1**, **NeoForge 21.1.262**, and **Java 21**, implementing the mob changes in [the specification and confirmed answers](docs/SPECIFICATION.md).

This repository contains only the Fantasy Feudalism - Ecosystems project, its build tools, tests, and documentation. Mercenaries is a separate mod: its implementation and JAR are not included. Optional compatibility patches and integration tests here belong to Mobs; the separate multiplayer test harness is excluded from the released mod.

## Install

Build with `./gradlew build --max-workers=2` (Windows: `gradlew.bat build`), then put `build/libs/fantasy_feudalism_ecosystems-0.1.27.jar` in the `mods` folder of a NeoForge 21.1.262 instance. Install the mod on both the client and the server for multiplayer. The NeoForge installer installs the loader; it is not the mod JAR.

## Version 0.1.27 changes

- Cold sheared sheep now visibly tremble through their head, body and legs, in addition to the native freezing tremor. This applies in snowy taiga and when exposed to rain.
- Adult sheep use a 30-in-1,000 grazing chance; lambs retain 3 in 50. Cold sheep still stop grazing and need 30 seconds of warm, dry shelter to recover.

See the [shivering validation report](docs/RELEASE_0.1.27.md).

## Version 0.1.26 changes

- Adult pigs dig on podzol, rooted dirt and coarse dirt about every two minutes, with a snout-down pawing animation.
- Dig success chances are 80%, 50% and 15%, respectively. Each success drops one mushroom: 80% brown, 20% red.
- Piglets eat nearby dropped mushrooms or accept hand-fed mushrooms; each removes 20% of remaining growth time.
- Adult pigs loosely regroup beyond about 15 blocks. Bred piglets remember both parents and follow family until adulthood.

See the [pig behavior report](docs/RELEASE_0.1.26.md).

## Version 0.1.25 changes

- Sheep need three successful grazes to regrow wool. Current grazing chances per native check are 30 in 1,000 for adults and 3 in 50 for lambs.
- Sheared sheep exposed to cool biomes or actual rain shiver, move 25% slower and stop grazing. A roof provides shelter in every biome.
- Previously cold sheep need 30 uninterrupted seconds in warm, dry conditions to recover. Grazing progress and recovery survive saves.

See the [sheep behavior report](docs/RELEASE_0.1.25.md).

## Version 0.1.24 changes

- The Bat Net switches to the supplied bat-in-net artwork while occupied and returns to the original sprite when emptied into an enclosure. Both states use flat item sprites.

See the [occupied-net validation report](docs/RELEASE_0.1.24.md).

## Version 0.1.23 changes

- The Bat Enclosure uses the supplied Blockbench bat box geometry, with custom spruce-colored wood, dirtied interior surfaces, and cauldron-colored front nails.
- Repositioned hanging bats to fit beneath the flat roof and stay clear of the model's angled rear baffle.

See the [model integration report](docs/RELEASE_0.1.23.md).

## Version 0.1.22 changes

- Field Mice have softly shaded white fur and a new amber/light-brown coat, both with pink ears, feet, noses, and tails. New natural and spawn-egg mice have an equal chance of either coat; their color saves and synchronizes in multiplayer.
- Mice ignore Creative and Spectator players, including ending an active escape when a player changes to either mode. Survival and Adventure players still scare mice.

- Cats spot mice within **16 blocks**, including mice spawned by eggs. Mice have a **90-second feeding cooldown** and wander and look around their remembered crops between visits.
- Enclosure bats hang from the ceiling with staggered positions. The interior has shaded, dirtied wood, and overlapping side-slat surfaces were removed to fix z-fighting.
- The Bat Net uses a flat wooden-and-mesh item sprite matching the supplied reference. Raw and cooked horse meat have new reference-based icons.

See the [mouse, enclosure, and item validation report](docs/RELEASE_0.1.22.md).

## Version 0.1.21 changes

- Added seventeen approved achievements for companions, bat farming, cat sleep, livestock protection, automatic horse births, and combat, including **Watch Dog** and **Master Naturalist**.
- **That's where I'd be** requires being hit by a skeleton arrow fired from its maximum 35-block range. **Everyone's got a plan until...** requires dying to one Enderman hit while at full health.
- On Hard, an unarmored player at the usual twenty health can now be killed by one ordinary Enderman melee hit. Armor, effects, shields, and totems retain their normal protections; Easy and Normal damage are unchanged.

See the [achievement list and validation report](docs/RELEASE_0.1.21.md).

## Version 0.1.20 changes

- Mice check crops and retry feeding every **20 seconds**, while keeping responsive fleeing and the two-minute feeding cooldown.
- Crop searches are staggered and shared across ticks. Morning spawning reuses a village's crop search and creates at most one mouse per server tick; pending morning work survives saving and reloading.

See the [before-and-after performance report](docs/RELEASE_0.1.20.md) for measurements and remaining startup spikes.

## Version 0.1.19 changes

- Added a white-and-pink **Field Mouse Spawn Egg** to the Ecosystems and vanilla Spawn Eggs creative tabs. It uses normal spawn-egg and dispenser behavior.
- Item ID: `fantasy_feudalism_mobs:field_mouse_spawn_egg`.

## Version 0.1.18 changes

- Field Mice now use the supplied five ambient squeaks and three hurt clips. Ambient calls use vanilla animal timing, with random selection among the clips. Hurt clips play for nonfatal and fatal damage.
- Added subtitles for mouse calls and damage. The original uploaded Ogg files are unchanged.

See the [sound validation report](docs/RELEASE_0.1.18.md).

## Version 0.1.17 changes

- A stray cat or your own cat within four blocks when you enter bed can settle beside you. After sleeping with it, wake up with **Rested** for six minutes: half the usual hunger exhaustion and 35% more natural healing. Its inventory icon is three blue Zs.
- **Field Mice** are tiny white village wildlife with pink tails and two hearts. Each morning, villages try to fill a population of one mouse per three cats; fewer than three cats produce none. Existing and unloaded mice count toward the limit.
- Each mouse eats one planted crop growth stage every two minutes, respecting `mobGriefing`. Mice flee players, cats, villagers, and Mercenaries within three blocks, then return to crops. Their escape speed is 10% below a cat's chase speed.
- Cats hunt mice within fifteen blocks and kill them in one hit. Sit orders and sleeping take priority.

See the [0.1.17 validation report](docs/RELEASE_0.1.17.md).

## Version 0.1.16 changes

See the [plain-language changelog](CHANGELOG.md) for the latest bat, spider, performance, and compatibility follow-up.

- Creeper vegetation respects `mobGriefing`; capturing a leashed bee returns its lead.
- Axes disable wither skeleton shields for five seconds. Explicit command/conversion equipment survives initialization.
- Spider companions and phantom guardians honor server PvP permissions. Frogs finish shoreline escapes into water.
- Bat navigation retries and phantom threat lookup perform less repeated work. Dedicated-server, vanilla, Mercenaries and two-client regression results are in [the audit report](docs/RELEASE_0.1.16.md).

## Version 0.1.15 changes

- Hard-only wither skeleton archers (10%) with half-damage arrows and five-second Wither I/Slowness I. Half of sword users carry shields, advancing crouched before charging from five blocks.
- Nighttime Eye of Ender phantom pacts protect against actual attackers until sunrise.
- Reusable Bat Nets, wooden/glass Bat Enclosures holding up to three real bats, nightly roaming and morning returns. Visible hanging bats always match actual interior occupancy.
- Apples heal housed bats and produce Fertilizer on independent five-minute cooldowns. Yellow Fertilizer instantly matures crops.

See [the release and validation report](docs/RELEASE_0.1.15.md).

## Version 0.1.14 changes

- Renamed the mod and creative tab to **Fantasy Feudalism - Ecosystems**. The JAR is now `fantasy_feudalism_ecosystems-0.1.14.jar`. Replace the old Mobs JAR when updating. Existing registry IDs and saved data are preserved.
- The [CurseForge description](docs/CURSEFORGE_DESCRIPTION.md) summarizes current features.

See [the release notes](docs/RELEASE_0.1.14.md). Earlier release names and validation reports below retain their historical names.

## Version 0.1.13 changes

- Frogs on land flee toward nearby water when a visible player approaches within six blocks, including creative players. They use a collision-checked jump, walking closer first when necessary.
- Water is searched within twelve blocks in loaded chunks. Spectators, frogs already in water, breeding, egg laying and leashes keep their normal behavior; no usable water leaves ordinary AI available.

See [the validation report](docs/RELEASE_0.1.13.md).

## Version 0.1.12 changes

- Automatic horse breeding now uses **2 hours 30 minutes / 180,000 world ticks** at normal tick speed. Existing horse timers shorten once while preserving time already waited; a horse that has waited two hours has thirty minutes left.
- Parent and foal cycles use the new interval. Manual breeding, grazing and foal growth are unchanged.

See [the validation report](docs/RELEASE_0.1.12.md).

## Version 0.1.11 changes

- Horse grazing can interrupt following a player holding golden horse food. Equal AI priorities previously allowed food-following to block grazing indefinitely.
- Eligible adults on grass use a 20–40-second grazing schedule, foals 10–20 seconds, and horses eligible for automatic breeding 5–10 seconds. Grazing yields to love mode, riding and danger.
- Automatic breeding still requires **14 Minecraft days / 336,000 world ticks / 4 hours 40 minutes at normal speed**. Ordinary grazing happens before that deadline. Manual golden-food breeding keeps vanilla behavior.

See [the validation report](docs/RELEASE_0.1.11.md).

## Version 0.1.10 changes

- Sitting tamed wolves protect sheep from wild wolves and chickens from foxes within a 24-block radius of the livestock. This also interrupts hunts already in progress and makes predators move away from nearby guarded livestock.
- A stopped wolf pack shares its normal one-day hunger cooldown; foxes now receive a persistent one-day hunting cooldown after a stopped hunt or a successful prey kill.
- Only the nearest guarding dog silently loses one heart per stopped pack or fox. Its ordinary health-based tail drops; feeding heals it normally. Fatigue cannot kill it and causes no hurt flash, damage sound, knockback, retaliation or standing.

See [the validation report](docs/RELEASE_0.1.10.md).

## Version 0.1.9 changes

- Mineshaft silverfish use twice their previous spawn weight and request packs of four to nine.

- Wild wolves stay near a stable pack leader, walking back when they drift more than six blocks away.
- A scout recruits nearby eligible pack members onto one sheep target. The pack regroups within eight blocks before a shared start signal launches stalking together. An unreachable assembly is canceled after ten seconds so the pack can try again.
- Tamed wolves, leashes and defensive combat keep their normal priorities. The existing shared charge and meal cooldown remain in place.

See [the validation report](docs/RELEASE_0.1.9.md).

## Version 0.1.8 changes

- Wild wolf packs share a sheep target detected within 15 blocks, stalk slowly together, and charge at normal attack speed once a member gets within seven blocks. Their animated stalking pose lowers the body, head and tail, with shorter walking steps and smooth transitions.
- A wild wolf killing a sheep starts a **24,000-tick / one-Minecraft-day** hunting cooldown for its whole pack, including unloaded members. Pack identity and meal deadlines survive saves. Tamed wolf orders, owner defense, feeding and normal combat stay vanilla.
- Baby villagers occasionally chase untamed village cats during their daytime play activity, for up to **160 ticks / eight seconds**. Cats run away playfully; neither participant attacks or damages the other. Danger interrupts play. Eligible children roll a 1-in-12 chance every ten seconds while idle, with at least two minutes between successful chases.

See [the validation report](docs/RELEASE_0.1.8.md).

## Version 0.1.7 changes

Foals naturally grow three times more slowly: **60 minutes** of loaded growth at 20 ticks/second, compared with vanilla’s 20 minutes. Existing foals also age at the slower rate, with partial growth progress saved across reloads. Feeding still advances vanilla age units immediately, and adult breeding cooldowns retain their normal rate. See [the validation report](docs/RELEASE_0.1.7.md).

## Version 0.1.6 changes

- Horses spawning together keep persistent herds, using the same regrouping rules as cows. Foals join a parent’s herd.
- Horses eat short grass or turn grass blocks to dirt using their native grazing animation, respecting `mobGriefing`.
- After 2 hours 30 minutes (180,000 elapsed world ticks), an adult’s successful grazing can start automatic breeding with one nearby eligible horse, including wild horses. Both parents receive a fresh 150-minute cycle after a birth. The first cycle starts when a horse is added; foals start their own timer at birth. Golden apple/carrot breeding remains unchanged.
- Adult horses drop 1–2 **Raw Horse**, in addition to normal loot. Foals drop no horse meat. Raw Horse cooks into **Cooked Horse** in a furnace (200 ticks) or smoker (100 ticks). Cooked Horse matches cooked mutton: six hunger points and 9.6 saturation. Both foods have their own sprites and appear in the mod creative tab.

See [the validation report](docs/RELEASE_0.1.6.md).

## Version 0.1.5 changes

Newly spawned cow groups keep a persistent herd identity. Separated cows walk back toward a shared leader, beginning beyond six blocks and settling within three. They elect another loaded herd member if the leader disappears. Panic, breeding, wheat temptation, calf following and leashes take precedence. Existing cows without a recorded spawn group are unchanged.

Adult and baby chickens flee Survival and Adventure players who come within three blocks. Fleeing takes priority over food temptation and uses 95% of normal player ground sprint speed (about 5.33 blocks/second on ordinary terrain). Normal wandering is unchanged; Creative and Spectator players do not scare them. See [the validation report](docs/RELEASE_0.1.5.md).

## Version 0.1.4 changes

Silverfish remain roaming until they personally land a killing blow on a zombie. Only that killer burrows into a nearby suitable block, then despawns after 30 seconds; the rest keep roaming until their own kill or death. Burrowing leaves no permanent infestation. Kill credit and the burrow deadline survive save/load. Silverfish prefer nearby zombies over players as prey. Version 0.1.9 raises mineshaft spawn weight to 120 and group attempts to 4–9, retaining dark placement rules and mob caps. See [the validation report](docs/RELEASE_0.1.4.md).

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

**Fantasy Feudalism - Ecosystems 0.1.14** (formerly Mobs) includes optional compatibility with the supplied **Fantasy Feudalism: Mercenaries 1.15.7** (`villagerscontract`). Install Ecosystems 0.1.14 and Mercenaries 1.15.7 on the server and every client, replacing the older Mobs JAR. Mercenaries remains optional when using Mobs alone.

Guards recognize tamed spiders as allies, companions respect guard alliances, and mercenary combat spacing follows a skeleton's equipped weapon. Faster skeleton arrows retain the requested 40% speed increase with a corrected gravity arc and vanilla spread. Rejected or losing guard-menu transfers explicitly correct the hidden player inventory so two viewers cannot retain predicted ghost items.

See [the current combined-mod validation report](docs/COMPATIBILITY_0.1.13.md) for exact JAR identities, completed server and two-client checks, screenshots, and limits. The [original compatibility report](docs/COMPATIBILITY.md) documents the earlier fixes. To run the combined GameTests against your own copy of the uploaded Mercenaries JAR:

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
| Wild wolves | Cohesive packs stalk sheep from fifteen blocks, charge within seven, and share a one-day hunting cooldown after a kill. Crouched stalking uses animated body, head and tail poses. |
| Tamed wolves and foxes | Sitting dogs guard sheep from wild wolves and chickens from foxes within 24 blocks. Stopped hunts start predator cooldowns and silently tire the nearest dog by one heart; feeding heals it. Foxes also rest after a prey kill. |
| Frogs | Flee visible non-spectator players within six blocks by jumping toward nearby water, walking closer first if needed. |
| Baby villagers | Occasionally chase wild village cats playfully for about eight seconds. |
| Horses | Persistent spawn-group herds, sheep-style grazing, automatic grass-triggered breeding after a 150-minute cooldown, and adult-only horse meat drops. Foals grow three times more slowly (60 minutes at 20 TPS). Manual breeding stays vanilla. |
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
| Silverfish | Natural mineshaft spawn weight 120, groups of 4–9, with vanilla hostile-mob light rules. They prefer zombies as prey, deal double damage to them, and flee wild or tamed spiders of both types. Each keeps roaming until its own zombie killing blow; only that killer burrows, then despawns 600 game ticks later. |

Spider-family natural spawn weights are halved. Spawn changes use vanilla weighted spawn lists and vanilla placement checks; actual pack sizes remain subject to available space, mob caps, and valid positions. Family changes include variants such as strays, bogged, wither skeletons, husks, drowned, zombie villagers, and zombified piglins where applicable.

## Configuration

The server generates `config/fantasy_feudalism_mobs-server.toml`. It has exactly these three switches:

```toml
creeperBlockConversion = true
endermanBlockPickup = false
blazeAerialDashing = true
```

Disabling creeper conversion keeps the vegetation and the protection against block destruction. No gameplay commands, advancements or custom sounds are added. The mod adds Bee Bottles, Raw Horse and Cooked Horse, with furnace and smoker recipes for horse meat.

## Development and verification

A full **JDK 21**, including `javac`, is required. Import the checkout as a Gradle project in your IDE.

```sh
./gradlew runGameTestServer build --max-workers=2
./gradlew runClient
```

`runGameTestServer` exercises the actual NeoForge server, vanilla entities, mixins, registry state, interactions, combat, navigation, spawning, persistence, loot, and configuration. Its development test classes and test structure are excluded from the distributable mod JAR. The normal `test` task has no JUnit tests; use the GameTest task for gameplay checks.

Before releasing changes used alongside Mercenaries 1.15.7, run `python tools/check_compatibility.py /absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar`. This checks the supported binary's hash and runs both mods' test namespaces in separate fresh worlds. Supply the Mercenaries JAR separately; it is never bundled. Both suites must pass; the ordinary CI build alone does not validate Mercenaries compatibility. See the [current results and reproduced Mercenaries-only test failure](docs/COMPATIBILITY_CURRENT.md).

The [follow-up mechanics and performance audit](docs/DEEP_AUDIT_CURRENT.md) records expanded vanilla checks, Creative/Spectator and offline-owner PvP fixes, trajectory benchmark results, and remaining Mercenaries runner failures. The [current two-client audit](docs/MULTIPLAYER_AUDIT_CURRENT.md) records the subsequent dedicated-server checks. Run `python tools/benchmark_ballistics.py --baseline 51c2ee8186c493acb3aa742b4f635ae18824a1b7` after building to reproduce the solver comparison.

In this cloud environment use `python /workspace/toolchains/gradle.py runGameTestServer build`. This helper selects the installed JDK and uses the configured network proxy with TLS verification. `runClient` requires a graphical desktop. Dedicated-server testing with two graphical clients, item conservation, persistence, resource reloads, and mob load is documented in [the validation report](docs/VALIDATION.md). The optional `multiplayerTestJar` task builds a separate development harness; keep it out of normal client/server installations. A dedicated `runServer` requires agreement to Minecraft's EULA.

Based on NeoForge's official MDK-1.21.1-ModDevGradle template at commit `00b49d3f81a257858ca425792d59b9e5ca0f7e56`. Its license is retained in `TEMPLATE_LICENSE.txt`.
