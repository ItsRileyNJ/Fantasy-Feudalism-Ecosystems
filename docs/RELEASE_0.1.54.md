# November 22, 2026 — Version 0.1.54 — Changelog

- **Scorpion shape:** Reworked around the new real-scorpion reference, with a lower segmented body, bent arms, larger tapered claw palms, curved pincer fingers and slender jointed legs.
- **Arched tail:** The segmented tail now curves forward over the back, with a strike adjusted to the new shape.
- **Stinger color:** Replaced the green stinger with normal dark shell coloring.

## Validation

Release and multiplayer-harness builds passed. A graphical client connected to the local dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. The [new silhouette](scorpion-0.1.54/closeup.png), [top view](scorpion-0.1.54/top.png) and [strike](scorpion-0.1.54/sting.png) were rendered and inspected. Observations recorded all eight articulated leg chains, walking motion, the full sting animation and finite transforms without render errors. The test victim received damage and poison after the animated strike.

Changes use native hierarchical model parts and existing material regions. All stinger geometry now uses the dark keratin region, with no green UV assignment. Armor edges and leg cuffs stand slightly proud of adjacent surfaces to avoid coplanar faces. Server combat, poison, spawn rules and collider dimensions retain their previous values. Unrelated gameplay suites were not rerun for these visual edits. Test code is excluded from the release JAR.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.54.jar`

SHA-256: `62251dfdf09b17c98244e3f0427a9ec61ea06369ba38956b20362c246f658a24`

Install the same version on server and clients. No commit or push was made.
