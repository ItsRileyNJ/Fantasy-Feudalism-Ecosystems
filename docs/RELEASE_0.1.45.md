# November 8, 2026 — Version 0.1.45 — Changelog

- **Pet spider attacks:** Tamed normal spiders apply Slowness I and Weakness I for three seconds on each successful hit. Later hits refresh both effects.
- **Sitting spiders:** The lowered abdomen stays connected to the thorax as it angles down.
- **Field mice:** Added another spawning check later in the morning, keeping the existing cat-based population limit.

Normal pet-spider debuffs now cover all permitted living targets, including undead, other spiders and non-hostile targets. Wild spiders do not gain these effects, and cave-spider effects retain their existing rules. Owner, alliance, Creative/Spectator, PvP and native status-effect protections remain in place. Rejected attacks apply no debuffs.

The sitting pose rotates by -0.25 radians around its front attachment. Y/Z pivot compensation keeps that point fixed at the native thorax connection. Each render resets the offsets so the sitting adjustment cannot leak into another entity or standing pose. The native front-center hinge was checked numerically; no full graphical suite was repeated.

Village mice get one additional census at tick 500, 25 seconds after dawn at 20 TPS, within the existing tick 0–999 morning window. They still require at least three cats and fill at most one saved mouse slot per three cats, including unloaded residents. The second-check stamp survives saving and loading. This adds a bounded opportunity to fill missing slots; it does not add daytime/nighttime checks or increase the population cap.

**42 targeted spider and mouse GameTests passed**, with Mercenaries 1.15.7, Passable Foliage 9.3.2 and Kiwi 15.8.7 loaded. Relevant tests cover new target types, exact three-second level-I effects and refreshes, rejected hits, wild/cave-spider behavior, morning timing, the unchanged population limit and reload protection. Existing spider movement/combat and mouse checks also passed. Full suites were not rerun. Log: `run/compatibility-7kqxlvh5/ecosystems.log`.

Release/test-harness builds, Python syntax, whitespace and release contents passed. The test-only filter is excluded from the release. The compatibility runner now defaults to Ecosystems and supports `--filter 'spider|mouse|mice'` for focused checks.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.45.jar`

SHA-256: `aad9502c3742bfad1158149cf541d2746f5bd6a6aaa7e596beeb81f4d6a987bc`
