# Fantasy Feudal Mobs 0.1.10

A living tamed wolf that is ordered to sit and actually seated protects sheep from wild wolves and chickens from foxes within **24 blocks of the livestock**. Distance uses a sphere, including the exact boundary. Standing or wild wolves do not guard. Protection checks new targets, active hunts and direct melee damage, so sitting a dog during a chase interrupts the hunt before a hit can bypass protection.

The deterred wild wolf's entire persistent pack receives its normal **24,000-tick / one-Minecraft-day** hunger cooldown. A deterred fox receives an individual saved cooldown of the same length. Foxes now also receive that cooldown after successful ordinary food-prey kills; they cannot immediately switch to rabbits, schooling fish or baby land turtles while fed. Defensive non-food combat and tamed wolf owner defense remain available.

Only the nearest eligible guard pays for a stopped hunt: **two health points / one heart**, with a minimum remaining health of one. The cooldown is committed before other pack members can retry, preventing repeated target scans from draining extra health. Idle guarding and avoidance checks cost no health. Fatigue directly adjusts ordinary wolf health without invoking damage, so it creates no hurt flash, damage sound, knockback, retaliation or standing. The existing health-based tail angle drops, and normal owner feeding restores health and the tail angle.

Deterred wild wolves and foxes also walk away from protected sheep/chickens within eight blocks using standard avoidance navigation. This goal runs only without a combat target, never applies to tamed wolves, and retains higher-priority survival behavior. It is installed once per entity even when a level is rejoined. Guard lookups used for navigation are read-only and never charge fatigue.

Install `fantasy_feudalism_mobs-0.1.10.jar` on the dedicated server and every client, replacing older Mobs JARs. Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Mercenaries 1.15.7 remains separate and optional.

Release size: **140,075 bytes**. SHA-256: `7978e83e2f9ea910f3451c1519bdb7494b7fd49a7bde5c56765a62137af3298a`.

## Automated validation

- **113/113** required standalone server GameTests passed.
- **123/123** required server GameTests passed with Mercenaries 1.15.7.
- Release and separate diagnostic builds passed. All 71 packaged main classes match compiled output; GameTests, diagnostics and third-party JARs are excluded.

Nine new regressions cover spherical range and exact boundaries; seated/tamed requirements; one fatigue event per pack and only one charged guardian; repeated-scan protection; normal tail movement and feeding; interruption of an existing world-ticked pack hunt; fox cooldown save/load and expiration; clearing an active fox stalk; nonlethal fatigue at one health; direct-melee interception; unchanged tamed and defensive targeting; no idle fatigue; a real unguarded fox kill; and both predators physically retreating through ordinary navigation. The rest of the previous wolf, livestock, spider, silverfish, inventory and optional Mercenaries checks still pass.

Reproduce with JDK 21: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. Add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory` for combined testing.

Evidence: [GameTest outcomes](release-0.1.10-evidence/gametests.txt) and [artifact manifest](release-0.1.10-evidence/manifest.json).

## Dedicated server and two graphical clients

All **ten live checks passed** using byte-identical final release JARs and Mercenaries 1.15.7 on the server and both graphical clients:

- An actual owner interaction seated a healthy dog. Three wild wolves in a natural spawn group attempted one sheep hunt; all received the same saved hunger deadline, the sheep remained unharmed, and the guard lost exactly one heart.
- Both clients received the lower health and rendered the lower tail. Passive per-tick probes recorded zero hurt-animation ticks for the guard, and passive sound probes recorded no wolf hurt/armor-damage sound. The guard stayed seated, with no repeated health loss during another sixty ticks of AI scans.
- One real survival-mode feeding consumed one beef and restored full health and the original rendered tail angle on both clients.
- With the dog standing, a fox acquired a chicken through its ordinary AI. Sitting the dog interrupted that active hunt, started the fox's hunger cooldown, and charged one fatigue event without harming the chicken. Further scans did not resume the hunt or drain the guard.
- A new fox hunted and killed a chicken placed twenty-five blocks from the dog, proving the boundary does not protect distant livestock. That real kill started the normal fox meal cooldown without tiring the dog.
- Another nearby fox walked away from a guarded chicken with no other predator or survival player nearby to cause the retreat. The chicken remained unharmed.
- Both clients remained connected with no recorded render error, no guard hurt animation or damage sound, and a normal survival inventory opened and closed successfully.

The diagnostic harness supplied entities, natural wolf grouping and ordinary fox spawn initialization, but did not assign predator targets, attacks, damage, cooldowns, guard health or model poses. Sitting and feeding used real client interaction packets. Livestock was stationary for observation. The optional brighter profile screenshot applies night vision only to the viewing player after the checks.

Evidence: [ten results with server/client snapshots and sound/pose counters](release-0.1.10-evidence/multiplayer-results.json), [one-heart fatigue during the hunt](release-0.1.10-evidence/guard-dog-fatigue.png), [seated tired guard profile](release-0.1.10-evidence/guard-dog-profile.png). Reproduce using `tools/multiplayer/guard_dogs_0_1_10.py` and the separate local diagnostic harness.

These are controlled loopback tests with offline identities and Linux Mesa software rendering at 960×540. They do not establish compatibility with every mod, graphics driver or network condition. Local clients disable NeoForge's optional early-loading window; cloud authentication/public-key lookup failures are expected. No mod crash, packet-handler failure or mixin application failure was found in the completed runtime logs.
