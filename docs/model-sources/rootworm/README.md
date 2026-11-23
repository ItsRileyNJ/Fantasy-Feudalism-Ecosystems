# Root Worm source provenance

User-supplied archive: `RootWorm-Integration-Kit-NeoForge-1.21.1.zip`.

- Archive SHA-256: `09cf3e7ad002ca683767ef553dd3d88bdfb95d6b4ab8fdced5f5b8506cf91eec`.
- Source version: **0.6.1**, source commit `8b9f0d954faab366d3bbffe3a444290afc66448c`.
- Original manifest is preserved unchanged as [SOURCE_MANIFEST.original.json](SOURCE_MANIFEST.original.json). All original manifest file hashes were verified before adaptation.
- Retargeted namespace `rootworm` to `fantasy_feudalism_mobs`, package `dev.fantasyfeudalism.rootworm` to `net.itsrileynj.fantasyfeudalismmobs.rootworm`. Supplied 128×64 texture is unchanged.
- Registered the module through the existing Ecosystems entry point and client events. Added the host creative-tab entry and native savanna biome modifier/tag. No second mod entry point, custom habitat/world generation, example build, Gradle wrapper or animation library was imported.
- Adapted the 22 supplied native GameTests to the host test package, template name and focused-test naming. Added two host integration tests covering the spawn egg and biome lists. Test sources and the kit arena are excluded from release artifacts.

The module retains the kit's model, behavior and arrow-part correction mixin. The listening curve was subsequently shortened at the user's request: the rear five segments stay grounded while the front four rise and scan. Rendering and multipart hitboxes share this curve. `RootWormClient` is a source helper; host renderer/layer registration is wired directly through `EcosystemsClient`, once.

Version 0.1.51 further anchors the head at its rear neck joint, eases the separate flat idle nod and renders both eye boxes solid black at the user's request. The original supplied texture remains unchanged.
