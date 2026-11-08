# November 8, 2026 — Version 0.1.41 — Changelog

- **Creeper vegetation:** Restored moss carpet, ferns and grass directly above suitable ground. Growth cannot stack or float.
- **Crop and water protection:** No vegetation replaces crops or water, or grows on farmland, waterlogged blocks or existing plants.

The earlier blanket air-preservation rule is relaxed only for normal air directly above approved dry, solid terrain. Cave air and void air remain unchanged. Short grass replaces two-block tall grass so new growth occupies exactly one air block. Original 75% coverage and 50:35:15 moss-carpet/fern/grass weighting remain, subject to native plant survival.

Mossy block conversion and the existing two-stage crop growth remain active. The block-conversion configuration switch still only controls mossifying existing blocks; surface vegetation retains its original independent behavior. Native mobGriefing and entity-griefing permissions gate all effects, and creeper explosions still preserve terrain blocks while damaging entities normally.

All **266 required native GameTests passed with Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 installed together**. New regressions use real normal/charged-size explosions repeatedly to verify surface-only growth, no stacking, special-air preservation, existing plant preservation, and crop/farmland/water/waterlogged-block protection. Updated existing regressions allow only the newly authorized surface-air exception. Existing moss conversion, disabled-conversion, crop growth, radius and griefing tests passed. Full log: `run/compatibility-a_r38mw6/ecosystems.log`.

Build and test-harness builds succeeded; release JAR inspection and whitespace checks passed. Test classes are excluded. This build also contains the pending bird feather drops and appearance updates from 0.1.38–0.1.40.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.41.jar`

SHA-256: `3fb3cb994851d381112e1b2f4ff39f11487ea895eab076c4087ebf612d561a72`
