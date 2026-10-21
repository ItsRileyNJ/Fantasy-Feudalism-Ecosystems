# Fantasy Feudal Mobs 0.1.2

Targets **Minecraft 1.21.1**, **NeoForge 21.1.262**, and **Java 21**. Install the same Mobs version on the server and every client, replacing older Mobs JARs. Mercenaries remains optional; its supplied **1.15.7** JAR was tested unchanged and is not included in this repository or the Mobs release.

Release: `fantasy_feudalism_mobs-0.1.2.jar` (79,424 bytes).

SHA-256: `42b51d631ef667e3acc295899295a857ba496e267be694439394832836dc7c7e`.

## Changes and resolved causes

The [README](../README.md#version-012-changes) and [approved specification](SPECIFICATION.md) describe all requested behavior. This release adds the creative tab, two-pixel Bee Bottle artwork, brown/blue tamed textures, and the lowered sitting abdomen. Texture changes retain the vanilla UV layout, and each rendered entity resets its abdomen position to prevent shared-model state from leaking into wild or following spiders.

Attack pounces start within 2.75–3.25 blocks, travel forward with a low arc, and land short of the target. High navigation jumps remain available for three-block obstacles. Following companions retain their targets after surviving damage; the previous damage callback unnecessarily reapplied the follow order and cleared the target. Sitting companions still wake when hurt.

Both wild and tamed spider types ignore falls shorter than nine blocks. The immunity check accounts for the final movement onto the ground, which Minecraft omits from its fall-distance accumulator, while retaining vanilla damage calculations for larger drops. A wild spider's lethal bee attack fully heals it. Nonfatal bee hits and tamed spider kills do not grant that healing.

Companion effects remain Slowness I/Weakness I, plus Poison I for cave spiders, for 120 ticks. The existing skeleton, zombie and spider family exclusions remain. Actual successful attacks against eligible hostiles apply the effects; rejected hits do not. Vanilla effect immunities remain respected.

Crossbow skeletons use pillager-style two-handed holds even while idle, with vanilla charging animations. Skeleton target retention now permits clearing expired targets and rejects Creative/Spectator players. Zombie melee pursuit remains active between short paths instead of stopping and restarting its goal; vanilla path recalculation and initial acquisition timing remain.

Natural spawning previously recreated modified spawn entries on each query. Minecraft's final spawn validation compares entry identity, so it could reject the selected silverfish, spider, creeper, zombie or ghast entry. Canonical immutable entries now survive that validation. Silverfish retain weight 60, groups 3–9, dark hostile placement rules, and the mineshaft restriction.

## Final release validation

All final runs used the source that produced the release hash above. The packaged release was byte-identical on the dedicated server and both graphical clients.

| Check | Result |
| --- | --- |
| Mobs-only GameTests | **64/64**, process exit 0 |
| Mobs + Mercenaries focused GameTests | **74/74**, process exit 0 |
| Supplied Mercenaries GameTests with both mods | **336/336**, process exit 0 |
| Mobs-only two-client checks | **12/12 grouped checks** |
| Combined-mod two-client checks, including restart | **15/15 grouped checks** |
| Ordinary natural spawning in a generated mineshaft | **Passed**; one silverfish observed inside a generated piece, without eggs, summons or spawners |

The 64 standalone tests include real falling physics at 8.5, exactly 9, and 9.5 blocks for both wild and tamed spider types; the actual natural-spawner path; successful and rejected combat effects; bee feeding; target release; and moving-player zombie pursuit. The combined run adds ten optional compatibility regressions. Counts from repeated suites are not additional unique tests.

The graphical checks exercised the creative tab and item transfer, normal/tamed textures on both clients, repeated sit/follow toggles, resource reloads, crossbow holds and charging, all skeleton variants switching away from real Creative/Spectator players, wild bee-kill healing, and actual falls. The Mobs-only zombie chase used real client movement keys and movement packets: the zombie advanced about **6.77 blocks** during the measured pursuit, retained its target, and had **zero stationary sampled ticks** after initial acquisition. The player stayed on the flat test floor throughout.

Combined checks covered 200 competing contract requests, shared guard inventories, protected ammunition, order buttons, GUI scales 1/2/3, valid string deposits, rejected Bee Bottle/Poison transfers, private highlight delivery, resource reloads, cursor-stack logout/reconnect conservation, and twelve repeated two-client inventory races. A normal process restart preserved guard state, player inventories, spider ownership and harvest cooldowns. Real guard GUI horse mounting/dismounting synchronized to both clients.

The generated mineshaft test used a separate normal world with seed `1`. With ordinary mob spawning enabled, a naturally spawned silverfish was recorded at `(-59.5, -15, 15.5)` inside a generated corridor. Mob spawning was disabled again after sampling.

Evidence: [GameTest outcomes](release-0.1.2-evidence/gametests.txt), [Mobs-only visual checks](release-0.1.2-evidence/updates-0.1.2-results.json), [movement/fall/feeding checks](release-0.1.2-evidence/chase-spiders-0.1.2-results.json), [combined checks](release-0.1.2-evidence/mercenaries-results.json), and [natural spawn observations](release-0.1.2-evidence/natural-mineshaft.json). Screenshots show the [creative tab](release-0.1.2-evidence/updates-creative-owner.png), [spider textures/sitting](release-0.1.2-evidence/updates-spider-sitting.png), and [crossbow holds](release-0.1.2-evidence/updates-crossbow-idle.png).

Reproduce with JDK 21:

```sh
./gradlew runGameTestServer build --max-workers=2
./gradlew runGameTestServer --max-workers=2 \
  -PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar \
  -PgameTestNamespaces=fantasy_feudalism_mobs \
  -PgameTestDirectory=/absolute/path/to/new-empty-mobs-test-directory
./gradlew runGameTestServer --max-workers=2 \
  -PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar \
  -PgameTestNamespaces=villagerscontract \
  -PgameTestDirectory=/absolute/path/to/new-empty-mercenary-test-directory
```

The development-only harness is built with `multiplayerTestJar`. Use `tools/multiplayer/updates_0_1_2.py` and `chase_and_spiders_0_1_2.py` with the documented disposable local fixture; they modify that test world. The harness, GameTest classes/structures, and third-party mods are excluded from the released JAR.

## Scope and limitations

These results establish the tested local configurations. They do not establish compatibility with every additional mod, graphics driver, network condition, or long-running public server. Graphical clients used Mesa software rendering in Linux; physical GPU/audio devices and online authentication were not tested. The local test server used offline identities and loopback networking. Minecraft public-key requests produced the known cloud connectivity errors, and headless audio produced device warnings. No mod crash, remaining tested duplication/desync, missing texture, non-finite pose, or mixin application failure was observed in the final runs.

The supplied Mercenaries mounted-grass retreat assertion failed intermittently in earlier runs, including an earlier candidate here and the previously documented Mercenaries-only baseline. The final release run passed all 336 unchanged assertions. See the [historical compatibility report](COMPATIBILITY.md) for the baseline diagnosis. The old 200-mob benchmark belongs to 0.1.1 and is not presented as a new 0.1.2 load measurement.
