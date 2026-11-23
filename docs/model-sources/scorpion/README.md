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
