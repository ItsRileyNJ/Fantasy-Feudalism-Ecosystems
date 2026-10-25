# Ecosystems 0.1.23 — supplied Blockbench bat box

The Bat Enclosure now uses the uploaded `Batbox Model.json`. All seventeen element bounds, rotations, and face UV coordinates are preserved, including the front opening, rear vents, floor slats, and angled rear baffle. The [original export](model-sources/Batbox%20Model.json) is preserved separately for future editing.

The export contains no texture definitions and every face references `#missing`. These placeholders are assigned materials explicitly:

- Exterior: custom 32×32 wood grain, quantized to the actual vanilla spruce-plank palette so its colors match spruce.
- Interior surfaces and rear baffle: the existing custom shaded, dirtied spruce lining.
- Four front nails: `minecraft:block/cauldron_side`, using the cauldron's native dark iron colors.
- Breaking particles: the custom wood texture.

The **large opening is the front**, the model's north / negative-Z side. This matches the existing facing blockstates, player-facing placement, and front entry/exit logic. No extra glass or cuboids are added to the supplied geometry.

Hanging bat positions are moved forward beneath the flat roof, clear of the angled rear baffle and full side walls. Their feet still attach at the underside of the roof at 14/16, and the fixed display animation clock remains in place.

The custom exterior artwork was prepared with image generation, resized to a Minecraft tile, and mapped to the vanilla spruce palette. It is an ordinary PNG with no runtime dependency.

## Validation

- `build multiplayerTestJar` passed; `git diff --check` passed.
- Structural comparison confirmed that all seventeen supplied elements retain their exact bounds, rotations, and UVs. Every face has a resolved material. The custom wood's pixels are a subset of the actual spruce-plank palette.
- A dedicated server and graphical client ran the release with Mercenaries 1.15.7. Actual capture/insertion showed one, two, and three bats; each rendered with zero pose movement, a fixed partial tick of zero, and more than ten sampled frames.
- Inspected the new model from all four front-facing directions and an oblique view. The custom wood, dirtied interior, nails, rear vents, opening, and hanging bats rendered without missing textures or renderer errors. Native resource reload passed.
- Actual client item placement was checked from north, east, south, and west: the large opening faced the player in every case.
- Verified the release contains the new model/texture and excludes GameTests, the test structure, and the multiplayer helper.

The full automated gameplay suites were not rerun for this model/renderer-only update. The 0.1.22 baseline passed 198 standalone and 211 Ecosystems/integration tests with Mercenaries. Its separate native Mercenaries-suite limitations remain as previously documented. Live testing uses the existing disposable offline server.

Evidence: [observations](batbox-0.1.23-live-evidence.json), [front view](batbox-0.1.23-enclosure-north.png), [oblique view](batbox-0.1.23-enclosure-oblique.png), and [build/asset verification](batbox-0.1.23-evidence.txt).

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.23.jar` (342823 bytes), SHA-256 `968a9d36ae437ba7f6bfbcbb3bc019ef388ceeeda7c9a85a0c64a6a9ec9f5f4e`.

## Follow-up: roof contact and staggering inspection

Used an opt-in test-only renderer wrapper to measure actual animated native bat foot vertices in enclosure-local coordinates. One, two, and three residents were inspected; the three-resident case was also checked facing north/east/south/west. Feet meet the roof underside at Y = 0.875. Maximum measured deviation across all samples was 1.1920928955078125e-07 blocks (floating-point rounding), with at least 36 frames per bat per case. Existing hanging-pose probes also measured zero movement.

The measured three-bat positions in the north-facing enclosure are approximately (X,Z) = (0.26,0.31), (0.50,0.50), and (0.74,0.39), so they are staggered in depth. Their yaw offsets are -5°, +12°, and +5°. All attach to the same flat ceiling height. Close front and oblique visual inspection confirmed the placement.

This required no production changes; the measuring wrapper is confined to the separate multiplayer test harness and is excluded from the release. Evidence: [measurements](bat-attachment-0.1.23-live-evidence.json), [front close-up](bat-attachment-0.1.23-front-close.png), [oblique close-up](bat-attachment-0.1.23-oblique-close.png).
