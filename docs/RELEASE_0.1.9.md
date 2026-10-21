# Fantasy Feudal Mobs 0.1.9

Wild wolves stay close to their persistent pack between hunts. A separated companion follows the pack's stable UUID-selected leader when more than six blocks away, stopping within three blocks. This uses ordinary navigation with periodic path updates, never teleporting. Higher-priority survival and combat goals take precedence, and tamed wolves never use this behavior.

A sheep scout recruits nearby eligible adult pack members onto one target. The scout holds position while companions regroup within eight blocks. Once their hunt goals are running, one shared server deadline starts stalking together. Assembly waits at least twenty ticks and cancels after two hundred ticks if a companion cannot regroup, allowing another attempt without awarding a meal. Existing fifteen-block detection, seven-block shared charge, crouched animation, vanilla melee timing and the shared one-day meal cooldown remain in place.

Mineshaft silverfish now have spawn weight **120 instead of 60**, with requested packs of **four to nine instead of three to nine**. This doubles their selection weight, rather than guaranteeing twice as many entities: other spawn entries, available space, darkness and normal mob caps still apply. The change uses Minecraft's natural spawner and remains restricted to mineshaft structure pieces. Personal zombie-kill credit, burrowing and thirty-second removal are unchanged.

Install `fantasy_feudalism_mobs-0.1.9.jar` on the server and every client, replacing older Mobs JARs. Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Mercenaries 1.15.7 remains separate and optional.

Release size: **134,923 bytes**. SHA-256: `e956a10a72904665ee4463eefb14723c6f4b951ac5668d864359f222ec2f61b1`.

## Automated validation

- **104/104** required standalone server GameTests passed.
- **114/114** required server GameTests passed with Mercenaries 1.15.7.
- Release and separate multiplayer diagnostics build successfully. All 69 packaged main classes match compiled output. GameTests, diagnostics and third-party JARs are excluded from the release.

New world-ticked regressions cover a separated wolf walking back without teleporting, a scout waiting for its straggler, both wolves beginning stalking within one server tick, and exclusion of tamed/defensive behavior from regrouping. Existing full hunts, shared cooldowns, persistence and tamed behavior continue to pass. Updated silverfish tests validate the actual mineshaft event entry, stable spawn-entry identity, a real natural-spawner pass producing at least four fish, dark placement, persistence, and ordinary AI hunting zombies.

Reproduce with JDK 21: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. For combined testing, add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`.

Evidence: [GameTest outcomes](release-0.1.9-evidence/gametests.txt) and [artifact manifest](release-0.1.9-evidence/manifest.json).

## Dedicated server and graphical clients

The final release was installed byte-for-byte identically on a normal dedicated server and two graphical clients, all also running Mercenaries 1.15.7. **Thirteen wolf/village checks passed**:

- A deliberately separated wild wolf walked back to its stable pack leader.
- Three naturally grouped wolves began stalking on exactly the same server game tick: **90,581** for all three. They shared a sheep target and caused no distant hits.
- Both clients rendered the crouched walking pose and the shared charge transition. A nearby tamed wolf retained its normal model pose. A real kill fed the whole wild pack, spared another sheep, cleared targets and restored standing poses.
- Real client attacks and interactions confirmed tamed owner defense, following and sitting during the former pack's cooldown. The previous harmless baby-villager cat play also passed its movement and stop checks.
- Both connections remained active with no recorded render errors.

The wolf fixture supplied normal natural-spawn grouping and stationary sheep for observation; it did not force combat targets, attacks, damage, launch times, meal deadlines or model poses. A passive diagnostic recorded each wolf's first stalking tick.

Evidence: [thirteen results and snapshots](release-0.1.9-evidence/wolf-multiplayer-results.json), [stalking with a normal tamed wolf](release-0.1.9-evidence/wolf-pack-stalking.png). Reproduce with `tools/multiplayer/wolf_cohesion_0_1_9.py` and the separate diagnostic harness.



Three further mineshaft checks passed, bringing the total to **sixteen live checks**. Four nearby silverfish spawned through normal natural spawning in an unchanged generated seed-1 mineshaft. Natural-spawn events and structure membership were recorded; the harness never created these silverfish. The group remained above ground for more than ten seconds with mob griefing enabled, and both graphical clients tracked the same four entities without recorded rendering errors. Competing mobs were removed periodically to free mob-cap slots and later isolated from the observation group; structure blocks and lighting were not rebuilt.

Evidence: [three mineshaft checks and natural-spawn events](release-0.1.9-evidence/mineshaft-multiplayer-results.json), [owner view](release-0.1.9-evidence/mineshaft-pack-owner.png), [guest view](release-0.1.9-evidence/mineshaft-pack-guest.png). Reproduce with `tools/multiplayer/mineshaft_packs_0_1_9.py` in the disposable generated world.

These are controlled loopback tests with offline identities and Linux Mesa software rendering at 960×540, rather than a large-population or arbitrary network/GPU compatibility benchmark. The optional NeoForge early-loading window is disabled in the local client test configurations. Cloud authentication/public-key lookups still log expected failures. No mod crash, packet-handler failure or mixin application failure was found in the completed runtime logs.
