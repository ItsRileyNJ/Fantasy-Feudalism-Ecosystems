# Fantasy Feudal Mobs 0.1.13

Frogs on land jump toward nearby water when a visible living player approaches within six blocks. Creative players count; spectators do not. The server searches surface water within twelve blocks in already loaded chunks. A clear ballistic arc launches the frog using its native synchronized jumping pose, sound and physical movement. For farther water, it walks toward a shoreline launch spot first. This does not teleport frogs or require new packets or renderers.

Breeding, egg laying, leashes, riding and existing native jumps take precedence. Frogs already in water keep their ordinary behavior. Covered or unavailable water is rejected; failed navigation releases the escape, and every attempt has a ten-second limit. Landing restores ordinary brain activity, pose and friction. Scans are throttled, and escape state holds no static world or entity references.

Install `fantasy_feudalism_mobs-0.1.13.jar` on the server and every client, replacing the older Mobs JAR. Requires Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Mercenaries remains optional and separate.

Release size: **146,555 bytes**. SHA-256: `9249d4babca7ab0899fe8355f68519f0bc9603b96073f5a3c96d7a2543a578fa`.

## Automated validation

- **120/120** required standalone server GameTests passed on the final code.
- **130/130** required server GameTests passed with Mercenaries 1.15.7.
- All **73** packaged main classes match compiled output. GameTests, templates, diagnostics and third-party JARs are excluded.

New tests exercise ordinary world ticks through the mixin: nearby jumping, measurable upward movement, walking closer before a distant jump, reaching actual water and restoring the standing pose. Additional cases cover missing water, solid roofs, detection range, creative/spectator players, love mode and pregnancy. The remaining tests exercise existing mob behavior and multiplayer regressions.

Reproduce: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. Combined testing adds `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs`. Use a fresh `-PgameTestDirectory` for each run. See [GameTest outcomes](release-0.1.13-evidence/gametests.txt) and [artifact manifest](release-0.1.13-evidence/manifest.json).

## Dedicated server and two graphical clients

All **three live checks passed** with Mercenaries 1.15.7 on the dedicated server and both graphical clients. A player moved toward a frog using normal client walking input; no frog movement, target or pose was assigned by the harness. The server launched the frog into real water. Both clients recorded rendered frames in its native jumping pose, then converged on the standing swimming pose without recorded render errors. World saving and opening/closing the survival inventory also worked.

The server and both clients loaded the exact release JAR hash above. See [the recorded entity states and per-client jump frame counts](release-0.1.13-evidence/multiplayer-results.json). Reproduce with `tools/multiplayer/frogs_0_1_13.py` and the separate diagnostic harness, which is excluded from the released mod.

This controlled loopback test uses offline identities and Linux Mesa software rendering at 960×540. Clients disable NeoForge's optional early-loading window. Cloud authentication DNS failures and fixture-teleport movement warnings occurred; no mod crash, packet-handler failure or mixin application failure was found in the completed logs. These checks cover the tested configuration, rather than arbitrary mod packs, networks or GPUs.
