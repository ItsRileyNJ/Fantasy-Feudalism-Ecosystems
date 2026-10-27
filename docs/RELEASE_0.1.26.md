# Version 0.1.26 — Pig foraging and loose herds

Adult pigs dig approximately every two minutes while standing on podzol, rooted dirt or coarse dirt. Success chances are 80%, 50% and 15%, respectively. A successful dig produces exactly one mushroom: 80% brown or 20% red. Digging does not change the soil.

The three-second animation lowers and moves the snout while the front legs paw alternately. It is synchronized from the server, blends into and out of ordinary movement, and supports the native saddle model. Higher-priority native activities and player control interrupt it. Cooldowns are saved before a reward can be produced.

Piglets seek nearby dropped mushrooms and accept either color from players. Each mushroom removes 20% of remaining growth time. Consumption respects pickup delays, player-reserved drops, stack counts and Creative mode. Adults retain vanilla breeding foods.

Adult pigs loosely regroup once separated by more than about 15 blocks, then resume wandering within 11 blocks. Bred piglets remember both parents across saves and follow family until adulthood. Egg-spawned piglets can follow a nearby adult. No parent chunks are forced to load.

Item searches and herd searches are throttled and their subsequent deadlines aligned to individual entity IDs. Digging only inspects the ground beneath the pig; it never scans terrain for suitable soil.

## Validation

- All **231 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed. Ten pig tests cover exact soil probabilities, the 80/20 type split across seeded native drops, a one-item limit, interrupted digs, saved cooldowns, changed soil, both feeding paths, item contention and pickup restrictions, native foods/Creative mode, navigation, real breeding parents and spawn-egg AI. All ten sheep tests also pass in this combined build.
- All **four two-client dedicated-server checks passed**: a real spawn-egg pig animates on both clients while an idle pig keeps its normal pose; digging ends with a clean return to idle; Survival feeding consumes one item and removes twenty percent of remaining growth; resource reload and the adult-to-piglet transition render without errors. See [live evidence](pigs-0.1.26-live-evidence.json) and [owner screenshot](pigs-0.1.26-owner.png).
- Release build, optional multiplayer test JAR, Python syntax checks and whitespace checks passed. The production JAR contains no GameTest classes or multiplayer probes.

Final automated log: `run/compatibility-_0vd8pf5/ecosystems.log`. Earlier runs exposed two fixture issues: the native egg-spawn placement needed time to land before forcing a dig, and mock Creative players needed their native `instabuild` ability set. One unrelated existing guard-retreat test failed in an earlier run and passed in subsequent runs without production changes. The final run passed every required test.

The earlier sheep release and its separate two-client evidence remain documented in [0.1.25](RELEASE_0.1.25.md). Audio-device and online-authentication checks are limited by the disposable environment's missing OpenAL device and Minecraft authentication DNS. This validation does not claim a full rerun of Mercenaries' own 338-test suite or arbitrary modpack coverage.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.26.jar`

SHA-256: `95ba621195d503cb892b579254714cd7964e7e1423e59203aba26d1c119f5c07`
