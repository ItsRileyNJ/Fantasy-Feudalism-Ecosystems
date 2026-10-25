# Ecosystems 0.1.22 — mice, enclosures, and item artwork

## Mouse appearance and behavior

White Field Mice now have ivory highlights and light-gray fur variation. A second coat uses amber, honey, and light-brown fur. Both retain black eyes and pink noses, ears, feet, and tails, using the existing 64×32 atlas and model.

New mice choose white or amber with equal probability during native spawn initialization, including villages, spawn eggs, and ordinary command spawns. Color is synchronized through entity metadata and saved as integer `Variant` (0 = white, 1 = amber). Old mice without the field remain white; unknown values fall back to white. Explicit summon NBT can select a coat, for example `/summon fantasy_feudalism_mobs:field_mouse ~ ~ ~ {Variant:1}`.

Mice ignore Creative and Spectator players and end an existing escape when the player changes to either mode. Survival and Adventure players, cats, villagers, and Mercenaries remain threats. Threat checks retain the existing responsive cadence.

The feeding goal now releases control after visiting its crop. This allows bounded idle wandering within roughly four blocks of the remembered crop and ordinary random head turns between visits. It fixes mice standing motionless on their crop while waiting for food. Crop searches/feeding checks retain their twenty-second cadence and shared search budget. The minimum feeding cooldown is **ninety seconds** (1,800 ticks); the next eligible crop check performs the bite, so actual intervals can be longer if the mouse is wandering, fleeing, or cannot eat. Each bite removes one stage from one crop and respects `mobGriefing`.

Old two-minute saved deadlines are shortened by thirty seconds once, preserving time already elapsed; new saves record the feeding interval to prevent repeated migration.

Cats now detect and pursue mice within **sixteen blocks**, with line-of-sight and sit/sleep priorities preserved. Mouse origin does not change hunting/fleeing rules: native spawn-egg mice are hunted by cats and flee nearby villagers.

## Enclosures and items

- Bat feet attach to the underside of the ceiling at 14/16 of a block. Display positions vary in horizontal placement, depth, and angle for one, two, and three residents. The existing fixed animation clock preserves still hanging poses.
- A shaded, dirtied wooden texture covers inward-facing surfaces directly. No extra coplanar lining overlay is added.
- The side slats previously had twenty pairs of overlapping, same-facing surfaces. Splitting intersecting pieces and removing fully buried faces eliminates those overlaps. Despite the splits, rendered model faces decrease from 200 to 166. The front glass has separate front/back planes.
- The Bat Net is a native flat handheld sprite based on the supplied wooden-handle/hoop/white-mesh reference. Both empty and occupied item models use that artwork; no custom cube geometry remains. Captured-bat data and tooltips still identify an occupied net.
- Raw and cooked horse meat use separate transparent icons based on the supplied red/pink and cooked red-brown reference images. Registry IDs, food properties, and cooking recipes are preserved.

Artwork was prepared with image generation and reduced to ordinary Minecraft PNG assets. There is no runtime image-generation dependency.

## Validation

- **198/198 standalone native GameTests passed.** New coverage includes all coat spawn paths and metadata, legacy/invalid coat data, player modes and active escape cancellation, native spawn-egg cat hunting and villager fleeing, crop-side movement/head turns, and saved timer migration. Updated boundary tests verify sixteen-block detection and the full ninety-second feeding cooldown. Existing twenty-second check/budget, crop, sound, achievement, and vanilla regression tests pass.
- **211/211 Ecosystems and integration GameTests passed with Mercenaries 1.15.7 installed.** Build and the separate multiplayer test harness also passed.
- **Dedicated server plus graphical client**, running the release JAR and Mercenaries: both coats rendered and matched server/client state; amber persisted through reconnecting. A mouse spawned by a real client egg interaction was caught/killed by a cat, and another egg mouse fled a nearby villager.
- Native client capture/insertion verified one, two, and three enclosure residents. All rendered hanging models stayed still across more than ten frames per bat, with zero pose changes and a fixed partial tick of zero. Inspected all four enclosure facings, the shaded interior, ceiling attachment, flat net, and meat icons. Resource reload completed without renderer errors.
- Static geometry audit found **zero** overlapping same-facing surfaces in the revised model. The item models contain no custom cuboids. Release packaging checks confirmed native advancements and textures are present and test classes/structures are excluded.

An older maximum-range arrow test was made deterministic by removing shot spread only in its test fixture; production aiming is unchanged. The chicken escape fixture allows another ordinary navigation attempt. Initial fixture failures were corrected before the final runs; an unrelated random sunrise/sunburn test failed on one intermediate run and passed in the final standalone and compatibility runs.

Evidence: [native test summaries](ecosystems-0.1.22-evidence.txt), [mouse multiplayer observations](mouse-coats-0.1.22-live-evidence.json), [mouse screenshot](mouse-coats-0.1.22.png), [geometry audit](visuals-0.1.22-geometry.json), and the visual multiplayer evidence/screenshots beside this report.

The separate Mercenaries native suite was not rerun. Its previously documented mounted-retreat failure and pre-start chunk-readiness stall remain outside these passing Ecosystems results. The live test used the existing disposable offline server because Minecraft authentication services could not be reached.

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.22.jar` (341697 bytes), SHA-256 `05bd8097525b206d71fce82d07f46ed1672ed4f827c6b4b674eeb1cccbdbb6a9`.

Visuals: [enclosure](visuals-0.1.22-enclosure-oblique.png), [items](visuals-0.1.22-items-final.png), [live observations](visuals-0.1.22-live-evidence.json).
