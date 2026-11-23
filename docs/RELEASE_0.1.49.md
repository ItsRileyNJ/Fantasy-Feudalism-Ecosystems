# November 22, 2026 — Version 0.1.49 — Changelog

- **Frightened Root Worms:** Damage below half health makes them dive underground and burrow 4–6 blocks away before resurfacing. Dirt particles mark their route.
- **Digging limits:** Worms burrow through soft ground, including dirt, sand and gravel. Stone, shallow soil over rock, water and blocked exits prevent digging. Terrain stays intact.

## Behavior

A retreat takes three seconds at 20 TPS: 12 ticks to sink, 36 to travel and 12 to resurface. A successful retreat starts a twenty-second cooldown. Worms prefer moving away from the attacker and check alternate directions if necessary. Ground must be deep enough for the body, with a dry, clear surface and a flat route. Leashed, riding, passenger-carrying, submerged and NoAI worms do not burrow.

The actual entity and all nine multipart hitboxes move below the surface. Ordinary AI and attacks pause; gravity and in-wall suffocation are suppressed for the retreat. Other damage handling remains active. Buried worms cannot be hand-targeted or fed. Terrain changes abort the retreat before the next obstructed column. Saved worms reload at the current surface point and retain their remaining cooldown.

Route searches are bounded and run only on accepted damage below half health. Active retreats recheck their next body footprint, emit particles every two ticks and finish after sixty ticks. No terrain edits, chunk loads or background world scans.

## Validation

- **40 focused native GameTests passed** with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed: all existing Root Worm and Bark checks plus seven new burrowing checks.
- New checks cover the strict health threshold, rejected damage, actual underground travel, resurfacing, health preservation, unchanged terrain, stone and shallow soil, stone barriers, wet/blocked exits, escape direction, cooldown, mid-retreat reload and changing terrain.
- A dedicated NeoForge 21.1.262 server and two graphical clients loaded Mercenaries, Farmer's Delight, Passable Foliage and Kiwi. Both clients observed underground movement and synchronized resurfacing, preserved seven health and retained all nine registered parts. No render errors were reported. The above-ground dirt trail and surfaced worm were visually inspected.
- [Multiplayer observations](rootworm-0.1.49/multiplayer-results.json), [dirt trail](rootworm-0.1.49/owner-trail.png) and [surfaced worm](rootworm-0.1.49/guest-after.png) are included. The local multiplayer check used a disposable offline-mode test server.
- Release build, JSON, required assets and test-code/test-arena exclusions passed. This is focused validation, not a large-modpack performance benchmark.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.49.jar`

SHA-256: `f59e81ca7e81a13261b4384e7693f4e20a2163052fa4b182efb83a4cafc67076`

Install the same version on server and clients. This version includes the pending Bark and Farmer's Delight compatibility additions from 0.1.48.
