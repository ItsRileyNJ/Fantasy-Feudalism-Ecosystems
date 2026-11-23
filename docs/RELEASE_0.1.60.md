# November 23, 2026 — Version 0.1.60 — Changelog

- **Idle scorpions:** Tails sway slowly and pincers occasionally flex while standing still.
- **Walking tails:** Both scorpion variants now sway and bob their tails while walking.

## Details

The shared model rotates existing tail joints and inner pincer pivots; it adds no cubes, entity metadata or server tick work. The outer pincer finger remains fixed. Most idle frames keep the jaws closed; a smooth occasional pulse opens the inner jaw up to 0.12 radians. Entity-specific phase offsets avoid synchronized motion. Idle movement fades as walking increases, and secondary tail/pincer movement fades toward the peak sting pose. Existing damage, timing, spawning and hostility are unchanged.

## Validation

Release and multiplayer-harness builds passed. A graphical client connected to the dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi observed both variants. Their native render probes recorded approximately 0.12 radians of idle jaw movement, 0.09 radians of idle tail yaw variation and 0.13 radians of walking tail yaw variation. All eight legs were present, model transforms remained finite and no render errors were reported. [Saved observations](scorpion-0.1.60/render-results.json). Unrelated gameplay suites were not repeated for this client animation edit. Test classes remain excluded from the release JAR.

[Animation preview: idle then walking](scorpion-0.1.60/animation.gif). The preview capture was stopped after enough frames were collected; its saved GIF is nine seconds long.

No commit or push was made.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.60.jar`

SHA-256: `2b09164a0482782743db595160af562d0a8130f53cb51f72b0e2abd23f1f9e93`
