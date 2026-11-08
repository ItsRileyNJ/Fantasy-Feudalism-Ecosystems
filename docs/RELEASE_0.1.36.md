# November 8, 2026 — Version 0.1.36 — Changelog

- **Longer flights:** Birds cruise for 20–35 seconds, usually 6–12 blocks above ground. Their maximum height is now 20 blocks above terrain.
- **Better landings:** Birds prefer taller perches such as tree canopies and settle onto the surface instead of hovering just above it.
- **Bird appearance:** Added feather shading and texture variation, removed side-facing eyes, and made legs trail backward in flight.
- **Robin pairs:** Partners regroup during longer flights and choose nearby landing spots.

## Behavior and fixes

Native flying paths finish at an air block's center, half a block above its support. Repeatedly requesting that path could leave a bird hovering above the intended perch. During a clear final approach, birds now release gravity and settle onto the support. Stalled approaches are retried after two seconds without progress; approaches also have an eight-second deadline, and invalid supports are discarded.

Cruising lasts 400–700 ticks before a landing search begins. Threats extend flight rather than shortening an existing cruise deadline. Terrain-relative destinations typically sit 6–12 blocks above ground, with the whole bird capped at 20 blocks. Leaves and tree trunks still do not raise the ground-height reference. These times assume 20 TPS.

Landing searches sample 12 loaded nearby columns, sort valid supports by height, and try at most six native paths. Leaves and other higher supports are preferred when reachable. Ground remains a fallback, and the height ceiling still applies to perches. Searches do not load chunks. Robin followers seek nearby partner landing destinations and leaders regroup when partners fall behind. Short walks now choose actual supported, reachable positions within two horizontal blocks.

Flight legs rotate backward by 0.9 radians and blend back into the existing walking animation on landing. The custom material atlas now has stronger feather mottling and highlights. The native geometry retains eight front-eye overlays across four models and no side-eye overlays; original supplied model files remain unchanged.

## Validation

All **261 required native GameTests passed with Mercenaries 1.15.7 installed**. Updated tests cover the 20-block ceiling and longer paired flights. New regressions check sustained flight well above ground, eventual landing without prolonged low hovering, and selection of higher tree canopies over nearby bare ground. Full log: `run/compatibility-_7gr9btp/ecosystems.log`.

Three additional checks passed with a dedicated server, Mercenaries and two graphical clients: all four updated models rendered, both clients observed the backward flight-leg pose for every species, and neither reported bird renderer errors. Front and in-flight screenshots and recorded native poses are saved with the [multiplayer evidence](birds-0.1.36/multiplayer-results.json). The in-flight screenshots freeze entity simulation after actual takeoff to inspect the pose; sustained movement and landing are covered by the native AI regressions.

JSON validation, deterministic geometry import, front/side-eye geometry checks, Python syntax and release JAR contents passed. Test code is excluded from the release. Cloud audio-device and Mojang authentication availability remain the limitations described in the 0.1.34 report; this is not a large-modpack stress benchmark.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.36.jar`

SHA-256: `16feeb7931b400ff3a577ea222afb6e8edf876a02f3ab3a8b7237b7f0a4f9c05`
