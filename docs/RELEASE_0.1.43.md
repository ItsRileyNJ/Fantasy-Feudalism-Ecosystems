# November 8, 2026 — Version 0.1.43 — Changelog

- **Spider movement:** Wild and pet spiders now jump normally while wandering or following. This also applies to cave spiders.
- **Combat jumps:** Higher jumps and three-block jump paths remain available while attacking a valid target.

The unconditional jump-strength base is restored to vanilla's 0.42 on spawn and load, including previously saved pets. Combat adds the extra launch impulse only while the spider has a living, permitted attack target. Native block jump factors, attribute modifiers and Jump Boost remain active. Losing a valid target immediately returns ordinary movement to normal jump strength. Existing forward attack pounces, physical step height, wall climbing and fall protection remain unchanged.

Pathfinding uses native jump limits outside combat instead of planning three-block edges that following spiders cannot reach with a normal hop. Combat retains the raised pathfinding limits.

All **269 required native GameTests passed** with Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 installed. Three new tests cover all four wild/pet spider/cave-spider variants: measured normal hop height (approximately 1.25 blocks), immediate combat/idle transitions while preserving Jump Boost, and normal versus three-block combat path planning. Existing high-jump fixtures now provide valid attack targets; native three-block traversal, attack pounces and companion-following checks passed. Full log: `run/compatibility-e1n8pyp0/ecosystems.log`.

Release/test-harness builds, whitespace checks and release JAR inspection passed. Test code is excluded.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.43.jar`

SHA-256: `2d19e362cbfbc34873568a4cdb8fe143a333e3da432bca7bc39465efef829445`
