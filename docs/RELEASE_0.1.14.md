# Fantasy Feudalism - Ecosystems 0.1.14

Renames Fantasy Feudal Mobs to **Fantasy Feudalism - Ecosystems** in the mod list, creative inventory tab, Gradle project display name and JAR filename. The current features are described in the [CurseForge page copy](CURSEFORGE_DESCRIPTION.md), with a separate [short project summary](CURSEFORGE_SUMMARY.txt).

Install `fantasy_feudalism_ecosystems-0.1.14.jar` on both the server and every client, replacing the old `fantasy_feudalism_mobs-*.jar`. Do not install both versions together. Requires Minecraft 1.21.1, NeoForge 21.1.262 or a compatible later NeoForge build for Minecraft 1.21.1, and Java 21.

The internal mod ID remains `fantasy_feudalism_mobs`. Registry IDs, resource paths, configuration filenames, persistent entity data and optional Mercenaries integration retain their existing identifiers. This preserves recognition of existing Bee Bottles, horse meat, companions and saved behavior data. The GitHub repository URL is unchanged.

## Validation

- **130/130** Mobs and focused compatibility GameTests passed with Mercenaries 1.15.7 loaded; build and multiplayer-helper build succeeded.
- All **73 gameplay class files** are byte-for-byte identical to release 0.1.13. Every packaged entry is unchanged except `META-INF/neoforge.mods.toml` and `assets/fantasy_feudalism_mobs/lang/en_us.json`.
- Verified the new display name and creative-tab translation, version 0.1.14, retained mod ID and unchanged resources/recipes. GameTests, templates, diagnostic helper code and third-party JARs remain excluded.
- The packaged renamed JAR started successfully on the existing dedicated-server world alongside Mercenaries, served a diagnostic snapshot and stopped normally without a mixin application failure. The old Mobs JAR was replaced before startup.
- The full **466 server tests and 56 two-client checks** in the [0.1.13 compatibility report](COMPATIBILITY_0.1.13.md) cover the unchanged gameplay implementation. That full live suite was not repeated for this metadata-only rename.

Release size: **146,598 bytes**. SHA-256: `51a9576f6ade1f1a2442b66202a90b503a6753e8bae6db0c3ad82b1b0daa2048`.

See [artifact comparison](release-0.1.14-evidence/manifest.json) and [server test/startup outcomes](release-0.1.14-evidence/validation.txt).
