# November 8, 2026 — Version 0.1.35 — Changelog

- **Robin pairs:** Robins now spawn in pairs of two, take off together and follow their partner in flight. Goldfinch flocks remain 3–6.
- **Lasting partners:** Robins remember their partner after saving and can find a new one if their partner dies. Existing and spawn-egg robins can pair with nearby unpaired robins.
- **Lone robins:** Unpaired robins wait for company before ordinary flights, but still escape threats and recover from water or falling.

## Behavior

Robin biome entries and native group limits both use two. Available space and native creature caps can still produce an incomplete group. Existing larger flocks are not deleted; their members form exclusive pairs as their AI runs.

Partner searches run at most every five seconds within loaded nearby entities. Pair UUIDs are saved; a missing partner has a 30-second grace period before the loaded bird can choose another. No chunks are loaded to find partners. Leashed, mounted and AI-disabled partners do not trigger ordinary paired takeoffs.

The follower uses native flight pathfinding near the leader and seeks landing sites near it, leaving enough room for both birds to settle. Landing state prevents a partner from repeatedly taking off while the other is landing. Terrain and obstacles can briefly separate birds; flight remains subject to the existing 15-block ceiling. Fleeing and recovery flights remain available even when no partner is available.

## Validation

All **259 required native GameTests passed with Mercenaries 1.15.7 installed**, and release/test-harness builds succeeded. Updated regressions verify two-robin biome entries and native group identities while retaining goldfinch flocks of 3–6. Three new tests cover exclusive pairing, partner persistence and replacement, lone-robin ambient-flight restrictions and threat escape, and real paired takeoff, proximity and landing. The live AI test observed both birds flying within eight blocks before both landed.

Resource JSON validation and whitespace checks passed. Full log: `run/compatibility-q683i65w/ecosystems.log`. The prior version's two-client graphical checks are documented separately in the 0.1.34 report; they were not repeated for this server-side behavior change.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.35.jar`

SHA-256: `85e62358fcacf6d2551b80203bcb3a62f4610d36fe848cb66b0e3d50a4a7738d`
