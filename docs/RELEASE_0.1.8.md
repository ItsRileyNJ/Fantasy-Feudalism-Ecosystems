# Fantasy Feudal Mobs 0.1.8

Wild adult wolves detect sheep within fifteen blocks and share a prey target with nearby members of their persistent pack. They stalk at a 0.5 navigation multiplier; once any member reaches seven blocks, the hunt switches to the vanilla 1.0 attack multiplier for the whole pack and stays in the charge phase while that prey flees. Hits use vanilla melee damage and attack timing. Sheep outside the 32-block pursuit range can be replaced with a new nearby target rather than locking the pack into a stale hunt.

Wolves from one natural spawn group share a pack identity. Existing ungrouped wild wolves and wolves introduced with eggs or commands join nearby wild companions within 24 blocks. Pack membership is saved per wolf. One wild wolf killing a sheep starts a **24,000-tick / one-Minecraft-day** voluntary-hunting cooldown for every member. The deadline is stored in server saved data, including unloaded members and packs split across dimensions. While fed, wolves also refrain from voluntary rabbit/fox/turtle hunting; defensive and skeleton combat remains available. Tamed kills do not feed wild packs. Failed chases grant no meal.

A synchronized stalking flag drives smooth client pose transitions, lowering body and head, hanging the tail down, shortening the legs and retaining a reduced walking gait with a small body bob. Model state resets on every entity render, including the head height and leg scales. Tamed and sitting wolves do not use the wild crouch pose. Taming during a hunt clears wild hunting state; ordinary tamed following, sitting, owner defense, feeding and melee remain available regardless of the former pack’s cooldown.

Baby villagers gain a rare, harmless cat chase during their existing daytime PLAY activity. Eligible idle children roll a 1-in-12 chance every 200 ticks; successful play is followed by at least 2,400 ticks without another play. Selection requires an untamed cat within twelve blocks, visibility, and village membership. Chasing lasts up to **160 ticks / eight seconds**, with normal villager brain navigation and a playful escape goal on the wild cat. Neither participant receives a combat target or damage from play. Danger, sleep, adulthood, taming the cat or the target escaping can end play early. Adult villagers and owned cats retain their normal behavior.

Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Replace older Mobs JARs on the server and every client. Mercenaries remains separate and optional.

Release: `fantasy_feudalism_mobs-0.1.8.jar` (131,456 bytes). SHA-256: `d115193596ee908ddb8723b11ba8ca921cfd6354643dcef6fcb3bf52e53dddd3`.

## Automated validation

- Standalone Mobs: **101/101 required server GameTests passed**.
- Mobs with Mercenaries 1.15.7: **111/111 required server GameTests passed**.
- Release and separate diagnostic harness builds passed. Test classes, diagnostics and third-party JARs are excluded from the release; all packaged class bytes match compiled output.

New regressions cover fifteen/seven-block boundaries, a shared charge latch, full world-ticked pack stalking and killing with another sheep left unharmed, egg/command pack membership, replacement of escaped prey, shared cooldown persistence for unloaded members and saved-data round trips, cooldown expiration and defensive targeting, taming during a hunt, retained tamed melee/sitting/feeding, and exclusion of tamed/non-sheep kills. Village play tests cover an occupied village POI, the 160-tick stop boundary, no immediate repeat, harmlessness, adult/tamed exclusions and danger interruption.

The first world-ticked pack test exposed vanilla persistent anger starting when a wolf targets a sheep. Hunt validity now recognizes its own sheep target rather than canceling that hunt. Retaliation against unrelated targets keeps priority.

Reproduce with JDK 21: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. Add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory` for combined tests.

Evidence: [server test outcomes](release-0.1.8-evidence/gametests.txt) and [artifact manifest](release-0.1.8-evidence/manifest.json).

## Dedicated server and two graphical clients

All **eleven live checks passed** with byte-identical final release JARs and Mercenaries 1.15.7 on the dedicated server and both clients:

- Three wild wolves finalized as one natural spawn pack acquired the same sheep and stalked without hitting it from range. Both clients rendered the lower body and head, downward tail, shortened legs and changing walking rotations. A nearby tamed wolf retained its ordinary model pose, confirming shared-model reset behavior.
- The pack switched to charging near seven blocks. A real AI kill gave all three the same saved meal deadline. Another sheep remained unharmed, all wolves released prey targets, and both clients returned to standing poses.
- A real player attack triggered the tamed wolf’s owner-defense goal against the remaining sheep despite its former wild pack’s cooldown. It also followed its owner and obeyed real client sit/follow interactions. Its kill did not reset the wild pack’s meal deadline.
- An occupied-bed village fixture and a guaranteed rare roll started the child’s registered brain behavior. The child followed a moving untamed cat through normal navigation; both retained full health and no attack target was assigned. The play marker and owned walk state were released after 160 server ticks.
- Both connections stayed active with no recorded model-render error.

The stationary first sheep had 40 health to give time to observe both movement phases; the extra sheep used vanilla health. The harness supplied fixture entities and the successful random roll, but did not assign wolf combat targets, attacks, damage, meal deadlines or client poses. The play behavior itself chose its cat and used the standard villager brain.

This was a loopback test with offline identities and Linux Mesa software rendering at 960×540, not a general graphics-driver, network or large-population benchmark. One earlier client startup failed inside NeoForge’s optional early-loading memory overlay with a FileSystemNotFoundException; the final run disabled that overlay in the local test configurations only. Cloud authentication/public-key lookup failures were also logged. No mod crash or mixin application failure was observed during the completed sequence.

Evidence: [eleven results and snapshots](release-0.1.8-evidence/multiplayer-results.json), [stalking and normal tamed pose](release-0.1.8-evidence/wolf-pack-stalking.png), and [baby villager chasing the cat](release-0.1.8-evidence/baby-villager-cat-play.png). Reproduction fixture: `tools/multiplayer/wolf_village_0_1_8.py`, using the separate diagnostic mod excluded from releases.
