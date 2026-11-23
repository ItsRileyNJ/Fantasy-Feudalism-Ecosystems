# November 22, 2026 — Version 0.1.52 — Changelog

- **Scorpions:** Added a spider-sized hostile mob inspired by the supplied reference, with bronze armor, dark spines and pincers, amber eyes and a curled green-tipped tail.
- **Animations:** Eight articulated legs, moving pincers and a tail strike with a visible wind-up.
- **Venom:** Successful stings apply Poison I for five seconds.
- **Spawning:** Scorpions spawn in deserts and all badlands variants. A spawn egg is included.

## Behavior

Native spider-sized dimensions: 1.4 blocks wide and 0.9 blocks tall, with extra rendering bounds for the raised tail and claws. Sixteen health, movement speed 0.26, four raw attack damage and a sixteen-block follow range. A sting lasts eighteen ticks, with damage after an eight-tick wind-up. The strike rechecks native target validity, melee range and line of sight. Poison applies only after accepted damage; native effect strength, immunity and removal rules remain active. Creative, Spectator and allied targets are protected.

The mob uses native monster AI, difficulty, darkness, despawn and population rules. Spawn weight is 12, groups 1–2, in Desert, Badlands, Eroded Badlands and Wooded Badlands. Natural encounters depend on native conditions and available monster population slots. Scorpions resist poison, belong to the native arthropod tag for enchantments and award five experience. No additional loot materials were introduced.

The original Java model has articulated hierarchical parts, an original 128×128 material atlas and native procedural animations. No GeckoLib is needed. Existing spider/bee audio is referenced through scorpion-specific sound events and subtitles.

## Validation

- Release build and **nine focused Scorpion GameTests passed**, with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. Checks cover the egg and dimensions, biome entries, real hostile target acquisition, delayed melee/poison, a victim leaving range, rejected damage, protected players, native arthropod membership and effects, and save/load without a stale attack.
- Dedicated-server checks used two graphical clients with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi installed. Both clients observed eight articulated legs, a full synchronized sting pose, actual walking and finite model transforms. The victim received damage and poison after the strike. Both native resource reloads completed without render errors.
- The model, material atlas and tail strike were visually inspected. A [side-by-side spider comparison](scorpion-0.1.52/spider-size-comparison.png) confirms the intended size. [Close-up](scorpion-0.1.52/closeup.png) and [saved render observations](scorpion-0.1.52/multiplayer-results.json) are included. The local multiplayer test used a disposable offline-mode server.
- Resource JSON, renderer/item/texture inclusion and release test-code/test-arena exclusions passed. Focused tests were used; unrelated suites were not rerun.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.52.jar`

SHA-256: `4eae3220bbf4c7922291a85b8868dcb98fde9cd280564c9f0d80424be2a68efb`

Install the same version on server and clients. This working build also includes the pending Root Worm fixes from 0.1.51. No commit or push has been made for these pending changes, as requested.
