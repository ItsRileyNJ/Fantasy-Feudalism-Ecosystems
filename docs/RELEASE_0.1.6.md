# Fantasy Feudal Mobs 0.1.6

Regular horses spawning in the same group now retain a persistent herd identity. They regroup beyond six blocks from their loaded leader and stop within three, with a 48-block local search and no teleporting or forced chunk loading. Foals inherit a parent’s herd. Panic, breeding, following parents, riders and leashes take priority over regrouping. Existing cow herd identities remain valid. Older ungrouped horses do not have their original spawn groups reconstructed.

Horses consume short grass or turn grass blocks into dirt, using their existing synchronized head-down grazing animation. As with sheep, `mobGriefing=false` preserves the block while allowing grazing. Riders and panic interrupt grazing. Interrupted grazing poses are cleared on load to avoid leaving a horse immobile.

Automatic breeding becomes eligible after **14 Minecraft days / 336,000 elapsed world ticks**, about 4 hours 40 minutes at 20 ticks/second. One adult eating grass can put itself and one eligible nearby adult horse into love mode, including untamed horses. Vanilla breeding AI then approaches and creates one foal with inherited horse attributes, coat and markings. Neither parents nor offspring become tamed automatically. There is no catch-up burst after unloading: eligibility waits for a successful grazing and a nearby eligible mate. The first timer starts when a horse is introduced to the world; a foal starts its timer at birth. Both parents receive a fresh interval after a birth. Deadlines persist through saves and chunk unloading. They use elapsed game time, so changing the displayed time of day does not reset or fast-forward the cycle.

**Golden apple/carrot breeding remains vanilla**, as confirmed by the user. Any successful birth also resets the parents’ automatic cycle. Babies, ridden horses and horses already in a breeding pair cannot start another automatic pair.

Adult regular horses drop **1–2 Raw Horse**, in addition to normal loot. Foals, donkeys, mules and undead horses do not gain these drops. `doMobLoot=false` suppresses them; Looting does not increase this new drop above two. Raw Horse has raw-mutton food properties and is tagged as raw meat for companion feeding. Cooked Horse has exactly cooked mutton’s food component: six hunger points and 9.6 saturation, with normal eating duration. Both have original 16×16 item sprites and entries in the mod creative tab. Furnaces cook one raw item into one cooked item in 200 ticks; smokers take 100 ticks, with 0.35 recipe experience each. Recipe-book unlocks are included.

Minecraft 1.21.1, NeoForge 21.1.262, Java 21. Replace older Mobs JARs on both server and every client. Mercenaries remains a separate optional mod.

Release: `fantasy_feudalism_mobs-0.1.6.jar` (103,556 bytes). SHA-256: `d8587c3c0ad979524f35ac725af03eb93c727a579365dc8afab172e384634211`.

## Automated validation

- Standalone Mobs: **87/87 required GameTests passed**.
- Mobs plus supplied Mercenaries 1.15.7: **97/97 required GameTests passed**.
- Release and separate diagnostic harness builds passed. Packaged classes match compiled output, with test code, the diagnostic harness and third-party JARs excluded.

New regressions cover horse spawn-group identity and separation, save/load of herds and exact cooldown boundaries, clearing interrupted grazing, grass/short-grass consumption and pose flags, one real AI-driven wild foal with herd inheritance, no immediate repeat breeding, solitary/baby/ridden exclusions, unchanged golden-carrot breeding, no-griefing grazing, adult-only drops and the mob-loot rule, actual furnace/smoker cooking times and item conservation, food properties and raw-meat tags. Existing cow, chicken, silverfish, spider and compatibility regressions also pass.

Reproduce with JDK 21: `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2`. For combined tests, add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`. Fourteen-day boundary tests restore saved deadlines close to the current game time rather than waiting 280 real minutes or changing global time for concurrent tests.

Evidence: [automated outcomes](release-0.1.6-evidence/gametests.txt), [artifact manifest](release-0.1.6-evidence/manifest.json).

The first graphical trial exposed an interaction with vanilla horse immobility: setting the grazing pose skipped all server AI, including the new goal’s countdown. The fix lets the horse’s grazing goal continue ticking while its own movement flags hold the horse in place. Native rearing and ridden behavior remain intact. A world-ticked grazing regression and the wild-breeding regression now exercise the scheduled goal instead of manually advancing its timer.

## Dedicated server and two graphical clients

All **nine live checks passed** using byte-identical final release JARs with Mercenaries 1.15.7 on the dedicated server and both clients:

- Both clients tracked a three-adult fixture finalized as one natural horse spawn group. A member teleported away for the test walked back under its own AI.
- Both clients rendered the native grazing pose, verified from synchronized eating state and actual rendered head rotation.
- After the two parents’ saved deadlines were advanced for the fixture, ordinary grazing and breeding AI produced exactly one foal. Neither love mode nor the foal was assigned by the harness. The foal appeared on both clients, inherited the herd, remained untamed, and both parents received fresh deadlines. No duplicate foal appeared during the additional observation.
- Both new food models resolved to their original sprites on both clients. Killing an adult produced one or two Raw Horse.
- A real client eating Cooked Horse consumed one item, increasing server and client hunger from 10 to 16 with 9.6 saturation.
- Both players opened the same smoker. One raw input produced one cooked output; one player took it, and the other viewer’s output slot cleared with no ghost cursor or extra cooked item. Closing the menu returned the one carried output to the correct inventory.
- Both connections remained active, with no recorded model-render error.

The loopback fixture uses offline test identities and Linux Mesa software rendering at 960×540. This tests the requested behavior and new item/menu flow; it does not cover every GPU driver, network condition, additional mod or population size. Normal cloud authentication/public-key lookup failures occurred during startup. No mod crash or mixin application failure was observed in the completed run.

Evidence: [nine results and synchronized snapshots](release-0.1.6-evidence/multiplayer-results.json), [grazing](release-0.1.6-evidence/horse-grazing.png), and [shared smoker output](release-0.1.6-evidence/horse-smoker-two-viewers.png). Reproduction fixture: `tools/multiplayer/horses_0_1_6.py`, with its separate diagnostic mod excluded from normal installations.
