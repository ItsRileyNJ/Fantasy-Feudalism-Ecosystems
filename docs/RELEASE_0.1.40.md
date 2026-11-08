# November 8, 2026 — Version 0.1.40 — Changelog

- **Goldfinch appearance:** Reworked the yellow plumage, black crown and wings, cream belly and yellow shoulder markings to follow the supplied reference more closely.
- **Blue jay wings:** Added layered feather shading and darker edges, with more definition in the white bars. Wing UVs now sample the detailed parts of the materials.
- **Blue jay chest:** Includes the 0.1.39 fix removing the extra overlapping box beneath the head.

Only the goldfinch atlas row and the blue jay wing/wing-bar tiles changed. Robin, crow and unrelated blue jay pixels were verified unchanged. Goldfinch crown and shoulder markings use partitions of the original cubes; the added cap overlay was removed. The blue jay's chest marking also uses its original geometry. Supplied Blockbench model files remain unchanged.

Validation: release build passed; importer output is deterministic; release textures and the existing one-feather drops were verified in the JAR. The updated models were rendered on a real client connected to a dedicated server with Mercenaries, Passable Foliage and Kiwi installed. Front, side and angled views were inspected, and no bird rendering errors were reported. This appearance-only update did not repeat the full behavioral suite.

Evidence: [visual results](birds-0.1.40/visual-results.json), [blue jay](birds-0.1.40/blue_jay-quarter.png), [goldfinch](birds-0.1.40/goldfinch-quarter.png).

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.40.jar`

SHA-256: `ea7c1eb2bbde174353864de5d51528c848016df63add3df736e1081fe1b1ff32`
