# November 23, 2026 — Version 0.1.65 — Changelog

- **Daytime scorpions:** Normal Scorpions can spawn in wooded badlands during the day as well as at night. They remain passive during the day.

Only wooded badlands ignores skylight during the normal Scorpion placement check. The native block-light limit remains active, preserving torch spawnproofing. Valid ground, non-Peaceful difficulty, player-distance and monster-cap rules remain native. Standard badlands and eroded badlands retain their existing monster-light checks. The higher wooded spawn weight, singleton groups and night-only hostility are retained. Lava Scorpions are unchanged.

All twenty-two focused Scorpion GameTests passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. The native category-spawn test now exercises wooded badlands at noon and midnight on red sand, terracotta, grass blocks and coarse dirt. A separate registered-placement test supplies full skylight explicitly: wooded badlands accepts daylight, the other two variants reject it, artificial block light remains protective, and wooded daytime spawns cannot start attacks. The native pipeline test retains its bounded retries and population-cap isolation described in the preceding release notes.

[Native surface observations](scorpion-0.1.65/native-spawn-observations.txt) · [Test result](scorpion-0.1.65/focused-tests.txt)

Release and multiplayer-harness builds passed; test classes are excluded from the release JAR. Rendering and unrelated gameplay suites were not rerun for this spawn predicate change. The pending 0.1.63 tail adjustment and 0.1.64 spawn-weight tuning are included.

The user authorized committing and pushing this release.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.65.jar`

SHA-256: `72dd483c16f873825bd9a63a760566f87db2bee4330f1688e5a68e6ecf98be28`
