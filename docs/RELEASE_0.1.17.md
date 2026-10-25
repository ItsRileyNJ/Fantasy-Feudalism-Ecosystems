# 0.1.17 — cats, Rested, and Field Mice

## Player-facing changes

A stray cat or a cat belonging to the sleeping player can join their bed if it was within four blocks when sleep started. A cat ordered to sit keeps its order, and another player's cat cannot grant the effect. The cat must actually settle on the bed and still be there when the player wakes after the normal 100-tick sleeping threshold. A quick bed visit gives no bonus. Owned cats keep their vanilla resting behavior and morning-gift logic; strays use a separate resting goal.

Sleeping with a qualifying cat grants **Rested** for **six minutes (7,200 world ticks)**. Its inventory icon is **three blue Zs**. Rested halves incoming hunger exhaustion and multiplies food-based natural healing by 1.35. Native healing intervals, food/saturation requirements, and `naturalRegeneration` still apply. It does not boost potions or other direct healing. Milk removes the effect normally.

**Field Mice** are small white wildlife with pink tails, ears, feet and noses. Their body dimensions are 0.35 × 0.22 blocks, smaller than a silverfish, and they have two hearts. Cats acquire visible mice within fifteen blocks and kill them in one melee hit. Hunting yields to sitting orders, panic and sleeping. Mouse escape navigation runs at 90% of a hunting cat's movement speed, including the cat's native 30% sprint attribute modifier.

Each morning, occupied village home POIs and loaded village cats define a local census. Nearby home groups share a population budget to prevent overlapping counts. The village attempts to fill `floor(cats / 3)` living mouse slots near planted crops. It tries once during the first 1,000 ticks of the day, including dawn reached by sleeping, and respects `doMobSpawning`. Fewer than three cats permit no new mice. Mice already alive are not forcibly removed when cats leave. Saved reservations count unloaded mice; restarting or unloading chunks cannot create extra population slots. A missed/failed morning attempt waits until a later morning. Attempts are bounded and never force chunks to load.

Each mouse eats one growth stage from one reachable planted crop, then waits two minutes (2,400 world ticks). Each mouse has its own saved deadline. Crop eating respects `mobGriefing` and the normal entity-griefing hook. Mice flee players, cats, villagers and Mercenaries guards that approach within three blocks; spectators are ignored. They remember their crop and return after fleeing. Supported crops include ordinary CropBlock crops, stems, nether wart, cocoa, berry bushes and pitcher crops. Age cannot go below zero. Pitcher halves keep matching ages; reducing to a single-block stage cannot break the lower crop or create item drops.

## Validation

| Configuration | Result |
| --- | --- |
| Ecosystems alone, final sources | **175/175 required tests passed**; build and separate test-harness JAR successful |
| Ecosystems namespace with unchanged Mercenaries 1.15.7 | **188/188 required tests passed** |
| Dedicated server with both packaged mods and two graphical clients | **8/8 live checks passed**, including a normal process restart and both resource reloads |

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.17.jar`, **236,280 bytes**, SHA-256 `a1138120f9bd21ed1dc973021f463af15088135fdb56035bd672505b8be344e4`. The release contains the new model/texture and Rested icon; it contains neither Mercenaries code, development tests, nor the separate diagnostic harness. The Mercenaries JAR remains unchanged, SHA-256 `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.

Both actual clients slept with an owned cat and a stray, woke with synchronized six-minute effects, displayed the new icon, rendered the mouse, observed crop eating/fleeing/return/hunting, and preserved effects and mouse state through a clean server restart. The [Rested inventory screenshot](cats-0.1.17-rested.png) and [mouse screenshot](cats-0.1.17-mouse.png) were inspected. [Live JSON evidence](cats-0.1.17-live-evidence.json) and [console evidence](cats-0.1.17-evidence.txt) retain the results. Full logs/worlds remain in the cloud workspace. The final shutdown saved all dimensions; both clients and the owned dummy display were stopped. Python syntax checks and `git diff --check` passed.

The first exploratory run exposed a feeding-reach issue, then corrected fixtures for the GameTest player's Creative default and missing client-driven sleeping ticks. The final tests use Survival players and drive native sleeping ticks in the embedded fixture; actual networked clients separately verify normal sleep. Review also caught native sprint modifiers and multi-block crop updates, now covered by regressions. An initial live run passed its behavior checks with the fixture's daylight cycle frozen, but screenshots were dark/cluttered. The recorded repeat enables actual overnight day advancement and uses daytime clean views for the screenshots. No failed assertion was disabled or counted as a pass.

These are functional checks, not a measured production TPS improvement. Population/crop scans are bounded; the village census runs at most once per morning, and ordinary navigation/nearby entity queries handle behavior. No new recipes, drops, sounds or unrelated gameplay items were added.

The new regression tests cover real owned/stray navigation onto beds, eligibility/radius/sit rules, real waking, hunger exhaustion, both native healing paths, unrelated healing, milk, mouse health/size/speed/one-hit kills, hunt range and navigation, crop stages and gamerules, full 2,400-tick meal timing, saved cooldowns, crop return, villager flight, daily village quotas and unloaded reservations. A combined-only case exercises flight from an actual Mercenaries guard.

Earlier Mercenaries native-suite mounted-retreat failure and pre-start test-runner stall remain documented in [the previous audit](DEEP_AUDIT_CURRENT.md). This release's combined test count covers the Ecosystems namespace and its integration cases; it does not claim that Mercenaries' separate native suite is fully passing. No third-party implementation is bundled. Cloud online authentication and physical audio remain outside the local offline-client checks.

## Reproduce

```sh
python /workspace/toolchains/gradle.py runGameTestServer build multiplayerTestJar -PgameTestDirectory=run/new-cats-standalone
python tools/check_compatibility.py /absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar --suite ecosystems
```

Use a fresh directory for each GameTest run. Outside this cloud environment use the repository Gradle wrapper with Java 21. The live fixture uses `tools/multiplayer/cats_0_1_17.py`, followed by a normal server save/stop/restart, client reconnect and `--resume`. Its control directories and graphical runtimes follow the existing multiplayer harness setup. State-mutating suites must run sequentially.
