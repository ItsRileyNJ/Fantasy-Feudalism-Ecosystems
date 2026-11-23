# November 22, 2026 — Version 0.1.48 — Changelog

- **Bark:** Stripping logs or wood now drops one Bark, with a new item texture.
- **Root Worm feeding:** Feed Bark to restore two hearts. Full-health worms consume nothing.
- **Crafting:** Turn one Bark into one stick in the crafting grid.
- **Farmer's Delight:** Automatically use Tree Bark for Root Worm feeding. Disable Ecosystems' Bark drops and stick recipe, and hide its Bark while Farmer's Delight is installed.

## Validation

Release build and focused native Bark GameTests passed with Mercenaries 1.15.7 installed:

- In standalone mode, all 21 vanilla strippable blocks produce exactly one Bark, preserve their axis and native durability cost, and produce no more when clicked again.
- NeoForge placement-event cancellation restores the original wood and produces no Bark.
- Copper scraping and wax removal retain native behavior without Bark drops.
- The shapeless recipe produces one stick from one Bark in any crafting slot.
- Feeding through multipart body segments consumes one Bark, heals four health, caps healing and leaves full-health worms neutral without consuming food.
- Creative feeding preserves Bark; ordinary sticks do not heal worms.

## Farmer's Delight integration

The nine focused Bark tests pass both with and without the supplied Farmer's Delight 1.3.4 JAR, with Mercenaries 1.15.7 present in both runs. Additional integration coverage checks:

- Ecosystems Bark is omitted from native creative contents and belongs to the common `c:hidden_from_recipe_viewers` tag only when Farmer's Delight is present. The hiding tag lives in an always-active compatibility data pack registered only for that mod; top-level tag JSON does not support recipe-style load conditions. Recipe-viewer tag membership is tested; a separate JEI GUI session was not run.
- The Ecosystems stick recipe and recipe-book advancement are entirely absent while Farmer's Delight is installed, and loaded in standalone mode.
- Real Tree Bark heals Root Worms through any body part with the existing healing and consumption limits. Inactive Ecosystems Bark cannot substitute for Tree Bark; existing stacks remain registered for save compatibility.
- Farmer's Delight's cutting-board interaction still returns exactly one Tree Bark and a stripped oak log, without Ecosystems Bark. Native paper and organic-compost bark recipes remain loaded.
- Standalone Ecosystems restores its Bark item, creative visibility, stripping rewards, recipe-book unlock and stick crafting automatically when Farmer's Delight is absent.

Supplied Farmer's Delight archive SHA-256: `139ad7696462c89c03eea463f805abffa552526c5dadaadae221dd9624cb197c`. The external JAR is a test fixture and is not bundled or a required dependency.

Resource JSON, Bark assets, mixin registration and release exclusions were checked. The drop is awarded after the complete native item-use transaction, including protection hooks. No new per-tick searches or world scans.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.48.jar`

SHA-256: `ca8143922ae7909a785e3058f7f4bdbe812863ebc286076d22911b14579f8719`

Install the same version on the server and clients.
