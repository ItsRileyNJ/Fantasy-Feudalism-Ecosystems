# November 23, 2026 — Version 0.1.61 — Changelog

- **Deep Lava Scorpions:** Spawn in groups of 3–5 only below Y=0 in covered caves near lava. They are immune to fire and lava.
- **Contact heat:** Lava Scorpions do not attack players. Touching them deals magma-style damage.
- **Spider hunters:** Normal Scorpions kill spiders with one successful sting, including tamed spiders, then briefly eat them.
- **Different attacks:** Normal Scorpion stings give players ten seconds of Nausea instead of poison. Pincer hits deal ordinary damage.
- **Arrow armor:** Large Scorpions are immune to arrows.

## Details

Lava spawning keeps rarity weight 3 and the existing loaded-chunk lava search (six blocks horizontally, three vertically), while requiring negative Y. Native spawn entries select packs of three through five and the Lava Scorpion cluster cap is five; actual pack success still depends on available valid positions and native caps. Spawn eggs bypass the natural habitat restriction. Native entity-type fire immunity blocks fire, burning and lava without changing the normal variant.

Lava Scorpions reject player targets and direct/timed attacks, including retaliation. Contact uses one point of native hot-floor damage on bounding-box contact. Native fire resistance and damage immunity remain effective, and careful stepping prevents damage as on magma blocks. Creative and Spectator players are protected. Contact applies no venom; normal native damage cooldowns regulate repeated contact. Lava Scorpions may still retaliate against non-player attackers.

Normal Scorpions retain world-clock night-only hunting. They prioritize spiders, including cave spiders and pets, and always use the stinger on spider prey. A lethal native mob-damage hit triggers forty ticks of stationary chewing with synchronized mouth animation; damage cancellation and invulnerability prevent both the kill and feeding. Native spider death and loot handling are retained. No item scans or loot collection are added. Against other targets normal Scorpions alternate their original telegraphed sting with a shorter pincer attack. Both recheck range, line of sight and target validity when damage lands. Player stings apply Nausea I for 200 ticks without poison; pincers apply no venom. Existing sting poison against non-player, non-spider targets is retained.

Large Scorpion arrow immunity covers native arrow subclasses, including tipped and spectral arrows, while excluding thrown tridents. Lava Scorpions remain vulnerable to arrows. Native damage handling still controls other weapons and protections.

Attack kind and feeding time synchronize only when those actions begin. Shared model geometry stays connected; the feeding pose smoothly blends in and out. The pending 0.1.60 idle/walking animations are also included.

## Validation

All nineteen focused Scorpion GameTests passed with Mercenaries 1.15.7 and Farmer's Delight 1.3.4 installed. Tests cover negative-Y boundaries and pack entries; native fire and physical lava exposure; player contact, careful stepping and fire resistance; wild and full-health pet spider hunting; invulnerable prey; separate player pincer/sting effects; daytime/dawn behavior; and large-only arrow immunity with trident damage retained. The real-player attack fixture waits out Minecraft's sixty-tick join protection. Release and multiplayer-harness builds passed; test classes are excluded from the release JAR. [Saved test result](scorpion-0.1.61/focused-tests.txt).

A graphical client on the dedicated server with Mercenaries, Farmer's Delight, Passable Foliage and Kiwi observed the [feeding pose](scorpion-0.1.61/eating.png) and [pincer attack](scorpion-0.1.61/pincer.png). Eight legs and finite transforms remained valid, the feeding and sting reached full animation strength, and the pincer victim lost health without effects. No render errors were reported. [Render summary](scorpion-0.1.61/render-summary.json). The subsequently added arrow rule was verified in the dedicated-server GameTests; it changes no rendering. Unrelated suites were not repeated.

The user authorized committing and pushing this release.

## Artifact

`build/libs/fantasy_feudalism_ecosystems-0.1.61.jar`

SHA-256: `98ca053e061ae2b4c192b45d82d4ef5aac04f6170763a7459aac3a3e79670901`
