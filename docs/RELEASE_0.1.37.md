# November 8, 2026 — Version 0.1.37 — Changelog

- **Leaf perching:** Added optional Passable Foliage support so birds can pass through leaves in flight and stand on them after landing.
- **Canopy landings:** Birds choose exposed leaf tops rather than spots inside passable foliage.
- **Robin pairs:** Partners now share their cruising deadline, preventing one from repeatedly taking off while the other finishes its flight.

## Integration

The supplied Passable Foliage 9.3.2 binary has a native `hasLeafWalker(LivingEntity)` check used by both entity collision and pathfinding. An optional mixin returns true for grounded Ecosystems birds and false while their synchronized flight state is active. Other entities retain the original result. No leaf blocks, items, enchantments or global configuration are changed, and neither Passable Foliage nor Kiwi becomes a required Ecosystems dependency.

During landing, the existing final approach disables flight before gravity settles the bird onto its support. This enables native Leaf Walker collision before contact; takeoff disables it again. Ground navigation uses the same native check to recognize canopy support. Interior canopy positions are excluded from landing searches even if leaf collision shapes are empty.

Passable Foliage's own configuration still applies. Its player-only setting disables non-player pass-through, and its pathfinding toggle controls native pathfinding changes. Test with the modpack's intended settings before relying on other configurations.

Uploaded binary: `PassableFoliage-1.21.1-NeoForge-9.3.2.jar`

SHA-256: `77ee6be7f01a0844e18c74e1a51b59b99c718d4a957e3b5b3aefda90028cff7a`

## Combined-mod validation

All **264 required Ecosystems GameTests passed with the real Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 JARs installed together**. Full log: `run/compatibility-w_wkouie/ecosystems.log`. The leaf-specific tests verified actual movement through leaves while flying, support under all four grounded species, renewed pass-through after takeoff, canopy walking paths, preservation of solid stone collision and unchanged behavior for other mobs.

Supplied Kiwi: `Kiwi-1.21.1-NeoForge-15.8.7.jar`

SHA-256: `f1222ac3773b2814d40ee6e77129bb27a29d82ec76d7a878ba23d3f5f9f8a392`

Kiwi sends a cosmetic packet during player login. Existing simulated players had bypassed channel negotiation, causing fixture-only packet rejections. The test factories now call NeoForge's `configureMockConnection` before login. A shared Creative test-player helper preserves the native helper's Creative/Spectator semantics while initializing those channels. No Kiwi packet handling or production gameplay was changed to accommodate the tests.

The compatibility runner accepts `--foliage /path/to/PassableFoliage.jar --kiwi /path/to/Kiwi.jar` alongside the existing Mercenaries JAR argument. These fixtures are local runtime dependencies only and are not bundled or published with Ecosystems. The three foliage regression tests check actual flying movement through leaves, gravity settling and takeoff for all four bird species, unrelated mob/stone collision, interior-perch rejection and ground paths across a canopy. With Passable Foliage absent, they instead verify normal vanilla leaf collision.


## Dedicated server and two-client validation

Five additional checks passed using two real graphical clients connected to a dedicated server with Ecosystems 0.1.37, Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 on all three processes. Both clients completed actual network negotiation, rendered all four bird species and agreed with the server about leaf support during perch–flight–perch transitions. A controlled native movement check carried all four flying species through the canopy. Neither client reported bird rendering errors, and the server saved and shut down cleanly.

The pose fixture uses AI-disabled birds to hold each transition still while probing the native collision shape on both clients. Actual gravity settling and canopy walking are exercised by the native regression suite. Recorded evidence: [five multiplayer checks](birds-0.1.37/multiplayer-results.json), [first client](birds-0.1.37/perched-owner.png), [second client](birds-0.1.37/perched-guest.png). Environment audio-device/authentication limitations remain as documented for earlier live tests; the offline fixture does not validate public account authentication.


## Validation without Passable Foliage

All **264 required native GameTests passed with Mercenaries 1.15.7 installed and Passable Foliage absent**, and release/test-harness builds succeeded. This separately confirms the optional hook is safe without its target and preserves normal vanilla leaf collision. Full log: `run/compatibility-64muxb3l/ecosystems.log`.

Long-flight fixtures were expanded and their test barrier roofs removed so 20–35-second flights can finish without running into the original two-second fixture's boundaries. The robin regression now asserts a shared cruise deadline as well as paired takeoff, proximity and landing.

Resource JSON, Python syntax, whitespace and release contents passed. The release includes the optional mixin but no test code or bundled third-party classes.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.37.jar`

SHA-256: `05c1b60650ece1145d6526ff673fc99be2974e749f23b0f9fbb83fb933cbb537`
