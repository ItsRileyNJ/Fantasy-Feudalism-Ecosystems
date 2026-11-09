# November 8, 2026 — Version 0.1.42 — Changelog

- **Bird subtitles:** Ambient, hurt and death subtitles now identify robins, blue jays, goldfinches and crows correctly instead of calling them parrots.

Twelve species-specific sound events reuse the existing vanilla parrot audio through event aliases. Each event has its own subtitle key. Existing volume, pitch and call timing are preserved, and vanilla parrot labels are unchanged.

All **266 required native GameTests passed** with Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 installed. Release and test-harness builds passed. All twelve packaged subtitle keys resolve to species-specific English labels and point to the expected audio aliases. Full log: `run/compatibility-s8tnuipr/ecosystems.log`.

A graphical client connected to the dedicated server with all three companion mods installed. Custom ambient aliases were played, and real damage/death triggered the bird sound overrides for all four species. Captured subtitles identify each species; a control still shows “Parrot talks” for the vanilla parrot event. Earlier captions may expire before the death screenshot. Evidence: [subtitle captures and notes](birds-0.1.42/subtitle-verification.json).

OpenAL's null output driver enabled the real sound engine and subtitle pipeline without an audio device; audible playback through speakers was not tested. This change reuses the existing audio files.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.42.jar`

SHA-256: `018c89b9e1451c694d9428c6b71e719ce9957c000e4dec366ffc94e75d855c14`
