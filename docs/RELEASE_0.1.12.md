# Fantasy Feudal Mobs 0.1.12

Automatic horse breeding now uses **150 minutes / two hours thirty minutes / 180,000 elapsed world ticks** at normal tick speed. New horses, both parents after a birth and newborn foals use that interval. Grazing, manual golden-food breeding and triple natural foal growth retain their existing behavior.

Existing horses preserve the time already waited. Their old 336,000-tick deadline is shortened by 156,000 ticks once, keeping the original cycle start. A horse already two hours into the old cycle has thirty minutes left. A horse already past 150 minutes becomes eligible immediately, subject to normal age, grazing and mate requirements. A saved interval marker prevents repeated access or reloads from shortening the deadline again. Elapsed time while unloaded continues to count as before; offline/paused world time does not advance.

Install `fantasy_feudalism_mobs-0.1.12.jar` on the server and every client, replacing older Mobs JARs. Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Mercenaries remains optional and separate.

Release size: **140,506 bytes**. SHA-256: `5995cc2ab2a61dc18f56ee916ebe5d163dab7b58a04ed3696d8b6da149e14dee`.

## Automated validation

- **116/116** required standalone server GameTests passed.
- **126/126** required server GameTests passed with Mercenaries 1.15.7.
- All 71 packaged main classes match compiled output. GameTests, templates, diagnostics and third-party JARs are excluded.

New tests load actual legacy-style entity NBT and verify that a two-hour-old cycle retains thirty minutes, repeated initialization and another entity save/load preserve the same deadline, and an older cycle becomes ready immediately. Existing tests now check the 180,000-tick interval for fresh adults, newborns, birth resets, exact expiration and persisted deadlines. Ordinary scheduled grazing, mating AI, manual golden-carrot breeding, triple growth and other mod regressions continue to pass.

Reproduce: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. For combined testing, add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`.

Evidence: [GameTest outcomes](release-0.1.12-evidence/gametests.txt) and [artifact manifest](release-0.1.12-evidence/manifest.json).

## Dedicated server and two graphical clients

All **seven live checks passed** with the final release and Mercenaries 1.15.7 on the dedicated server and both graphical clients. Fresh natural adults started with unmodified 180,000-tick deadlines, grazed normally while the owner held a golden carrot, and converted actual grass to dirt. Two elapsed hours produced no premature offspring. The full shortened cycle then produced exactly one untamed foal, both parents reset to exactly birth time plus 180,000 ticks, and the foal inherited its herd. Both clients tracked the same foal without recorded render errors; further ticks caused no duplicate foals, and the normal inventory opened and closed.

Vanilla `/tick sprint` accelerated world ticks during the long calendar portions. The harness did not assign horse timers, love, mating targets, goals or offspring. Grazing and its synchronized native animation were observed at normal speed. The fenced pasture used normal grass spreading and natural daylight, with unrelated mob spawning disabled. Daylight and viewing-player night vision were adjusted only after the checks for the family illustration.

Evidence: [seven checks and actual deadlines/birth times](release-0.1.12-evidence/multiplayer-results.json), [the resulting family](release-0.1.12-evidence/horse-autonomous-family.png). Reproduce with `tools/multiplayer/horse_autonomy_0_1_12.py` and the separate diagnostic harness.

This is a controlled loopback test using offline identities and Linux Mesa software rendering at 960×540, not an arbitrary-mod/network/GPU compatibility guarantee. Local clients disable NeoForge's optional early-loading window; cloud authentication lookups and fixture-teleport movement warnings may appear. No mod crash, packet-handler failure or mixin application failure was found in the completed logs.
