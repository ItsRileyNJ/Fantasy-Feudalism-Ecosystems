# Scorpion model and texture

Original Minecraft Java model and texture created for this mod using the user's bronze-and-black scorpion image as a visual reference. No external game mesh or texture files were imported.

- Geometry: `scorpion/client/ScorpionModel.java`, with eight two-part legs, two articulated pincers, overlapping dorsal armor, dark spines and a five-jointed tail with a dark hook.
- Texture: `assets/fantasy_feudalism_mobs/textures/entity/scorpion.png`, 128×128 pixels. Material regions: bronze at (0,0), dark keratin at (64,0), gold rims at (0,64), unused legacy green at (64,64), amber eyes at (96,96). The original deterministic pixel atlas uses seed 52419 for mottled grain and plate ridges.
- Animation: native Java walking, pincer movement, idle tail motion and a synchronized sting. Tail/claw segments are parented at their attachment points. No animation-library dependency.
- See the release notes and native screenshots for validation and size comparison.

Version 0.1.53 widens the shoulder/claw stance and front carapace, replaces the rectangular abdomen with five tapered sections, and follows that taper with the armor, spines and leg attachment points.

Version 0.1.54 follows the user's second, real-scorpion reference: a lower armored body, bent pedipalps with large stepped-ovoid palms and curved fingers, slimmer bent legs and a forward-arching tail. The stinger uses the dark keratin material at (64,0), not the unused green atlas region. The tail strike drops forward with the new arch.

Version 0.1.55 simplifies the rigid torso shell to six cubes and removes layered tail caps/collars. Mirrored outer pincer fingers are larger; the face has eight eyes and separately articulated chelicerae. The native model contains 105 cubes in total.

Version 0.1.56 follows the newest face reference with three graduated pairs of frontal eyes, larger chelicerae and paired tapered cheek feelers. Large claw palms and curved fingers are bulkier, while the simplified torso shell and dark stinger are retained.

Version 0.1.57 lengthens both arm segments, reshapes the pincers into closed-rest crab-like chelae with a fixed outer finger and hinged inner finger, and enlarges the venom bulb with a longer three-section dark hook/needle.


Version 0.1.58 removes palm top plates and simplifies each palm and finger to one cuboid, as requested. Native geometry only; the original material atlas is retained.

The subsequent pincer revision halves each black finger base and adds a slimmer second cube at its tip. Both cubes use the same articulated pivot.

The half-size fingers were rejected. The current revision restores their original dimensions and spacing, with only one step down from the bulky rear block to a smaller end block.


Version 0.1.59 adds the quarter-scale Lava Scorpion through the same geometry builder and animation code. Its material UVs select the existing charcoal and gold-orange atlas regions rather than changing cubes or editing the source texture. Original Scorpion UVs are unchanged. The separate renderer applies a uniform 0.25 scale; the server entity type supplies matching collision dimensions.


The Lava Scorpion now uses `lava_scorpion.png`, an image-generated edit of the original material atlas. The charcoal quadrant adds mottled rock-like plates, pits, gray highlights and sparse burnt-orange fissures. The atlas quadrant layout is retained; native geometry, UV coordinates and original Scorpion texture remain unchanged. Generated source: `/workspace/generated_images/exec-da663ad6-3969-41c2-adb2-76d2afc41df4.png`.

The Lava Scorpion eye mask now samples charcoal instead of gold, separating the six orange eyes without altering model geometry.

The latest Lava Scorpion atlas edit replaces the amber/gold accents with molten red-orange, darker red mottling and bright yellow-orange highlights. It was generated from the preceding atlas with the charcoal quadrant preserved. Source: `/workspace/generated_images/exec-54d9e29c-4a64-4ed2-9ba7-15e66098d884.png`.
