# November 23, 2026 — Version 0.1.57 — Changelog

- **Longer arms:** Both segments of the scorpion's pincer arms are about thirty percent longer.
- **Crab-like pincers:** Broad palms and shorter, heavy curved jaws now rest closed. The inner jaw opens during attack wind-up and clamps shut as the hit lands.
- **Sharper stinger:** A larger venom bulb leads into a longer, sharply curved dark hook and needle point.

## Details

Upper arm length increases from 5.2 to 6.8 model pixels; forearms increase from 3.8 to 5.0. Elbow/palm pivots move with the segment endpoints so the longer arms stay connected. The larger outer pincer finger is fixed; its inner counterpart hinges open for an attack. Idle jaw oscillation is removed. Pincer motion derives from the existing synchronized sting clock and closes at the original eight-tick damage point.

The venom bulb is a single 4.2×3.8×5.8 cube, with three connected dark hook/needle sections. Tail caps and collars remain absent. Only the rendering envelope grows for the extended claws; body collider, attack timing, damage, poison and spawning are unchanged.

## Validation

Release and multiplayer-harness builds passed. A graphical client on the dedicated server loaded the updated model with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The [longer arms and stinger](scorpion-0.1.57/closeup.png), [closed resting claws](scorpion-0.1.57/face.png), [open attack claws](scorpion-0.1.57/open-pincers.png), walking and sting poses were inspected. All eight leg chains and model transforms remained finite; the full strike was observed and the victim received damage and poison. No render errors were reported.

Native observations recorded a maximum idle jaw angle of exactly zero and an attack jaw opening of approximately 0.5 radians. [Saved observations](scorpion-0.1.57/render-results.json). Test code is excluded from the release JAR. Unrelated gameplay suites were not rerun for these visual edits. No commit or push was made.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.57.jar`

SHA-256: `c4ef449368248bc65dbbecb3ba00eefd04f1285973dc04d0c029880cdef8f2e9`

Install the same version on server and clients.
