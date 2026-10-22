# Fantasy Feudal Mobs 0.1.11

Horse grazing and following a player holding golden horse food previously shared priority 3. Minecraft only lets a strictly higher-priority goal replace a running goal, so an active food-following goal could indefinitely block grazing. Grazing now runs at priority 2, above food-following. Panic and riding retain priority, and love mode ends grazing so the ordinary breeding goal can proceed.

The old per-scan random grazing roll also made observations depend on repeated luck. Grazing now schedules a deadline: **20–40 seconds for eligible adults, 10–20 seconds for foals, and 5–10 seconds for adults ready for automatic breeding**, at normal tick speed. Becoming breed-ready shortens an already scheduled ordinary interval. Each grazing animation still lasts forty ticks and uses the native synchronized horse head pose. Horses still require grass at their feet, and normal riding, water, danger and mobGriefing restrictions remain in place.

Automatic breeding retains the requested **fourteen elapsed Minecraft days: 336,000 world ticks, or four hours forty minutes at normal speed**. Two hours is only six Minecraft days and does not reach the first deadline. Grazing happens independently before that deadline. Saved breeding deadlines, normal manual breeding, herd inheritance and triple natural foal growth remain unchanged.

An isolated baseline run of 0.1.10 confirmed the timing distinction: two natural adults grazed repeatedly over 144,013 ordinary world ticks without a foal, then produced a foal at tick 436,520 against an unmodified saved deadline of 436,423. Only `/tick sprint` accelerated processing; no timer, love state, goal selection or offspring was assigned. The fix addresses grazing starvation and observation reliability while retaining that breeding schedule.

Install `fantasy_feudalism_mobs-0.1.11.jar` on the dedicated server and every client, replacing older Mobs JARs. Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Mercenaries 1.15.7 remains separate and optional.

Release size: **140,350 bytes**. SHA-256: `e98ade627540bd8db8482eaa97ff2b80d4e8bfdf90f3678747b18e0a83c94163`.

## Automated validation

- **114/114** required standalone server GameTests passed.
- **124/124** required server GameTests passed with Mercenaries 1.15.7.
- All 71 packaged main classes match compiled output. GameTests, diagnostics, templates and third-party JARs are excluded from the release.

The new food-following regression first confirms a real golden-carrot temptation goal is running, then waits for ordinary world AI to start and finish grazing, consume grass and retain the original future breeding deadline. The existing grazing/freezing and complete automatic-breeding integration tests no longer repeatedly tick the goal selector or force a grazing start. They now wait for normal scheduling, mate selection and birth, checking one reserved pair, one untamed foal, herd membership and exactly two reset parent deadlines. Direct goal ticks remain only in focused block-consumption/no-griefing tests. The existing deadline persistence, manual feeding, riding/foal exclusion, growth and meat/cooking tests also pass.

Reproduce with JDK 21: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. For combined testing, add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`.

Evidence: [GameTest outcomes](release-0.1.11-evidence/gametests.txt) and [artifact manifest](release-0.1.11-evidence/manifest.json).

## Dedicated server and two graphical clients

All **seven live checks passed** with byte-identical final release JARs and Mercenaries 1.15.7 on the server and both graphical clients:

- Two adult horses finalized as one natural herd started with the full saved fourteen-day deadline. While the owner continuously held a golden carrot, ordinary AI started grazing after **595 ticks / 29.75 seconds**. Both clients simultaneously received the eating flag and rendered the native head-down pose. The food was neither used nor consumed.
- Both adults grazed and converted real grass to dirt before breeding readiness. Food-following was then released, and they roamed a grass pasture normally.
- After two simulated hours (144,000 normal world ticks), they had repeatedly grazed but produced no premature offspring, and their original saved deadlines remained unchanged.
- After the full cycle, exactly one untamed foal was born at world tick **776,175**, following the unmodified deadline of **775,930**. Both parents received exactly `birth tick + 336,000` as their next deadline. The foal inherited the herd without taming either parent. The complete observation spanned **342,005 elapsed world ticks**.
- Both graphical clients tracked the same single foal without a recorded render error. Further world ticks produced no duplicate foals, and a normal survival inventory opened and closed correctly.

Vanilla `/tick sprint` accelerated tick processing for the long calendar portions. Every ordinary AI/world tick still ran. No horse's timer, love state, mating partner, goal selection, grazing start, damage or offspring was assigned by the harness. The initial visible grazing phase ran at normal tick speed. The pasture was fenced and loaded, with normal grass spreading, natural daylight progression, mobGriefing enabled and unrelated mob spawning disabled. Daylight and viewing-player night vision were adjusted only after the checks for the final family illustration.

Evidence: [seven results with counters, native model poses and family snapshots](release-0.1.11-evidence/multiplayer-results.json), [naturally born foal and herd](release-0.1.11-evidence/horse-autonomous-family.png). Reproduce with `tools/multiplayer/horse_autonomy_0_1_11.py` and the separate diagnostic harness. The earlier baseline clock observations are preserved alongside these results.

These are controlled loopback tests with offline identities and Linux Mesa software rendering at 960×540. Accelerated tick processing is a calendar/AI verification, not a normal-play performance benchmark. Local clients disable NeoForge's optional early-loading window; cloud authentication/public-key lookups and fixture-teleport movement warnings may appear. No mod crash, packet-handler failure or mixin application failure was found in the completed runtime logs.
