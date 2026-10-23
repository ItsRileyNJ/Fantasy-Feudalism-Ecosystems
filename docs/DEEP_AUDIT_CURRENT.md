# Vanilla, Mercenaries and performance follow-up audit

This audit covers the current Ecosystems working tree, including the enclosure display fix, with the unchanged user-supplied Mercenaries 1.15.7 JAR. Intentional gameplay changes remain defined by `SPECIFICATION.md`. The checks exercise vanilla mechanics affected by those changes; they cannot establish every vanilla interaction or arbitrary modpack configuration.

## Fixes

1. `SpiderGoals.mayAttack` now rejects Creative and Spectator players for both wild and tamed spiders. Previously its shared permission check could accept those modes on explicit targeting and retaliation paths.
2. A tamed spider now honors the server's PvP setting even when its owner is offline. Previously the PvP check was inside the `owner != null` condition. Normal survival targeting remains available when PvP is enabled.
3. The skeleton projectile solver caches the 121 constant drag powers used by its existing bounded search. Target selection, spread, speed, gravity compensation and search precision are unchanged.

Six new GameTests cover the two targeting fixes and native furnace/hopper operation for both vanilla beef and horse meat, brewing, redstone lamp updates and shearing. They use actual ticking block entities and the normal player interaction route. Mode-sensitive tests use real `ServerPlayer` instances: Minecraft's `makeMockServerPlayerInLevel` permanently overrides `isCreative` and `isSpectator`, even after `setGameMode`.

## Results

| Check | Result |
| --- | --- |
| Ecosystems alone, final sources | **162/162 required tests passed**, build and diagnostic JAR successful |
| Ecosystems namespace with Mercenaries | **174/174 required tests passed**, build and diagnostic JAR successful |
| Mercenaries namespace, first follow-up run | 261 passed, 77 pending/unrun; stopped after confirmed pre-start runner stall |
| Mercenaries namespace, diagnostic repeat in a fresh world | 260 passed, 1 failed, 77 pending/unrun; watchdog stopped the same pre-start stall |
| Graphical client with both packaged mods | Client startup and resource reload completed; no reported rendering exception, missing model or Mixin application/injection failure |
| Trajectory comparison | **10,000 seeded cases matched bit for bit**, including unreachable trajectories |

Neither incomplete Mercenaries run is a passing suite. Its repeated original failure is `mountedcrossbowretreatrestartsacrossgrassystep`, already reproduced with Mercenaries alone in the [earlier comparison](COMPATIBILITY_CURRENT.md). The native test requires X > arena X + 24 even when a diagonal retreat is safe. No original assertion was changed or disabled.

The stall is `anormallyactivenitwitbecomesaguardthatautonomouslyfights`: the world advances while the test remains `started=false`, `startTime=0`, `testTick=0`. Minecraft's GameTest runner waits for the structure's chunks to become entity-ticking before it starts this test; consequently its own timeout cannot fire. The exact ticket/readiness cause remains unresolved. A similar stall was recorded before this audit in the release findings. The watchdog detects repeated read-only diagnostics, stops the run and reports pending cases as unrun. It never starts a pending test, changes assertions or counts an aborted run as success.

The first audit attempt also had an intermittent failure in the existing unseeded chicken-flee check. Later standalone and combined runs passed it. Its assertion remains unchanged, and failure messages now include positions/path details. No production chicken behavior was changed; its occasional timing/route failure remains a retained finding rather than a claimed fix. Inspection of the saved failed-run world showed that the surviving chicken subsequently moved well away from its original position.

## Performance evidence

The isolated solver benchmark alternates baseline and optimized runs in one JVM, discards two warmup rounds and takes the median of six 100,000-solve rounds. It ran after the graphical client and GameTest servers stopped.

| Measurement | Baseline | Optimized |
| --- | --- | --- |
| Median trajectory solve | 2,561.0 ns | 1,060.7 ns |

That is a **58.6% reduction in this solver microbenchmark**, not a measured server TPS improvement. Identical results were checked against the pre-change compiled implementation using Java 21.0.13.

Java Flight Recorder also sampled a complete standalone gameplay suite. Of 1,531 server-thread execution samples, the largest leaf counts were native block-neighbor updates, collection iteration and chunk lookup/save work. The first Ecosystems frame appeared in only six sampled server stacks (three in the equipment-distribution test's `WitherCombat.equip` workload). This short fixture-heavy recording does not establish production capacity or rule out other bottlenecks. It recorded 54 GC events, 1.144 seconds of cumulative pauses and a maximum individual pause of 46.42 ms. No unrelated vanilla chunk/world code was changed based on test-fixture costs.

## Errors reviewed

| Message/failure | Diagnosis |
| --- | --- |
| Spider player-mode/PvP checks | Production permission bugs addressed above; final regressions pass |
| Initial shearing audit failure | Test called the entity directly; NeoForge routes shearing through the player/item interaction hooks. Corrected fixture passes |
| Initial offline-PvP fixture's positive control | The default GameTest mock always reports Creative. Replaced with a real player and verified actual modes |
| Missing `server.properties` in a fresh GameTest directory | Native first-run initialization; the file is generated and the server continues |
| Minecraft public-key fetch failure | Cloud authentication-service access unavailable; headless gameplay checks continue. Online authentication remains untested |
| Missing optional Mercenaries Mixin target warnings in standalone runs | Mercenaries is optional and absent in that configuration; combined runs load its targets |
| Native command ambiguity, asset URL, shader sampler and deprecated API warnings | Loader/vanilla/development warnings; no observed Mixin injection failure, missing release model or application crash |
| ALSA/OpenAL sound-device failure in the graphical cloud client | No physical audio device is available; sound hardware behavior remains untested |
| Mounted-retreat assertion and pre-start runner stall | Retained failures/blockers described above; not suppressed |

## Reproduce

```sh
# In the checkout root; the ordinary test task has no JUnit cases.
python /workspace/toolchains/gradle.py runGameTestServer build multiplayerTestJar
python tools/check_compatibility.py /absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar
python tools/benchmark_ballistics.py --baseline 51c2ee8186c493acb3aa742b4f635ae18824a1b7
```

The compatibility command retains separate fresh worlds/logs, requires at least 174 and 338 passing required cases, propagates failures and bounds stalled runs. It uses a single-use Gradle daemon so timed-out processes can be stopped with their invocation. It remains a manual release check; ordinary GitHub CI does not supply Mercenaries.

The benchmark needs the built development classes and generated GameTest classpath manifest. It compiles the requested Git revision into a temporary directory and compares it with the current compiled solver without modifying the checkout. Rebuild before benchmarking source changes. The accepted Mercenaries hash remains `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.

Current release: `build/libs/fantasy_feudalism_ecosystems-0.1.16.jar`, **202,303 bytes**, SHA-256 `c78e5d9e11958bde194bd03ba1cdfad7bf39118edde9a63fb00eb563a914ca4f`. No Mercenaries code or development test classes are bundled. [Recorded evidence](deep-audit-evidence.txt) preserves the summaries and measured benchmark output.

## Remaining validation

The user subsequently accepted Minecraft's EULA, and the disposable dedicated-server/two-client follow-up is recorded in [the multiplayer audit](MULTIPLAYER_AUDIT_CURRENT.md). It uses the release hash above and the unchanged supplied Mercenaries JAR. Physical audio and online authentication remain untested.

The compatibility suite is not fully green while the Mercenaries assertion and runner blockage remain. Previous release multiplayer reports are historical evidence for their earlier binaries.
