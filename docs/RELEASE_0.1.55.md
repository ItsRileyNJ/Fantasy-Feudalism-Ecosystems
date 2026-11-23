# November 22, 2026 — Version 0.1.55 — Changelog

- **Simpler torso:** Reduced layered head/body armor from forty cubes to six while retaining the tapered shape.
- **Tail:** Removed overlapping cap and collar layers that could cause z-fighting.
- **Pincers:** The outer finger of each large claw is slightly thicker and longer than the inner one.
- **Eyes and mouth:** Added eight eyes and distinct paired chelicerae with moving small jaws.

## Geometry

The abdomen has five single-cube, butt-joined sections and the head has one rigid shell. Separate edge strips, stacked plates and the brow are removed. Tail shell geometry drops from nineteen cubes to eight: one per link, plus the bulb, hook and tip. The overall model drops from 139 cubes to 105, including the additional eyes and chelicerae.

Outer fingers are eighteen percent thicker and eight percent longer, correctly mirrored left/right. Two median eyes and three lateral eyes per side retain the amber material. The mouth consists of a dark opening and two small chelicerae, each with a fixed finger and moving fang. Their motion is cosmetic; sting combat, poison, size and spawning are unchanged.

## Validation

Release and multiplayer-harness builds passed. A native mesh audit verified six torso-shell cubes, eight eyes, mirrored larger outer fingers, and no overlapping coplanar tail faces across twenty idle/strike pose samples. See [mesh audit](scorpion-0.1.55/mesh-audit.json).

A graphical client on the dedicated server loaded the new model with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The [simpler body](scorpion-0.1.55/closeup.png), [face/chelicerae](scorpion-0.1.55/face.png), walking and sting poses were inspected. All eight leg chains and model transforms remained finite, the full strike pose was observed, and the test victim received damage and poison. No render errors were reported. [Saved observations](scorpion-0.1.55/render-results.json).

Test code is excluded from the release JAR. Unrelated gameplay suites were not rerun for this model edit. No commit or push was made.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.55.jar`

SHA-256: `313d59c160ba3b8f039cfc2caf9739d8d4ffcbbad551751e2b51a0211fba554f`

Install the same version on server and clients.
