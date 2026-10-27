# Version 0.1.25 — Sheep grazing and cold

Sheep attempt grazing three times as often, and a sheared sheep needs three completed grass or short-grass meals to regrow wool. The eating animation, lamb growth bonus, wool color, shearing drops and mob-griefing hooks remain native. Failed or interrupted meals do not count. Progress is saved.

Exposed sheared sheep get cold in biomes below plains temperature (base temperature 0.8), or when native rain reaches them in warmer biomes. A roof provides shelter in every biome, including glass, leaves and slabs. Rainy global weather does not wet sheep in a rainless desert.

Cold sheep shiver using the native renderer, move 25% slower, and stop grazing. They take no cold damage. Recovery requires 600 uninterrupted server ticks without cold exposure (30 seconds at 20 TPS); renewed exposure resets it. Shivering, slowdown and grazing restrictions continue during recovery. The recovery deadline and wool progress survive saves. Wool restores insulation immediately.

Weather checks run once per second, staggered between sheep, so transitions can take up to one extra second. No nearby-entity scans or new grass-search scans were introduced. Cold metadata and movement modifiers are owned by the server and synchronized to clients; the slowdown does not stack or change the base movement attribute.

## Validation

- All **221 required Ecosystems and integration GameTests passed** with the unmodified Mercenaries 1.15.7 JAR installed. This includes ten new sheep tests covering adult/lamb grazing probability, three real meals, short grass, interruptions, save/reload, legacy data, native baby growth, unrelated animals, mob-griefing, weather/shelter, speed, recovery timing and interrupted recovery.
- Production JAR and optional multiplayer test-harness JAR built successfully. Test classes and probes are excluded from the production JAR.
- All **five two-client dedicated-server checks passed** with Mercenaries installed: synchronized cold/speed/native shivering on both clients, continued cold halfway through recovery, normal speed and no shivering after the full recovery, resource reload/renewed exposure, and rendering without errors. Results are recorded in [the live evidence](sheep-0.1.25-live-evidence.json).

Automated test log: `run/compatibility-hchfotom/ecosystems.log`. Native GameTests encase their arenas in a barrier roof; the weather fixtures explicitly open their test column to the sky. This corrected the initial weather-fixture failures without changing production shelter detection.

The disposable graphical test environment lacks an OpenAL audio device and Minecraft authentication DNS. These environment limitations prevent audio-device and online-authentication validation; they do not prevent the offline-identity dedicated-server/render tests. This release does not claim a full rerun of Mercenaries' own 338-test suite or arbitrary modpack coverage.

Historical sheep-only test build (superseded by the combined 0.1.26 build):

SHA-256: `cf90ea94194cfe0ba2fd3eb5ea66b58a959b8b2b2bd6cc38534919b745b40739`
