# Fantasy Feudal Mobs 0.1.4

Silverfish now remain as roaming packs until they personally land a zombie's killing blow. Only that killer burrows; assistants and bystanders continue roaming until their own kill or death. Nearby zombies take targeting priority over players. Uncredited silverfish retain retaliation and avoidance of wild/tamed spiders. The existing natural mineshaft spawn weight 60, group attempts of 3–9, hostile light checks, collision checks and mob caps remain unchanged. Vanilla spawning can produce smaller groups when positions are unsuitable.

Kill credit is saved per silverfish and applies to all spawn origins, including infested-block, spawner, egg and command silverfish. Once credited, the killer stops hunting and enters a nearby stone-type block. It remains hidden, silent and inert for **600 game ticks (30 seconds)**, then is discarded. The block itself is unchanged, so there is no permanent infestation or replacement silverfish to release. The deadline starts on entering the block and survives save/load without resetting. If the chunk unloads, an expired hidden silverfish is removed when ticking resumes. No extra spawning loop, entity type or configuration option was added.

Vanilla status-effect timers keep ticking while hidden. Six-second spider debuffs and their particles still expire on schedule, and an effect update does not reveal the burrowed silverfish.

Targets Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Replace older Mobs JARs on the server and every client with `fantasy_feudalism_mobs-0.1.4.jar` (84,210 bytes).

SHA-256: `d15e7246777d6b74d09dfb13224937f152e32c454540780f4557a297152bc843`.

## Reproduction and automated validation

The strengthened regression invokes the real natural spawner inside a registered mineshaft fixture and requires a pack from one spawning pass. Before the fix, it created three silverfish, but **0/3 remained alive after 200 ticks**: all had merged into stone. The earlier regression discarded new mobs immediately and only required one silverfish, so it missed this failure.

After the fix, the naturally spawned pack remains above ground for 200 ticks, including one member saved and reloaded through normal entity NBT. The pack then acquires and damages a zombie through its own targeting and melee AI, without forced targets. Further regressions cover exclusive final-hit credit, rejected hits on a dead zombie, assistants receiving no credit for environmental or non-zombie deaths, zombie variants, all spawn origins, burrowing with mob griefing disabled, and preserving the original stone block. Burrowing extinguishes the killer to avoid a visible flame above its hiding place. The timer regression saves and reloads the hidden fish at 300 ticks, checks that it is still alive at 599 ticks, and confirms removal after the 600-tick deadline.

- Standalone Mobs GameTests: **68/68 passed**, process exit 0.
- Mobs with the supplied Mercenaries 1.15.7, focused Mobs/compatibility GameTests: **78/78 passed**, process exit 0.
- Release build and separate multiplayer harness build: passed. GameTest classes, the diagnostic harness and Mercenaries are excluded from the release JAR.

Reproduce with JDK 21 using `runGameTestServer build multiplayerTestJar`. The combined run uses `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar -PgameTestNamespaces=fantasy_feudalism_mobs` and a fresh `-PgameTestDirectory`. The supplied mod's unrelated full native suite was not rerun for this change.

Automated evidence: [baseline and final outcomes](release-0.1.4-evidence/gametests.txt).

## Dedicated server and two graphical clients

All **nine live checks passed** with byte-identical final release JARs and Mercenaries 1.15.7 on the dedicated server and both clients:

- A fresh group of at least three uncredited silverfish spawned through ordinary natural spawning in the existing seed-1 generated mineshaft. No silverfish were summoned, spawned with eggs or created by the harness.
- The pack remained above ground for over 220 server ticks, and both clients tracked the same live entities.
- Silverfish already pursuing a nearby Survival player switched to a visible zombie and damaged it through their own AI.
- The zombie's health was lowered for the final-hit fixture. Its actual killing-blow source alone received burrow credit; the other pack members kept roaming. Both clients received the killer's invisible, silent, inert state.
- Slowness, Weakness and Poison applied for six seconds while it was hidden expired normally, including their synchronized particle data. The fish stayed hidden and its original burrow deadline did not change.
- The killer remained present before its deadline, then was removed from the server and both clients after 600 ticks. The other pack members remained roaming.

The fixture freed competing mob-cap slots during natural spawning. After capturing a fresh pack, it removed other entities to isolate this behavior from cave-spider spawners and unrelated combat; the zombie fixture was preserved. Terrain and mineshaft blocks were unchanged. The zombie's position was checked for collision clearance and line of sight. No silverfish target or kill-credit flag was assigned by the harness.

Evidence: [nine results and entity snapshots](release-0.1.4-evidence/multiplayer-results.json), [release manifest](release-0.1.4-evidence/manifest.json), and [surviving silverfish in the generated mineshaft](release-0.1.4-evidence/natural-mineshaft-owner.png). The local fixture script is `tools/multiplayer/mineshaft_packs_0_1_4.py`; keep its separate diagnostic mod out of normal installations.

## Test scope

The final completed sequence retained both connections and recorded no render error. No mod crash, mixin application failure or missing texture was observed. This was a loopback test with offline identities and Linux Mesa software rendering at 960×540; it does not establish behavior for every graphics driver, network condition or additional mod.

Earlier fixture attempts encountered cave-spider attacks and a client timeout during startup/resource tuning. The final sequence began after both clients completed loading and used separate CPU allocations for the server and software-rendered clients. Cloud authentication/public-key lookups also produced the previously known connectivity messages. These results are focused functional checks, not a new load benchmark.
