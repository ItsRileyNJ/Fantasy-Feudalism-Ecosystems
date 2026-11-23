# November 22, 2026 — Version 0.1.51 — Changelog

- **Root Worm animations:** Fixed the detached head during raised listening. The separate flat head-nod now eases in and out, with smoother head turns.
- **Root Worm eyes:** Both eyes are now solid black and still blink.
- **Burrowing:** Airborne damage now queues a retreat after landing. Worms can burrow through one soil layer above stone without entering the rock.

## Implementation

The head's rear neck joint attaches to the next segment after all listening, looking, nodding and movement rotations. The shared listening curve uses the same joint for matching damage parts. Head/neck geometry retains half a pixel of overlap. The rear five segments still stay grounded during listening.

The flat idle nod uses an eight-tick blend with smooth easing and partial-tick interpolation. Visual head yaw approaches its target gradually. The entire eye mesh renders in black; separate pupil detailing was removed. Eye placement and blinking are preserved.

Accepted damage below half health queues a retreat for up to eighty ticks. If airborne, the worm waits to land and checks at most every ten ticks. Normal-scale burrow depth is 0.8 blocks, allowing the complete body to fit within a single soil layer while remaining above underlying stone. Route validation, clear exits, cooldown, particles and saved retreat handling remain active.

## Validation

- Release build and all **34 focused Root Worm GameTests passed** with Mercenaries 1.15.7 and Farmer's Delight 1.3.4. Tests cover head-joint continuity throughout scanning/raising/lowering, hittable raised heads, native flat-nod transitions, one-layer burrowing above stone and queued retreat after landing, alongside the existing Root Worm behavior checks.
- Dedicated-server checks with two graphical clients also loaded Passable Foliage and Kiwi. Both clients observed the fully raised listening pose, closed-mouth flat nods, black blinking eyes and finite animation transforms.
- Independent render observations measured head/neck attachment error below **0.001 model pixel** in both flat and raised poses, and a maximum idle-blend change of **0.183594 per game tick**, with both fully active and inactive states observed. The small numerical attachment tolerance accounts for Minecraft's lookup-table trigonometry versus the independent quaternion probe.
- A real airborne hit over one grass layer with stone immediately beneath it triggered burrowing after landing. Server and both clients observed the escape and resurfacing with seven health; all multipart positions stayed above the stone. No render errors were reported.
- [Saved observations](rootworm-0.1.51/multiplayer-results.json), [connected listening head](rootworm-0.1.51/owner-raised.png) and [flat head with black eyes](rootworm-0.1.51/owner-flat.png) are included. Multiplayer used a disposable offline-mode local server.
- Resource JSON and release test-code/test-arena exclusions passed.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.51.jar`

SHA-256: `966209edeb425582a91797b71769c8e2a7e2df45bdbea1f67a7edae6b2c91a21`

Install the same version on server and clients.
