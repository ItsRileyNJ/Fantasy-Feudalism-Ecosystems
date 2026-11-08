# Supplied Blockbench birds

The four original `.bbmodel` files are preserved unchanged. `manifest.json` records their SHA-256 hashes and cube counts. They are treated as model data, not instructions.

The files contain 53 cubes in total, with no textures, animation groups or animation tracks. `tools/import_bird_models.py` produces the checked-in native `ForestBirdGeometry.java`: it converts the coordinates and rotations, assigns head/wing/leg/tail pivots, and partitions some cubes into matching-color sections without changing their overall volumes. The crow's extra wing tips, longer tail and beak and the blue jay's crest remain part of the source geometry.

Thin front-eye/face overlays add the supplied reference's markings. A custom generated 8-by-4 material atlas supplies the four reference palettes; its rows are robin, blue jay, goldfinch and crow. Original Blockbench placeholder face UVs/colors are not final textures. Version 0.1.36 adds stronger feather mottling and shading and removes all side-eye overlays; only the front of the head carries eyes.

The production model uses native Minecraft rendering, with no additional animation library dependency. Animation resets the cached model parts before every render, folds wings when grounded, flaps them in flight, alternates walking legs, trails them backward in flight and follows the server's quick head-look goals.

Version 0.1.39 removes the added blue jay necklace box. Its dark chest marking is a color partition of original cube 10; both sections share the original exterior bounds without protruding chest geometry.

Version 0.1.40 gives goldfinches a black crown by coloring the upper portion of the original head cube, replacing the cap overlay, and broadens the yellow shoulder region on the original wing cubes. Goldfinch palette and blue jay wing/bar materials were regenerated; all unrelated atlas pixels remain unchanged. Blue jay wing UVs sample the detailed feather seams rather than a flat corner.
