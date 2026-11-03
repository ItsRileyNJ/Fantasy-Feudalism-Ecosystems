# November 3, 2026 — Version 0.1.33 — Changelog

- **Creeper spawning:** Halved the current natural-spawn weight, making creepers less common.

## Behavior

Creeper entries now use 20% of the incoming spawn weight instead of 40%. For a vanilla weight of 100, this changes 40 to 20: half the previous weight and 80% below vanilla. Actual encounter counts still depend on the rest of the weighted spawn pool, mob caps, loaded terrain and valid spawning conditions.

The existing minimum weight of one remains in place. Group sizes, native light/placement checks and stable spawn-entry identity are preserved. Eggs, commands and spawners retain their behavior. The pending 0.1.32 zombie rage reduction is included in this build.

## Validation

The native spawn-list test checks that a creeper entry with weight 100 becomes 20, while spider/ghast weights and other group sizes remain correct. The existing spawn revalidation regression checks stable entry identity. All **243 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed, including the zombie rage reduction. Release and optional multiplayer test-harness builds passed. Full automated log: `run/compatibility-un02zsi8/ecosystems.log`.


Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.33.jar`

SHA-256: `33ecc62e68e436d1556ffba7d9e6723bbd631fe68a0c4d4478d0846df8ba1cc9`
