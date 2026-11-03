# November 2, 2026 — Version 0.1.30 — Changelog

- **Creeper explosions:** Only existing blocks turn mossy. Air spaces stay empty, preventing floating moss and vegetation towers.

## Behavior

The previous moss-carpet, fern and tall-grass planting pass has been removed. Moss carpet's permissive native survival rule could allow newly placed carpet to support more carpet during the same pass. Existing eligible blocks still convert to their mossy equivalents, retaining properties such as facing and waterlogging.

Normal air, cave air and void air are explicitly skipped. Conversion still respects its configuration toggle, `mobGriefing` and NeoForge entity-griefing permission. Creeper explosions continue to damage entities without destroying blocks. The conversion path uses one bounded scan of loaded blocks and does not load chunks.

## Validation

The regression snapshots air throughout a charged-size explosion's bounding cube, including explicit cave-air/void-air positions and a gap in the ground, then compares exact block states after repeated normal and charged-size native explosions. Existing tests also check enabled/disabled conversion, block preservation, retained properties, entity damage and `mobGriefing`.

All **233 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed. Release and optional multiplayer test-harness builds passed. Final automated log: `run/compatibility-80p6p05u/ecosystems.log`. An initial run caught an old test expectation that disabled conversion still plants vegetation; this expectation was updated to the requested air-preserving behavior.


Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.30.jar`

SHA-256: `9196cd99c119ceb0be4a0593ab72bdc76cd7b12bcfff0c052863e3cdcf1a7e03`
