# Current dedicated-server and two-client audit

Follow-up to [the mechanics/performance audit](DEEP_AUDIT_CURRENT.md), after the user accepted Minecraft's EULA. The disposable server runs Minecraft 1.21.1 / NeoForge 21.1.262 with the packaged Ecosystems release and unchanged supplied Mercenaries 1.15.7. Two real graphical clients use software Mesa on a local dummy display. A separate diagnostic JAR drives normal client interactions and observes server/client state; it is excluded from the release.

Ecosystems SHA-256: `c78e5d9e11958bde194bd03ba1cdfad7bf39118edde9a63fb00eb563a914ca4f` (202,303 bytes). Mercenaries SHA-256: `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.

## Completed multiplayer checks

- Fourteen core checks passed: competing bee capture/taming, spider ownership, 2,000-request silk and poison harvest races, full-inventory output conservation, native inventory moves, client resource reload, reconnect, full server restart and persisted cooldowns, active skeleton/stray/bogged crossbow rendering, and a 240-mob load.
- Mercenaries checks passed for contract recruitment races, companion alliances, shared guard storage and ammunition protection, synchronized orders, reserved-slot rejection, private highlights, resource reload, cursor-stack logout conservation, full-process persistence, cooldown persistence, horse mount/dismount, twelve repeated shared-inventory races, and graphical GUI scales 1/2/3 at 1280×720.
- Ten vanilla interaction checks passed: lead recovery after bee capture, real axe shield interruption and recovery, creeper `mobGriefing=false`, ordinary bone meal, fertilizer, milk effect removal/bucket return, native furnace/hopper processing of horse meat, both players opening the native furnace menu, and save/render health.

The first vanilla attempt passed eight checks but timed out opening the furnace after a teleport. Vanilla rejects use packets while teleport acknowledgement is pending. An isolated repeat opened the actual furnace successfully on both clients. The fixture now observes the destination before issuing the use packet; the full ten-check repeat passed. No production interaction code changed for this fixture issue.

Twelve further live night-combat/content checks passed: actual Wither/Slowness particles on both clients, raised-shield crouching and charge/melee transitions, Creative-target cleanup, phantom pact ownership/defense/sunrise expiry and Mercenary alliance recognition, reserved guard-slot rejection of the three bat-farming items, both creative tabs and all six item models, resource reload and save.

## Bat enclosure rendering

**All 23 enclosure checks passed**, including actual Bat Net capture/insertion for one/two/three bats, fourth-bat rejection, competing feed packets and cooldowns, fertilizer use, night flight within its radius, a clean saved-world process restart, original UUID conservation, dawn return, resource reload, and occupied-cabinet destruction/reinsertion.

The development renderer probe observes real `RenderLivingEvent.Post` calls for enclosure-only bat copies. It compares interpolated body/head/pitch plus every model part's position, rotation and scale against its first rendered frame. Each five-second sample requires at least 30 frames for every expected UUID on each client. It does not substitute synthetic entities or advance a test-only pose.

| Stored bats | Owner frames per bat | Guest frames per bat | Maximum pose change | Render partial tick |
| --- | ---: | ---: | ---: | ---: |
| 1 | 106 | 107 | **0** | 0 |
| 2 | 108 | 108 | **0** | 0 |
| 3 | 106 | 106 | **0** | 0 |

These checks ran after the normal restart, dawn return and resource reload. No client render errors occurred. Both final client screenshots were inspected; the [front view after dawn return](multiplayer-current-enclosure.png) also shows the three hanging occupants. Screenshots alone do not establish stability; the frame measurements above do.

## Live ranged combat and load

A bow skeleton hit the guard at 10 blocks and both clients observed the health change. Two 25-second crossbow observations at 27 blocks timed out; subsequent observation showed actual damage on both clients. A bounded repeat using seed 42 and a 120-second observation window synchronized its first observed hit after **49.775 seconds**. The original timeouts remain failures in the retained evidence. The longer observation establishes working damage/synchronization, not a 25-second hit guarantee or an accuracy improvement. Vanilla shot spread remains in use; the trajectory-cache optimization was independently bit-for-bit equivalent.

| Workload | Samples | Mean tick | p95 tick | Maximum tick |
| --- | ---: | ---: | ---: | ---: |
| 240 active mixed mobs + two clients | 706 | 26.565 ms | 94.578 ms | 501.166 ms |
| 180 active modified mobs + 20 mercenaries + two clients | 743 | 16.789 ms | 45.718 ms | 267.319 ms |

Both clients remained connected with advancing rendered frames and no recorded rendering failures. The first workload advanced 705 ticks over 35.626 seconds (~19.79 ticks/s). The second advanced 742 ticks over 35.833 seconds; its short-window count includes catch-up ticks and must not be interpreted as sustained capacity above Minecraft's nominal 20 TPS. The software-rendered clients share the cloud CPU allocation with the server, so these are bounded smoke tests, not a production capacity guarantee. Tail latency remains visible. The separate solver microbenchmark improved by 58.6%; that number is not a TPS gain.

## Additional visual observation

The first frog approach check observed the server's jump and water landing, and the observing client's rendered jump frames. The approaching client's render probe recorded none, so the overall visual assertion failed. Its saved screenshot showed a camera aimed downward across the water. A repeat with a higher camera pitch still missed the approaching client's rendered jump assertion. Both server jump/landing checks succeeded and the observing client recorded 8/15 frames respectively, but the cause of the approaching-client observation gap remains unresolved. Neither attempt is counted as a passing visual suite; no production frog code or assertion was changed.

The final server stop logged `All dimensions are saved`; both client processes and the owned Xorg display exited cleanly. The release JAR hash remained unchanged through the graphical checks. Python syntax compilation and `git diff --check` also passed.

## Limits

The headless Mercenaries mounted-retreat assertion failure and pre-start GameTest runner stall remain as documented in the deep audit. This multiplayer follow-up does not turn those incomplete suites green. One earlier unseeded chicken-flee timing failure also remains recorded. Public-key/session-service DNS is unavailable in this environment; the loopback-only test server uses offline fixture identities. Online authentication and physical audio are untested. No test can certify every vanilla interaction or perfect compatibility with arbitrary worlds and additional mods.

## Evidence and reproduction

[Compact JSON evidence](multiplayer-current-evidence.json) preserves check names, retained failures, load metrics and the per-UUID pose measurements. [Console evidence](multiplayer-current-evidence.txt) preserves the live suite output, including failed attempts. Full worlds, logs, control records and screenshots remain under `/workspace/multiplayer`; the temporary server, clients and display were stopped after the final save.

The existing disposable fixture uses `tools/multiplayer/control.py`, `suite.py`, `phase_two.py`, `mercenaries.py`, `mercenaries_restart.py`, `mercenaries_races.py`, `mercenaries_gui_scales.py`, `mercenaries_load.py`, `vanilla_audit_0_1_16.py`, `night_combat_0_1_15.py`, `ecosystems_0_1_15.py` and `bat_restart_0_1_15.py`. Some script/report names retain older version labels; the binaries tested here are the hashes at the top of this report. Run state-mutating suites sequentially against the three configured RPC control directories. Persistence phases require a normal save/stop, new server process and client reconnect; never run them concurrently with another suite. The extended ranged observation used `mercenaries_load.py --combat-timeout 120`; its default remains the original 25 seconds. `enclosure_stability.py` can repeat the three-bat rendered-pose sample after the enclosure fixture completes. EULA acceptance must be supplied by the person operating a new dedicated server.
