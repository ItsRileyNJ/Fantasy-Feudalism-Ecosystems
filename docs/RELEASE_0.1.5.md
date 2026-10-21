# Fantasy Feudal Mobs 0.1.5

Chickens flee Survival/Adventure players within three blocks. Fleeing outranks breeding and seed temptation; vanilla panic and swimming remain available. Adult chickens and chicks use the same fleeing speed: 95% of a normal unbuffed player's ground sprint speed, approximately 5.33 blocks/second on ordinary ground. Creative/Spectator players retain vanilla avoidance exemptions. Normal wandering speed is unchanged. Terrain, collisions, potion effects and sprint jumping can change actual travel speed.

Cows finalized in the same spawn group receive a shared herd identity, saved in each entity's persistent data. Separated members walk toward the same loaded herd leader beyond six blocks and settle within three blocks. A missing leader is replaced by another loaded member. Regrouping yields to panic, breeding, wheat temptation, calf following and leashes. Navigation respects obstacles; it does not teleport cows or load distant chunks. Herd searches are local (48 blocks) and throttled, and routes update every ten ticks. Older cows without recorded spawn-group membership remain unchanged; separate nearby spawn groups do not merge.

Targets Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Install the new Mobs JAR on the server and each client, replacing older Mobs versions. Mercenaries remains optional and is not bundled.

Release JAR: `fantasy_feudalism_mobs-0.1.5.jar`, 90,048 bytes.

SHA-256: `9552f7a5e04b38a63792e8d91ac7d11f37a062fc43f579857f8099e704067c14`.

## Validation

Standalone Mobs: **74/74 required tests passed**. Mobs with Mercenaries 1.15.7: **84/84 required tests passed**. Release build and packaging checks passed. See [test outcomes](release-0.1.5-evidence/gametests.txt) and [artifact manifest](release-0.1.5-evidence/manifest.json).

Reproduce with JDK 21: `./gradlew runGameTestServer build --max-workers=2`. For the combined run add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`.

The dedicated GameTest server exercises both new behaviors alongside the existing Mobs regressions. New checks cover:

- Adult and baby chicken avoidance at 2.9 versus 3.1 blocks, with a real escape path leading away from the player.
- Normal chicken AI escaping instead of following a player holding seeds.
- Actual vanilla ground acceleration/friction at steady speed, comparing chicken motion with sprinting-player motion; the measured ratio must be 0.95 within 0.002.
- Shared cow spawn-group membership, isolation from unrelated nearby groups, and persistence through entity save/load.
- A separated cow autonomously navigating back to its own herd.
- Herd leader replacement and yielding to a leash.

GameTest classes and the separate diagnostic harness are excluded from the release JAR. These are server behavior regressions, not a new two-graphical-client or network-load audit. Earlier graphical multiplayer results remain documented with their original releases.
