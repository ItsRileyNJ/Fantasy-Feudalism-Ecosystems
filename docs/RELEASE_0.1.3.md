# Fantasy Feudal Mobs 0.1.3

Sitting tamed regular and cave spiders now angle their large rear abdomen downward by 0.18 radians (about ten degrees), replacing the vertical offset in 0.1.2. Following restores the normal angle. The shared model resets for each rendered spider, keeping wild and following spiders neutral.

Targets Minecraft 1.21.1, NeoForge 21.1.262, and Java 21. Replace the older Mobs JAR on the server and every client.

Release: `fantasy_feudalism_mobs-0.1.3.jar` (79,458 bytes).

SHA-256: `688054213e2d2ab85d31fb1947dac3a3b686fa4fd405d09257159ea26186fd5e`.

## Validation

`build multiplayerTestJar` passed with JDK 21. The dedicated server and both graphical clients loaded byte-identical packaged release JARs alongside the supplied Mercenaries 1.15.7. The separate diagnostic harness and third-party mod are excluded from the release JAR.

All **three focused live checks passed**: sitting pitch and unchanged vertical position on both clients; repeated real sit/follow interactions without accumulating rotation or affecting adjacent wild/following models; and correct poses after both clients reloaded resources. Each scene included wild, following, and sitting spiders of both types. Screenshots were inspected to confirm the rear tips downward. Evidence: [results](release-0.1.3-evidence/results.json), [model poses](release-0.1.3-evidence/poses.json), [owner view](release-0.1.3-evidence/sitting-pose-0.1.3-owner.png), and [second-player view](release-0.1.3-evidence/sitting-pose-0.1.3-guest.png).

No render error was recorded by the harness. Tests used loopback networking and Linux Mesa software rendering, so they do not establish behavior on every graphics driver or mod combination.

The focused two-client check uses `tools/multiplayer/sitting_pose_0_1_3.py` against the disposable local fixture described in [the validation report](VALIDATION.md). This correction changes only client model posing; the broader gameplay results in [the 0.1.2 report](RELEASE_0.1.2.md) are historical and were not rerun for this cosmetic update.
