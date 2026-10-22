# Fantasy Feudalism - Ecosystems 0.1.16

This release fixes vanilla-interaction problems found during a compatibility audit and reduces repeated AI work. Validation uses Minecraft 1.21.1, NeoForge 21.1.262, Java 21 and the original **Fantasy Feudalism Mercenaries 1.15.7** JAR. Mercenaries remains optional and separate; its implementation and JAR are not bundled.

## Corrections

- **Creepers:** Vegetation placement now honors `mobGriefing` and NeoForge entity-griefing permission. Explosion damage to entities remains native. Enabled vegetation retains relevant facing/waterlogged state.
- **Bee capture:** Bottling a leashed bee drops its lead exactly once, including repeated stale capture requests.
- **Wither shields:** Real axe hits disable blocking for 100 world ticks/five seconds. The advance goal cannot immediately raise the shield again. Native shield damage/breaking, directional blocking and piercing-arrow bypass remain available.
- **Explicit equipment:** Command-created and converted skeletons preserve supplied weapons/shields, names and enchantments. Normally finalized spawns still receive the intended randomized loadouts; a 300-spawn test exercises that separate path.
- **Companion PvP:** Tamed spiders and bound phantoms respect the server's PvP setting as well as owner/team permissions. Previously recorded threats cannot bypass disabled PvP through companion damage.
- **Frog escape:** Vanilla long-jump vectors deliberately undershoot the destination. A shore landing now retains the water destination and walks the final distance instead of abandoning the escape or repeatedly jumping at the bank. Leashes and breeding still release the override.
- **Bat flight:** Each resident reuses its flight navigation. Failed/completed paths retry on their normal deadline rather than allocating navigation and calculating a path every tick. Dawn/radius corrections can still request an immediate path.
- **Guardian lookup:** Idle guardians resolve recorded attacker UUIDs directly instead of scanning every nearby living entity. Loaded valid attackers retain nearest-target selection; malformed UUID entries are safely removed.
- **Saved data:** Bat-colony and wolf-pack saved-data factories are reused rather than allocated on every lookup.

Existing custom gameplay rules remain intentional. This audit checks their interactions with native mechanics; it does not claim the mod leaves all vanilla behavior unchanged.

## Server suites

| Configuration | Final passing cases |
| --- | ---: |
| Ecosystems alone | 156/156 |
| Ecosystems namespace with Mercenaries | 168/168 |
| Mercenaries namespace with Ecosystems | 338/338 |
| Mercenaries-only reference for original mounted-retreat test | 3/3 |

The combined configuration therefore passed **506 cases** across its two namespaces. Its 338 Mercenaries-namespace cases comprise the original 336 tests plus two supplementary terrain/range cases that use the original mod's test fixtures. No original assertions were changed or suppressed.

Coverage includes actual combat/AI, equipment distributions, ordinary player arrows, axes/piercing/shield durability, milk and effect removal, bone meal and Fertilizer, zombie-villager curing, loot gamerules, creeper terrain, bee leads, piston rejection for occupied enclosures, bat identities/capacity/cooldowns/flight, guardian permissions, spider effects/fall/leaps, wildlife behavior and the previous horse, wolf, mineshaft and companion regressions. Mercenary suites cover recruitment, food/supplies, equipment, orders, mounting, navigation, combat and inventory synchronization. See [suite outcomes](release-0.1.16-evidence/gametests.txt).

Reproduction: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`, using a fresh `-PgameTestDirectory=/absolute/path`. Combined runs add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar`. Run `-PgameTestNamespaces=fantasy_feudalism_mobs` and `-PgameTestNamespaces=villagerscontract` separately. Multiplayer tools are under `tools/multiplayer/`; the diagnostic mod is built separately and excluded from release.

## Dedicated server and two graphical clients

The packaged release passed **77/77 recorded live checks**, plus **12/12 repeated shared-inventory race trials**, on a normal dedicated server and both independently connected clients. Live evidence includes actual interaction packets, inventories, GuardScreens, resource reloads, rendered frames and normal process restarts. Server commands prepare fixtures; client actions perform capture, feeding, attacks, drinking, GUI insertion and menu interactions. See the [manifest and per-file counts](release-0.1.16-evidence/manifest.json).

Recorded checks cover vanilla lead recovery, axe disable/recovery, protected terrain, normal bone meal, Fertilizer, actual milk drinking, furnace cooking/hopper extraction and both furnace screens; concurrent bee/harvest ownership and item conservation; Mercenary contracts, shared inventory races, reserved slots, orders, GUI scales and private highlights; restart persistence and mounting; bat capture/display/flight/dawn return; phantom combat and sunrise; wither effects/particles/poses; item geometry/creative tabs; frog escapes; and mixed active-mob load. The JSON evidence records individual outcomes and measured load timings.

The final 35.5-second mixed-load run began with 180 active modified mobs and 20 mercenaries. It recorded **8.92 ms mean / 22.13 ms p95 / 66.94 ms maximum** server tick time and approximately 20 TPS. Natural combat reduced the remaining entity count to 158. The two software-rendered clients advanced 509 and 595 frames, approximately **14.3 and 16.8 FPS**; that is responsive rendering under this fixture, not a claim of smooth hardware-accelerated performance. No render errors were recorded. [Load details](release-0.1.16-evidence/mercenaries-results.json) include the full measurements.

Visual evidence includes the settled one-bat cabinet on [owner](release-0.1.16-evidence/screenshots/owner-final-one-bat.png) and [guest](release-0.1.16-evidence/screenshots/guest-final-one-bat.png), [three occupants](release-0.1.16-evidence/screenshots/owner-bats-3-clean.png), the [mounted guard menu](release-0.1.16-evidence/screenshots/owner-mercenary-mounted-menu.png), and [ordinary furnace cooking](release-0.1.16-evidence/screenshots/owner-furnace-owner.png).

## Controlled performance comparison

The exact 0.1.15 release is compared with 0.1.16 on the same dedicated-server setup with Mercenaries installed. No graphical clients or Gradle tasks run during these measurements. Each fixture warms for ten seconds, then collects approximately 32 seconds of server timings and Java Flight Recorder samples in forced-loaded chunks. Exact entity counts are checked before accepting results.

| Fixture | Mean tick ms, before → after | p95 tick ms, before → after | Maximum tick ms, before → after |
| --- | --- | --- | --- |
| 40 enclosures / 120 resident bats | 2.737 → 2.497 | 5.080 → 3.446 | 23.102 → 94.250 |
| 120 idle guardians + 400 nearby creatures | 5.745 → 3.925 | 16.872 → 7.735 | 34.713 → 18.630 |

Both maintained approximately 20 TPS. Observed mean reductions are **8.8%** and **31.7%**; p95 reductions are **32.2%** and **54.2%**. The bat maximum increased: these short runs do not establish consistently better worst-case latency. JFR sampled allocation estimates under bat ticking fell from approximately 71 MB to 32 MB. The former guardian scan produced approximately 261 MB of inclusive sampled allocation under its attack predicate; the optimized run recorded no allocation sample there. These are statistical samples, not exact totals or proof of zero allocation. See [raw timing summaries](release-0.1.16-evidence/performance.json) and [JFR summary](release-0.1.16-evidence/jfr-summary.json).

## Findings retained and limits

Earlier attempts are retained in [the findings record](release-0.1.16-evidence/prior-run-findings.txt). The original mounted-retreat test can fail despite safe separation: its strict X-coordinate endpoint assertion rejects a valid diagonal retreat. A crossbow-equipped fixture also starts outside the crossbow retreat range. The final untouched original suite passed; new supplemental cases explicitly test bow retreat by actual separation and crossbow holding/fire outside its shorter range. Another earlier native suite stopped advancing and was terminated. Read-only diagnostics now report stalled pending tests; they do not advance ticks, remove entities, or alter assertions.

Two initial performance fixtures had incorrect loaded counts and were discarded. The accepted runs force-load the arena, clear delayed saved entities and verify matching counts. Live fixture corrections included facing a shield toward the attacker and inserting furnace ingredients through the GUI: incomplete command-loaded furnace NBT had a native zero cook-time total. Neither required a gameplay workaround.

Tests use a controlled loopback server, two offline test identities and Linux Mesa software rendering at 960×540 with a 30 FPS cap. They cannot prove every vanilla interaction or arbitrary mod pack, terrain, network or GPU is defect-free. Intermittent original Mercenary test behavior remains documented. No new crash, watchdog failure, mixin injection/application error, packet-handler exception, missing model, duplicate output or inventory divergence was observed in the completed final sessions. The [log review](release-0.1.16-evidence/log-review.txt) distinguishes offline authentication/DNS errors, fresh GameTest property-file initialization and absent optional Mercenary target-class warnings from gameplay failures. Final normal shutdown saved all dimensions.

## Installation

Replace the old Ecosystems/Mobs JAR on the server and every client with `fantasy_feudalism_ecosystems-0.1.16.jar`; do not keep both versions installed. Registry IDs and saved-data namespaces remain unchanged.

Release: **201,908 bytes**, SHA-256 `c315a3600871a190e594118e38aab8b021020c3cc984707ad38a96611d680bdf`. All 91 production classes match compiled output; resource JSON parses. GameTests, diagnostic classes, test structures and third-party JARs are excluded. The [manifest](release-0.1.16-evidence/manifest.json) records runtime hashes.
