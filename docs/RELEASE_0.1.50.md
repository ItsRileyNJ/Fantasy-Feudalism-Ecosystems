# November 22, 2026 — Version 0.1.50 — Changelog

- **Root Worm sounds:** Added six ambient clicking sounds with the subtitle **Rootworm clicks**.

## Validation

- Release build and the focused native ambient-sound GameTest passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. The test observes the registered event from the normal mob scheduler, its position and neutral sound category, the 120-tick ambient cooldown and native Silent handling.
- All six supplied mono Vorbis clips decode successfully and are preserved byte for byte. Every sound path and the exact subtitle text were verified in the release JAR. Test code is excluded.
- Clips use equal selection weights through Minecraft's sound system, with the same ambient cooldown as field mice. Hurt, death and feeding audio are unchanged.

Source archive: user-supplied `Rootworm Sounds.zip`. Files `Rootworm #1.ogg` through `Rootworm #6.ogg` map to `sounds/entity/root_worm/ambient_1.ogg` through `ambient_6.ogg`.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.50.jar`

SHA-256: `c4ceaa3d96d3dde15d06fe93baa3f9d37055c34b9dab7ba5b038973673c50958`

Install the same version on server and clients.
