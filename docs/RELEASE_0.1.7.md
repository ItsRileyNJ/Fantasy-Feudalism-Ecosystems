# Fantasy Feudal Mobs 0.1.7

Natural foal growth takes three times as long: **72,000 loaded server ticks / 60 minutes at 20 TPS**, instead of vanilla’s 24,000 ticks / 20 minutes. Baby horse age advances once every three server ticks. This also slows the remaining growth of existing foals. The partial-tick phase is stored in persistent entity data and survives save/load without multiplying or resetting the saved age.

Vanilla age units and feeding remain intact: food advances the same amount of age immediately. Adult breeding cooldowns still count down every tick, and the fourteen-day automatic breeding interval is unchanged. Other animals, including donkeys, retain vanilla growth. Maturation uses vanilla `setAge`, baby-state synchronization and dimension refresh.

Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Replace the older Mobs JAR on the server and every client.

Release: `fantasy_feudalism_mobs-0.1.7.jar` (104,539 bytes). SHA-256: `c4661fe12a3e2b30e96ddac8fb0e09daaea778414bc41b3bbc7c97429ecbb8c3`.

## Validation

Standalone Mobs: **90/90 required server GameTests passed**. Mobs plus Mercenaries 1.15.7: **100/100 required server GameTests passed**. Release build and packaging checks passed.

The new regressions exercise the real aging routine across individual ticks and 300 further ticks, save/reload between partial growth ticks, actual world-ticked maturation after a shortened test age, adult dimension changes, vanilla adult cooldown rates, unaffected cow/donkey growth and immediate feeding-based growth. The full-hour duration follows from the unchanged 24,000 vanilla age units advancing once every three ticks; tests do not wait a real hour.

Reproduce using JDK 21: `./gradlew runGameTestServer build --max-workers=2`. Combined tests use `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`.

Evidence: [test outcomes](release-0.1.7-evidence/gametests.txt) and [artifact manifest](release-0.1.7-evidence/manifest.json). This change was tested on the dedicated GameTest server; graphical two-client checks were not rerun. The previous release’s graphical results remain documented in [0.1.6](RELEASE_0.1.6.md).
