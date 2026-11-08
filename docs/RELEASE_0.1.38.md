# November 8, 2026 — Version 0.1.38 — Changelog

- **Bird drops:** Robins, blue jays, goldfinches and crows now each drop one feather when killed.

The four native entity loot tables each contain one guaranteed single-item feather roll. There is no player-kill restriction or Looting multiplier. Native `doMobLoot` and loot hooks remain active.

Validation: Gradle build passed, resource JSON checks passed, all four packaged loot tables were verified in the release JAR, and whitespace checks passed. The resource-only change did not require repeating the full behavioral suite.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.38.jar`

SHA-256: `4528ad3b370c9a4d3173992cf2b156be74887af2bcd216f69060272ccbf0b78c`
