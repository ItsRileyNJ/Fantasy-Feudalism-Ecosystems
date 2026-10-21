# Validation

This report records the earlier Mobs 0.1.0 validation. See [the current Mobs 0.1.1 + Mercenaries report](COMPATIBILITY.md) for the compatibility release.

Validated on 2026-10-21 with JDK 21.0.12.1, Minecraft 1.21.1, NeoForge 21.1.262, and Gradle 9.2.1.

## Gameplay regression suite

```sh
python /workspace/toolchains/gradle.py runGameTestServer build
```

**46 GameTests executed, 46 passed, 0 failed; BUILD SUCCESSFUL.** The ordinary JUnit `test` task has no sources; these checks execute in the actual NeoForge GameTest server. See [the recorded result](validation-evidence/gametests.txt).

The original 36 checks cover capture/taming and bottle returns, raw-food healing, companion attributes and persistence, independent harvest cooldowns, combat effects and exclusions, fire damage, three-block jumping and actual path traversal, follow/teleport, neutrality, swimming prevention, wild prey, crossbow AI/projectile speed/variant effects, skeleton targeting, Enderman disengagement, zombie variation, phantoms, creeper explosions and vegetation/configuration, mossy blocks, spawn entries and cluster caps, armor distributions and variant overrides, Blaze dashing/configuration, silverfish spawning/fleeing/combat, and loot.

Ten multiplayer regression checks cover stale capture references, shared harvest cooldowns between players, dead-spider harvesting, spectator mutation prevention, ownership protection, AI re-entry, projectile speed on re-entry, companion index updates/removal, the actual entity-data packet codec, and full-inventory item conservation.

A repeat run exposed a test assumption: safe following teleports choose random candidate positions, and all ten candidates in one search can be unsuitable. The test now seeds its RNG and permits the goal's normal subsequent retries. Production teleport behavior and safety checks were retained. The expanded test template also keeps long-range targets inside their own test arena.

## Dedicated server and two graphical clients

The dedicated server used NeoForge's production `forgeserver` launch. Two separate graphical NeoForge client JVMs, named FFMOwner and FFMGuest, joined over loopback. Clients used the NeoForge development runtime with the **packaged release JAR** loaded from each `mods` folder; production source folders and GameTests were excluded from client mod loading. A separate development harness observed state and sent real interaction/container packets through each client's normal game mode.

The test server bound only to `127.0.0.1:25577`, used a disposable world, and ran in offline authentication mode. The user explicitly accepted Minecraft's EULA for this test server. Graphics used Xorg's dummy display and Mesa llvmpipe software rendering at 1280×720.

**16 live checks passed**, recorded in [multiplayer-results.json](validation-evidence/multiplayer-results.json):

- Both clients join and synchronize mod registries/configuration.
- 200 competing bee-capture requests create one Bee Bottle and consume one glass bottle.
- Real taming synchronizes UUID ownership, sitting, health, and movement attributes to both clients. Another player cannot take ownership by feeding.
- 2,000 competing poison-harvest requests create exactly one potion and consume one bottle, with matching client/server inventories.
- 2,000 competing silk-harvest requests produce one 1–4 string harvest and spend one shears durability. The separate poison cooldown does not block silk.
- Full inventories drop exactly one harvest output and consume exactly one input under competing requests.
- Inventory pickup, placement, and shift-click preserve item counts and agree with the server, including the cursor stack.
- Both clients reload resources without a missing Bee Bottle model or observed render exception.
- Disconnect/reconnect restores inventories and companion state.
- Normal server stop, world save, process restart, and both clients reconnect preserve companion UUID, owner, sitting order, both cooldown timestamps, and player inventories. Restarting cannot bypass the harvest cooldown.
- Both clients render skeleton, stray, and bogged crossbow use with finite model rotations and advancing frames. [Inventory screenshot](validation-evidence/inventory.png), [owner view](validation-evidence/crossbows-owner.png), and [guest view](validation-evidence/crossbows-guest.png) were visually inspected.
- Explicit vanilla logout closes each client connection in under half a second, without waiting for a server timeout.
- A bounded load run spawns 240 active mobs across spiders, cave spiders, skeletons, strays, bogged, zombies, husks, silverfish, and blazes while both clients remain connected. Clients keep rendering, server ticks advance, and control requests remain responsive. Entity counts can fall through normal combat or silverfish infestation.

The isolated load check completed 710 server ticks in 35.15 seconds (approximately 20 TPS). Across 711 tick samples, mean processing time was **12.32 ms**, the 95th percentile was **24.23 ms**, and the maximum was **81.98 ms**. No Gradle or GameTest process ran during that measurement. This was a brief stability check, not a long-duration capacity benchmark. [Load screenshot](validation-evidence/load-owner.png).

The audit fixed removed/dead-entity interactions, moved silverfish goal registration into construction to prevent accumulation on re-entry, and replaced repeated full-world companion searches with a level-scoped UUID index that updates on state changes and entity removal.

## Repeating the live checks

Build the optional harness separately:

```sh
./gradlew multiplayerTestJar --max-workers=2
```

The helper mod is `build/multiplayerTest/ffm-multiplayer-test-1.jar`. It is **not included in the release JAR**. Use it only in a disposable test server and two matching test client instances alongside the release mod. The helper can create/remove entities, replace inventories, change world blocks, and save the test world. It adds no production commands or network protocol.

1. Provision a NeoForge 21.1.262 dedicated server and two NeoForge 21.1.262 graphical clients. Accept the EULA for the test server and use a separate world. For this fixture use server address `127.0.0.1:25577`, offline test identities FFMOwner/FFMGuest, survival mode, and disabled secure-profile enforcement. Keep the offline fixture bound to loopback.
2. Set `FFM_TEST_ROOT` for the Python scripts (default `/workspace/multiplayer`). Give each JVM `-Dffm.test.control=<absolute-root>/control/server`, `/control/owner`, or `/control/guest` respectively. These are local JSON control directories, not network endpoints. Clear old command/response files before launching a fresh JVM.
3. Start the server and both clients with release and helper JARs in their `mods` folders. Enable `-Dmixin.debug.verify=true -Dmixin.debug.countInjections=true`. On a graphical Linux desktop, use native OpenGL; the cloud fixture uses `DISPLAY=:97` and software Mesa. Client simulation distance must be at least 5.
4. Run `python tools/multiplayer/control.py` to prepare the test platform and connect both clients, then `python tools/multiplayer/suite.py` for live interactions, resource reloads, inventory checks, and reconnects. This saves `restart-state.json` and `results.json` under the test root.
5. Disconnect both clients through the helper, stop the server with its normal `stop` command, and confirm world-saving completion. Restart the same server/world with old control request/response files removed; leave the clients on their title screens. Run `python tools/multiplayer/phase_two.py` to reconnect, verify persistence, inspect crossbow rendering, and run the bounded load check.
6. For a load measurement without concurrent build/GameTest work, run `python tools/multiplayer/load.py` after those processes finish. Preserve results, screenshots, and runtime logs. Run `python tools/multiplayer/logout.py` to verify explicit clean logout of both clients, then stop the server normally.

In the current cloud snapshot the already prepared launcher is `/workspace/multiplayer/launch.py` (`server`, `owner`, or `guest` argument). The official installer originally failed on a blocked legacy metadata hostname; preparation continued using the official version manifest, verified mapping/library checksums, and the installer's remaining processors. Runtime logs and installer preparation scripts remain under `/workspace/multiplayer`. Live processes do not survive environment publication and must be started again when needed.

## Scope and remaining limits

No mod crash, watchdog termination, mixin application failure, missing Bee Bottle model, invalid observed skeleton pose, inventory duplication, client/server inventory divergence, or GUI defect was observed in these completed checks. This is evidence for the tested cases, not a guarantee for every server or modpack.

Online account authentication and secure chat were not validated: the cloud network blocked the Minecraft services public-key fetch, which logged an authentication-service error and caused chat-session warnings in this local offline fixture. Startup also emitted loader asset-URL and vanilla shader-sampler warnings. An initial client fixture set simulation distance below the supported minimum; its options were corrected and the clients restarted. The initial helper also unloaded client worlds without closing their sockets, causing timeout messages during scripted disconnects; it now uses the same connection-close sequence as Minecraft's menu, verified with both clients (0.41 s and 0.32 s logout). These environment/harness messages are distinct from mod failures. An initial simultaneous GameTest/load run produced a server overload warning; the isolated load result is recorded separately rather than treating that run as a clean performance benchmark.

Testing is limited to two local clients, a short bounded load run, Linux software graphics, and this Minecraft/NeoForge version. Physical GPU/driver combinations, other operating systems, high network latency/packet loss, large public servers, compatibility with unrelated mods, and long-duration production load remain untested.

The packaged mod is checked for its Java 21 entry point, expanded loader metadata, all declared common/client mixins, and Bee Bottle resources. GameTest classes, test structures, and the separate live-test harness are absent from the release JAR.

Release JAR SHA-256: `c5a9475b706b5a4f82c1504c374426ed7ea593eec511b521ddfafe7a55d8c65a`. The server and both clients loaded identical release JAR bytes.
