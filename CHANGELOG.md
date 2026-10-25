# Changelog

## 0.1.17 — cats, Rested, and Field Mice

- Nearby strays and your own cats can join you in bed. Sleep beside one to wake with six minutes of **Rested**, reducing hunger exhaustion by half and improving natural healing by 35%. Look for three blue Zs in your inventory.
- Added tiny white **Field Mice** with pink tails and two hearts. Villages try to spawn mice each morning, up to one mouse per three cats.
- Mice nibble one growth stage from a planted crop every two minutes. They flee nearby players, cats, villagers, and mercenaries, then return to the crops.
- Cats chase mice within fifteen blocks and kill them in one hit. Mice run 10% slower than a chasing cat.
- Mouse crop damage respects `mobGriefing`; saved meal timers and village population counts prevent reloads from bypassing the limits.

## 0.1.16 — fixes and compatibility audit

### Fixes

- Fixed bats shaking while hanging inside Bat Enclosures. Their resting poses now stay still instead of repeatedly replaying part of an animation.
- Fixed spiders being allowed to attack Creative and Spectator players through some targeting paths.
- Fixed tamed spiders ignoring a server's disabled PvP setting when their owner was offline.

### Performance

- Reduced repeated calculations when skeletons aim arrows. The aiming calculation was about 59% faster in an isolated benchmark and produced identical results. This does not mean the whole game runs 59% faster.

### Multiplayer and compatibility testing

- Tested with the supplied **Fantasy Feudalism — Mercenaries 1.15.7** mod on a dedicated server with two real game clients.
- Verified shared guard inventories, recruitment, companion alliances, horse mounting, resource reloads, and saved data after restarting the server.
- Checked vanilla interactions including leads, axes and shields, milk, bone meal, furnaces, hoppers, and the `mobGriefing` setting.
- Passed all 23 Bat Enclosure checks, including capture, capacity limits, feeding, nighttime flight, morning return, and saving/reloading. Bats showed no pose movement across more than 100 rendered frames per bat on both clients.
- Passed 162 automated Ecosystems tests on its own and 174 with Mercenaries installed.

### Known issues and testing limits

- The full Mercenaries automated suite is not all green: a mounted-retreat test also fails with Mercenaries alone, and another test can stall before starting.
- A frog jump/landing worked on the server, but the approaching client's visual check remained incomplete. The observing client displayed the jump.
- Load tests stayed connected and responsive, but occasional lag spikes remain. These checks do not guarantee perfect compatibility in every world or modpack.

See the [multiplayer results](docs/MULTIPLAYER_AUDIT_CURRENT.md) and [mechanics and performance audit](docs/DEEP_AUDIT_CURRENT.md) for evidence and remaining findings.

Earlier version summaries are in the [README](README.md#version-0116-changes).
