# 0.1.20 — mouse checks every twenty seconds

The user's final clarification applies the twenty-second interval to **crop searching and feeding only**, keeping fleeing responsive. The release also includes the previously requested white-and-pink Field Mouse Spawn Egg.

## Changes

- Crop searches, remembered-target checks and failed feeding retries use **400 world ticks** at normal speed. A successful meal still sets its saved **2,400-tick** cooldown. Initial checks are staggered; ordinary movement and lightweight timer comparisons continue ticking normally.
- Crop seeking checks remembered crops before scanning an area, and a full scan can retain a young-crop fallback instead of scanning the same area twice. Empty-field searches are staggered and share a maximum of **two full AI crop searches per level per tick**. Denied budget requests retry without doing a full area scan. Cached crop stages are looked up by property name instead of enumerating every state property.
- Threat detection runs separately, with an immediate first check and a ten-tick polling deadline afterward, subject to vanilla goal scheduling. It retains the three-block trigger, actual escape paths and unchanged movement speeds.
- Each village finds a crop once for its morning attempt. Morning work performs at most **one village crop search and one mouse spawn per level per tick**, checking current cat numbers and existing reservations. Queued jobs are saved, resume after loading, stop outside the dawn window and cannot restart the daily census. Existing morning quotas, `doMobSpawning`, unloaded reservations and `mobGriefing` remain in effect.

## Functional verification

**179/179 standalone tests passed**, with a successful release build. **192/192 Ecosystems/integration tests passed** with the supplied unchanged Mercenaries 1.15.7 JAR. Added checks verify a denied crop is not retried before twenty seconds, feeding resumes when permission returns, sixteen mice all receive opportunities to feed, and a six-cat morning is split across ticks and survives a saved-job reload without duplication. Existing tests retain the full two-minute meal cooldown, flee/hunt behavior, crop returns, sounds, Rested and vanilla interactions.

One exploratory run timed out on the existing villager-escape assertion; a diagnostic repeat passed. The final implementation keeps the first threat check immediate. An early new fairness fixture incorrectly assumed one mouse would exclusively choose each plant. Its final assertion checks that every mouse actually feeds; shared crop choices are valid. Both exploratory failures are retained in the console evidence, rather than presented as passing runs.

## Controlled load comparison

Compared an immutable copy of **0.1.19** with **0.1.20** on a disposable dedicated server: Minecraft 1.21.1, NeoForge 21.1.262, Java 21, Mercenaries 1.15.7, four CPU cores of quota and a 2 GiB heap. The same diagnostic JAR, arena, mouse count and per-mouse random seeds were used. Chunks were force-loaded so mice continued ticking with no connected players. No graphical clients or builds ran during the measurements. JFR was enabled for both runs.

Each scenario used **200 live mice**, two rounds, a five-second initial/settling window and a subsequent roughly twenty-five-second measurement. Every recorded window retained all 200 mice; gains did not come from deleting or disabling them. The threat scenario adds twenty stationary villagers. Arena creation/command execution is outside the measured tick hook; queued settling work can enter the initial window. Recorded timings use the server pre/post tick hooks and exclude the file-control handler itself.

| Scenario | Weighted mean tick, ms: before → after | Worst per-run p99 tick, ms: before → after | Ticks over 50 ms in measured windows: before → after |
| --- | ---: | ---: | ---: |
| Crop damage disabled | 1.81 → 1.81 | 3.28 → 5.19 | 0 → 0 |
| No crops available | 4.67 → 3.58 | 65.93 → 6.46 | 20 → 0 |
| Normal foraging | 1.93 → 1.35 | 9.06 → 4.92 | 2 → 0 |
| 20 nearby villagers | 2.66 → 2.54 | 5.65 → 5.00 | 0 → 0 |

The repeated empty-field search stalls improved: worst-run p99 fell from **65.93 ms to 6.46 ms**, and the **20** measured ticks exceeding the 50 ms budget fell to **zero**. Across all eight optimized measured windows, no recorded tick exceeded 50 ms. This is evidence for these workloads, not a guarantee for arbitrary servers.

With crop damage disabled, permission checks fell from **203,200 to 400** across the two measured windows (about **99.8% fewer**). Overall mean tick time in that case was essentially unchanged, so the check-count reduction must **not** be described as a corresponding overall speedup. Observed counts depend on the windows' phase within the twenty-second polling cycle.

A separate crop-free village test with 96 stationary cats repeated five morning attempts. Median synchronous morning work fell from **44.99 ms to 7.66 ms**; maximum of those five samples fell from **82.43 ms to 46.28 ms**. This is a small synthetic test, not a village-capacity guarantee.

## Remaining limits

The optimized initial mass-spawn/settling windows still recorded **five ticks over 50 ms**, with a maximum of **158.40 ms** (baseline: eleven and 284.84 ms). Startup latency is therefore **not eliminated**. Native world/chunk/collision work, JVM warmup, scheduling and profiling overhead are present; their individual contributions to that maximum were not isolated. Cold windows are not interchangeable with the stabilized measurements above.

JFR recorded 935 server execution samples before and 802 after. First mod frames in `cropAge`/crop-target validation/search were much less common after the change; native collision and chunk work remained prominent. Counts are sampling observations, not exact CPU percentages. JFR's heap-inspection collection at recording boundaries is also present in the raw recording, so recording-wide GC maxima are not steady-state tick measurements.

These are server-only tests. They do not establish client FPS, network latency under real players, or perfect compatibility with every modpack. The separate Mercenaries native suite's earlier mounted-retreat/runner issues remain documented in the previous audits; the passing count above is the Ecosystems namespace and its integration cases.

## Artifact and evidence

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.20.jar`, **318,494 bytes**, SHA-256 `deb9e0692db95a4fb487cec25580c59f55ba241c20f73fd621eda84e54215095`. The packaged constants were checked as 400 and 2,400 ticks; the spawn-egg model and translation are packaged. Development tests and the diagnostic harness are excluded.

[Raw measurements and artifact hashes](mice-performance-0.1.20.json) · [console evidence](mice-performance-0.1.20-evidence.txt). Full JFR recordings and parsed events remain under `/workspace/performance-audit/mice/`. The test server was saved and stopped cleanly, and the test force-load tickets were removed.

For reproduction, build the source and run `tools/multiplayer/mouse_performance.py --label <name> --rounds 2` against the prepared local harness. This fixture **clears its arena and force-loads chunks; use a disposable test world**. Use separate immutable JAR copies, stop the server before changing its runtime JAR, and keep other builds/clients stopped while measuring. Functional checks use the ordinary GameTest command with fresh world directories; `tools/check_compatibility.py ... --suite ecosystems` requires at least 192 passing cases for this version.
