# November 22, 2026 — Version 0.1.53 — Changelog

- **Wider claws:** Scorpion arms and pincers now spread farther outward.
- **Tapered body:** A wider front narrows through five armored sections toward the tail. Leg mounts and spines follow the new shape.

## Validation

Release and multiplayer-harness builds passed. A graphical client on the local dedicated server loaded the updated model with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The [wider stance](scorpion-0.1.53/closeup.png), [front-to-back taper](scorpion-0.1.53/front-top.png) and [sting pose](scorpion-0.1.53/sting.png) were rendered and visually inspected. Render observations showed finite model transforms, all eight leg chains and a full sting pose without render errors. Test code is excluded from the release artifact.

Only geometry and attachment positions changed: shoulders splay by 0.7 radians with a 0.1-radian inward forearm angle; the front carapace is twelve model pixels wide, and abdomen sections taper from 11.5 to 5.7 pixels. Rear leg mounts move inward with the body. Parent-child joints and existing animations remain connected. Body collider and combat behavior retain their previous values. Unrelated gameplay suites were not rerun for these model edits.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.53.jar`

SHA-256: `3222624a3045951ef6f5ea15e1e7b61358289d4f9486e63cb513302cf0a972e0`

Install the same version on server and clients. No commit or push was made.
