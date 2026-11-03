# November 3, 2026 — Version 0.1.32 — Changelog

- **Zombie rage:** Reduced the speed boost for zombies close to death.

## Behavior

The low-health speed bonus is now 25%, down from 100%. It still activates below three hearts, does not stack, and clears when the zombie recovers to three hearts or more. This applies to the existing zombie families, including husks, drowned, zombie villagers and zombified piglins.

Individual spawn variation, damage and the native baby speed bonus remain unchanged. A baby zombie may therefore still be faster than an adult.

## Validation

The existing native zombie-family test checks the 25% bonus, repeated updates without stacking, and removal at the health threshold across five zombie variants. All **243 required Ecosystems and integration GameTests passed** with Mercenaries 1.15.7 installed. Release and optional multiplayer test-harness builds passed. Full automated log: `run/compatibility-ghvf9637/ecosystems.log`.


Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.32.jar`

SHA-256: `0d1623cdb840022a8f0c983cb904b2254effa8fb91cf98c316dd722f048eaf31`
