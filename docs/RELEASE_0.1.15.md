# Fantasy Feudalism - Ecosystems 0.1.15

Bat Enclosures display **exactly the actual bats stored inside: zero, one, two or three**. The cabinet model contains no decorative bat. Residents disappearing into nighttime flight are removed from the interior display, and the same identities reappear after returning at dawn. Both connected clients receive the server's occupant data, including after reloads and a normal server restart.

The enclosure follows the supplied wooden/glass cabinet reference: plank top, floor, back and sides, framed glass front, upper side vents and corner studs. Hanging bats are spaced according to occupancy. Their models render without world-entity shadows or debug hitboxes. A reusable Bat Net preserves the captured bat's identity, health, name and fertilizer deadline; full enclosures leave the fourth bat in its net. Breaking a home releases its actual occupants once. Missing or unloaded outside bats are never replaced by spawned duplicates.

## Added gameplay

- **Wither skeletons:** On Hard, 10% of new equipment rolls use bows; direct arrow damage is halved and accepted hits apply visible Wither I and Slowness I for 100 ticks. Archers never receive shields. Sword weights are 80% stone, 15% iron and 5% gold; half of sword users carry shields. Against players they advance crouched and blocking, then stand and sprint into melee within five blocks. Shields block frontal damage, wear down and leave the back vulnerable. Creative/Spectator transitions clear the combat stance.
- **Phantom guardians:** Right-click at night with an Eye of Ender. A successful survival interaction consumes one eye and cannot be stolen by another player. The phantom defends only against actual attackers of its player, respects allies and expires at sunrise, when ordinary daylight burning resumes.
- **Bat Net:** Recipe `SSS / SNS / STS`, where S is string, N an iron nugget and T a stick. Its empty and occupied forms have distinct visible models.
- **Bat Enclosure:** Recipe `PPP / FAF / PPP`, where P is any plank, F a wooden fence and A an apple. Capacity is three residents including bats outside; night flight stays within fifteen blocks of the home.
- **Fertilizer:** Apples heal housed bats and show hearts. Every bat independently drops one fertilizer when fed and ready, with a 6,000-world-tick/five-minute cooldown. Three ready bats consume three apples for three fertilizers. Injured bats can heal during cooldown without extra output; healthy cooling bats do not consume apples. Cooldowns follow bats through capture, relocation and saves. Yellow bone-meal-style Fertilizer instantly matures supported crops, including vanilla field crops, pitcher crops, stems/fruit, wart, berries, cocoa, cane and cactus. Mature or blocked plants do not consume it.

Normal skeleton equipment and the existing wildlife/companion rules remain available. The creative tab includes all six Ecosystems items. Mercenaries is optional and remains a separate mod; the integration recognizes a player's phantom guardian as an ally and preserves guard inventory synchronization.

## Installation and artifact

Replace the previous Ecosystems/Mobs JAR on **the server and every client** with `fantasy_feudalism_ecosystems-0.1.15.jar`. Requires Minecraft 1.21.1, NeoForge 21.1.262 and Java 21. Registry IDs, config identifiers and existing save data retain their previous namespace.

Release size: **200,567 bytes**. SHA-256: `b5505b9ac8a3d6f0315ae5945e218d21dd315cad8120927b13a340ce383b4a51`.

All **91 production classes** match compiled output. The release excludes GameTests, test structures, the local diagnostic mod and third-party JARs. The [manifest](release-0.1.15-evidence/manifest.json) records release and runtime hashes.

## Server validation

Completed passing suites:

- **141/141** standalone Ecosystems GameTests.
- **153/153** Ecosystems and integration GameTests with Mercenaries 1.15.7.
- **336/336** original Mercenaries GameTests with Ecosystems installed: **489 combined compatibility cases** across the two namespaces.

Tests use real world AI for bat departure/return, wither ranged combat, shield advance/charge/melee, phantom defense and sunrise behavior. They also verify equipment distributions, accepted arrow damage/effects, directional shield protection/durability, ownership races, duplicate capture/transfer rejection, capacity, blocked exits, original UUID release, saved cooldowns, apple/item conservation, crop maturation/clearance, crafting recipes and previous mod features. No assertions were suppressed for the compatibility suites.

The automated suites preceded final client-only render-position and item-model corrections. Server gameplay code was unchanged by those visual corrections. The final resource models were then checked on the exact release JAR in the server and both graphical clients.

Reproduce with `./gradlew runGameTestServer build multiplayerTestJar --max-workers=2` and a fresh `-PgameTestDirectory=/absolute/path`. Combined tests add `-PcompatibilityMod=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar`. Run `-PgameTestNamespaces=fantasy_feudalism_mobs` and `-PgameTestNamespaces=villagerscontract` in separate fresh worlds. See [recorded suite outcomes](release-0.1.15-evidence/gametests.txt).

## Dedicated server and two graphical clients

**37/37 recorded live checks passed** across the colony, combat and final-model sessions, with original Mercenaries 1.15.7 installed on all three processes. Sessions used the same server gameplay implementation; the manifest distinguishes the intermediate visual builds from the final JAR.

- [20 colony checks](release-0.1.15-evidence/ecosystems-results.json): actual network captures/inserts, both clients' 0/1/2/3 interior displays, repeated full-capacity attempts, competing feeds, exact fertilizer/apple conservation, crop use, nightly flight, saved UUIDs, a normal server restart, dawn return, resource reload, cooldown persistence, breaking the home and reinserting its original bats.
- [12 combat/menu checks](release-0.1.15-evidence/night-combat-results.json): native bow hits with both particle colors actually emitted on each client; rendered crouched shield poses; real charge/melee damage; Creative release; eye consumption and competing ownership requests; native phantom retaliation; mercenary alliance; sunrise; new items safely rejected by reserved guard slots; both creative menus and resource reloads.
- [5 final-model checks](release-0.1.15-evidence/item-models-results.json): actual nonempty baked geometry, the yellow fertilizer tint, empty/occupied nets, synchronized capture, exactly one real interior bat, creative models, reload and save on the exact final JAR.

Screenshots show [three real occupants](release-0.1.15-evidence/screenshots/bats-3-clean.png), [the final one-bat display](release-0.1.15-evidence/screenshots/final-one-bat.png), [the final creative tab](release-0.1.15-evidence/screenshots/creative-final.png), and [a captured bat in its net](release-0.1.15-evidence/screenshots/net-captured.png). Test tools live under `tools/multiplayer/`; their Java harness is built separately and is excluded from the mod.

## Findings and limits

An earlier Mercenaries run failed its existing `mountedCrossbowRetreatRestartsAcrossGrassyStep` navigation assertion and later stopped making test progress; it was terminated. A fresh-world rerun of all 336 unchanged tests passed. An earlier standalone run failed the existing nearby-frog water-landing assertion; its full fresh-world rerun passed all 141 tests with unchanged frog code. These intermittent AI-test findings are retained in [the prior-run record](release-0.1.15-evidence/prior-run-findings.txt), rather than claiming every attempt passed.

Visual review caught and corrected roof occlusion of stored bats and a blank Bat Net caused by using a generated-item parent for cuboid geometry. The final net has nonempty geometry and was checked in hand and in inventory. A legacy invulnerable test-player flag was reset before the completed live combat run.

No mod crash, watchdog failure, mixin application error, packet-handler failure, duplicate bat/output or inventory desynchronization was observed in the completed live sessions. The log review separates cloud authentication DNS failures and offline-identity warnings from gameplay failures. Tests use a controlled loopback server, two Linux Mesa software-rendered 960×540 clients, offline test identities and the optional diagnostic mod. These results do not establish zero defects across arbitrary terrain, hardware, networks or other mod packs.
