# November 23, 2026 — Version 0.1.56 — Changelog

- **Scorpion face:** Rearranged the eyes into the reference's front-facing cluster, with large slanted outer eyes and smaller central eyes.
- **Mouthparts:** Enlarged the chelicerae and added two long, tapered whisker-like feelers beside the mouth.
- **Bulkier pincers:** Larger palms and thicker curved fingers, retaining the larger outer finger on each claw.

## Details

The new dark face panel has three graduated pairs of amber eyes. Cheliceral bases and fangs are enlarged and remain articulated. Tapered cheek feelers slope down and forward with subtle idle and sting movement.

Large pincer palms increase from 5.6×4 to 6.8×4.8 model pixels, with stronger necks, fuller caps and heavier hooked fingers. Their joints remain connected, and outer fingers retain the existing relative size advantage. The rendering envelope expands slightly for the larger claws; the body collider, sting damage/timing, poison and spawn behavior are unchanged.

## Validation

Release and multiplayer-harness builds passed. A graphical client connected to the local dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The [front face](scorpion-0.1.56/face.png), [larger pincers and feelers](scorpion-0.1.56/closeup.png), walking and sting poses were rendered and inspected. All eight leg chains and model transforms remained finite, the full strike pose was observed, and the test victim received damage and poison. No render errors were reported. [Saved observations](scorpion-0.1.56/render-results.json).

Test code is excluded from the release JAR. Unrelated gameplay suites were not rerun for this visual edit. No commit or push was made.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.56.jar`

SHA-256: `b6d1e636f8332c19a35324284e0f8140c9bbea986523599b28dec4bf5bfe7722`

Install the same version on server and clients.
