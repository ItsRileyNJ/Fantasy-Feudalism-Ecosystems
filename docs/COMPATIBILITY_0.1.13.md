# Mobs 0.1.13 and Mercenaries 1.15.7 compatibility

The completed checks found no compatibility defect requiring a gameplay change in this pair. Both mods used their existing release JARs. Tests covered the actual NeoForge server, two connected graphical clients, concurrent item transfers, combat, rendering, persistence and a bounded mixed-mob load.

## Exact tested versions

- Minecraft **1.21.1**, NeoForge **21.1.262**, Java **21**.
- Fantasy Feudal Mobs **0.1.13**, code commit `713c410`; SHA-256 `9249d4babca7ab0899fe8355f68519f0bc9603b96073f5a3c96d7a2543a578fa`.
- The user-supplied Fantasy Feudalism: Mercenaries **1.15.7** (`villagerscontract`); SHA-256 `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.

The dedicated server and both clients loaded identical copies of both JARs, verified by hash. Mercenaries configuration retained its defaults, including ammunition consumption. The two JARs have no overlapping asset/data file paths. Mercenaries is not modified or bundled into Mobs. The separate local diagnostic harness is excluded from the released mod.

## Automated server tests

| Suite, with both mods loaded | Required tests passed |
| --- | ---: |
| Mobs behavior and focused compatibility regressions | 130 / 130 |
| Supplied Mercenaries tests | 336 / 336 |
| Total | **466 / 466** |

These are completed NeoForge GameTest server runs, with successful Gradle exits and no failed required tests. Each namespace ran in a separate fresh test world, because Mercenaries village fixtures use fixed saved-data identifiers. No assertions were disabled or patched. Ordinary Gradle `test` has no JUnit sources and is not included in these totals.

Mobs coverage includes companion alliance and friendly-fire rules, skeleton weapon classification and projectiles, rejected guard-menu prediction corrections, harvesting and cooldowns, modified mob AI, herds, grazing, growth, breeding, guard dogs, silverfish, spawning, wolves, frogs and save/load. Mercenaries' suite exercises contracts, inventory/packet safety, orders, combat, mounting, patrols, village supplies, sieges and persistence. See [recorded outcomes](compatibility-0.1.13-evidence/gametests.txt).

## Dedicated server and two graphical clients

56 completed live checks passed. They used the packaged JARs through a production dedicated-server launch, two separate graphical client JVMs and normal client interaction/container packets. Test fixtures set up entities, inventories and terrain; they did not replace the mod networking or the guard-menu transfer code.

- Competing bee capture, actual randomized taming, ownership protection, 2,000 competing poison requests, 2,000 competing silk requests, full inventories and item conservation.
- Two players sending 200 competing contract interactions convert one two-pillager patrol once and consume exactly one contract.
- Shared GuardScreens preserve string counts, correct hidden-inventory predictions, protect ammunition, synchronize orders, accept valid deposits and reject Bee Bottles and harvested Poison without loss or duplication. Twelve additional races issue 96 competing quick-move requests and conserve 17 string per race.
- Private guard highlights reach only the requesting client. Both clients survive resource reloads without missing Bee Bottle models or recorded render failures. Disconnecting while holding a menu cursor stack preserves the total across players, world drops and guard pickup, with exact inventory agreement after reconnect.
- A normal server save/stop/process restart preserves guard contracts, equipment, supplies, orders and skins, companion ownership and independent harvest cooldowns, and both player inventories. Restarting cannot bypass poison cooldowns. Actual guard-menu mount/dismount buttons update both viewers correctly.
- GUI scaling is checked on both clients at actual scales **1, 2 and 3**, using **1280×720** windows so scale 3 is supported. Inventories and guard slots still match the server. These windows return to 960×540 afterward. The original 960×540 requested-scale test alone cannot establish actual scale 3, because Minecraft clamps it.
- Live bow and crossbow skeleton AI damage mercenaries at **10 and 27 blocks**, with matching health changes visible on both clients.
- Additional current-feature checks cover real spider-effect particle creation/expiry on both clients, sitting-dog protection/fatigue/healing, the complete 150-minute horse breeding interval and native grazing, and frogs physically jumping into water with synchronized native animation. Vanilla `/tick sprint` accelerates only the long horse calendar portions; horse timers, love, mating goals and offspring are not assigned by the harness.
- Both clients complete the vanilla logout sequence promptly, rather than remaining connected until timeout.

Evidence: [all live results](compatibility-0.1.13-evidence/live-results.json), [actual scale-3 guard GUI](compatibility-0.1.13-evidence/guard-gui-scale-3.png), and [mixed mobs and guards](compatibility-0.1.13-evidence/mixed-load.png). Screenshots were visually inspected. They supplement the state and rendering assertions; they are not an exhaustive visual review of every possible animation.

## Bounded mixed load

The dedicated server ran **180 active modified mobs plus 20 armed mercenaries**, with both graphical clients connected, for **35.38 seconds** after setup and without concurrent Gradle or GameTest processes. All 200 entities remained represented at the final sample. Both clients advanced normal and guard-render frames without recorded render errors, missing Bee Bottle models or disconnects.

| Server tick processing | Measured time |
| --- | ---: |
| Mean | **15.18 ms** |
| 95th percentile | **44.63 ms** |
| Maximum | **136.89 ms** |

The fixture sampled 735 tick durations and advanced 734 ticks during the timed RPC observation, remaining around the normal 20 TPS. RPC timing and the beginning/end of snapshots make its computed elapsed-rate estimate approximate. These are bounded measurements, not a production capacity guarantee.

## Logs and limits

The completed server and client logs showed no mixin application/injection failure, packet-handler failure, watchdog crash, out-of-memory error or exception ticking an entity/server. The tested inventories conserved items and matched the server; observed model checks recorded no non-finite rotations or missing Bee Bottle models. See [the runtime log review](compatibility-0.1.13-evidence/runtime-log-review.txt) and [artifact/fixture manifest](compatibility-0.1.13-evidence/manifest.json).

The cloud could not resolve Minecraft authentication services, producing Yggdrasil public-key/profile lookup errors. This test used offline identities bound to loopback, so online authentication and secure chat are outside its coverage. Fixture teleports can log movement warnings; vanilla loader/asset/shader warnings may also appear. The report does not count those environment messages as mod compatibility failures.

Coverage is limited to this exact mod pair, Minecraft/NeoForge version, two local clients, software Mesa graphics and the recorded durations. Physical audio output, native GPU/driver combinations, other operating systems/modpacks, public-server scale, long-duration production use, and high-latency/lossy networks remain untested. Passing these checks does not establish that every possible configuration is bug-free.

## Reproduce

Use the two fresh-world commands in the [README](../README.md#mercenaries-compatibility) for the namespaces `fantasy_feudalism_mobs` and `villagerscontract`. Build the separate local helper with `./gradlew multiplayerTestJar --max-workers=2`.

For the retained disposable cloud fixture, start Xorg and the server/owner/guest roles as documented in the [original compatibility report](COMPATIBILITY.md#repeating-the-live-fixture). Run `control.py`, `suite.py`, `mercenaries.py`, and `mercenaries_races.py` from `tools/multiplayer`. After races, save and refresh the restart checkpoint from the current guards, pets and player inventories; the races intentionally change the earlier checkpoint's inventory contents. Disconnect both clients, stop the server normally, restart the same world, and run `mercenaries_restart.py`.

Before clearing the guards, run `mercenaries_gui_scales.py` (local X11 fixture; requires libX11, xwininfo and Pillow). Then run `mercenaries_load.py`, `spider_effect_particles.py`, `guard_dogs_0_1_10.py`, `horse_autonomy_0_1_12.py`, `frogs_0_1_13.py`, and `logout.py`. Set both players to survival before the initial inventory suite. These scripts modify the disposable fixture world and must not be pointed at a production server.
