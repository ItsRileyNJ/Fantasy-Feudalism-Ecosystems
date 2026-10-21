# Mobs 0.1.1 and Mercenaries 1.15.7

Validated October 20, 2026 (America/Los_Angeles; runtime logs use October 21 UTC), on Minecraft 1.21.1, NeoForge 21.1.262, and JDK 21. This report covers this specific pair, without additional third-party mods.

## Release and installation

Use `fantasy_feudalism_mobs-0.1.1.jar` with the uploaded `fantasyfeudalism-mercenaries-1.15.7.jar` on the dedicated server and every client. Replace the older Mobs JAR. The optional integration does not make Mercenaries mandatory for Mobs-only installations. No new gameplay settings, items, commands, or GUIs were added by this compatibility work.

The supplied Mercenaries JAR is unchanged. SHA-256:

`32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`

Final Mobs release SHA-256:

`436907336838132913b2b373a7b9ed1d22c79816e23aadb0148cd7a7beb38c08`

The production server and both graphical clients loaded identical copies of the final Mobs JAR and the original Mercenaries JAR. The separate observation/control harness was confined to test installations and is excluded from the release and downloadable mod pair.

## Conflicts found and corrected

- **Friendly spiders:** Mercenaries uses the vanilla `Enemy` marker to classify threats. Tamed spiders retain that marker. Guards now recognize both tamed spider types as allies, including for target validation and friendly-fire cancellation. Companions respect the reverse alliance, avoid debuffing friendly guards, and keep their quiet-sound rule when only friendly guards are nearby. Wild spiders remain hostile.
- **Skeleton threat classification:** Mobs gives skeletons crossbow capability even when they are holding another weapon. Mercenary spacing now follows their actual equipment: 20 blocks for bows, 12 for crossbows, and 7 for melee. This avoids treating every skeleton as a crossbow user.
- **Faster arrows:** Scaling launch velocity alone made some vanilla bow/crossbow arcs overshoot. The corrected launch direction accounts for vanilla discrete drag and gravity, retains the requested 40% speed increase, and preserves vanilla angular spread. Mercenary arrows do not receive the skeleton multiplier. Real-client combat verified that bow and crossbow skeletons can hit mercenaries and synchronize the resulting health changes.
- **Hidden-inventory prediction:** Competing shift-clicks on one guard stack reproduced a ghost stack: the losing client showed 13 string while its server inventory held zero. The server itself conserved items. A rejected/no-op guard transfer now sends an authoritative private-inventory correction even when the server inventory never changed. Successful transfers retain the existing delta path; idle menus do not resend corrections. Repeated races verified item conservation and exact client/server inventory agreement.

The three integrations into Mercenaries use optional `@Pseudo` mixins. They do not alter the uploaded JAR or add its classes to the Mobs release.

## Automated gameplay checks

- **60/60 focused GameTests passed** with both mods loaded, followed by a successful release build. This final run includes the sound-filter correction, projectile-hit tests at 10/20/34 blocks, and ten targeted compatibility regressions.
- **336/336 supplied Mercenaries GameTests passed** with both mods loaded. These cover contracts, inventory safety and packets, orders, combat, mounts, patrols, village supplies, sieges, and persistence.
- **50/50 standalone Mobs GameTests passed** with Mercenaries absent, validating optional loading and the original gameplay regressions plus projectile-hit checks.

The supplied and standalone suites ran before the final one-line friendly-guard sound-filter change; the final focused suite and final packaged-client checks ran afterward. See [recorded test outcomes](compatibility-evidence/gametests.txt). The ordinary Gradle `test` task has no JUnit sources; the counts above come from real NeoForge GameTest server execution.

Use separate fresh test worlds for the two namespaces, as shown in the README. One all-in-one 395-test attempt was interrupted with **314 completed passes, zero reported failures, and 81 unfinished tests** after batch advancement stopped progressing usefully. A thread dump located work in Minecraft chunk processing. That incomplete run is not counted as a pass; the separate completed namespace runs provide the coverage above. Mercenaries' village tests also use fixed saved-data IDs, so reusing an earlier test world can create false failures.

Earlier investigation retained real failed runs. Four initial companion-alliance tests failed before the fix, and controlled nominal arrows demonstrated overshoot before the trajectory correction. Mercenaries' mounted-grass-step assertion also failed in one of three Mercenaries-only reference cases and one of three controlled bow cases: it requires crossing a particular X coordinate even when the randomized retreat reaches the desired distance diagonally. The final completed supplied suite passed without changing or disabling that assertion. These diagnostic outcomes remain distinct from the final passing results.

## Dedicated server and two graphical clients

The fixture used a production NeoForge `forgeserver`, two separate graphical NeoForge development-runtime JVMs, the packaged release JARs in their `mods` folders, and a separate local-only control/observation mod. Project source/class folders were excluded from client mod loading. Interactions and GUI clicks used each client's real game-mode methods and normal Minecraft connection, including client prediction.

**26 final packaged-client checks passed** (11 Mobs/logout checks and 15 Mercenaries/combat/load checks). Final checks cover:

- Concurrent bee capture, actual taming, ownership protection, entity-state synchronization, 2,000 competing poison-harvest requests, 2,000 competing silk requests, independent cooldowns, and full inventories.
- Two-player contract redemption of one two-pillager patrol: one conversion and one consumed contract across 200 competing requests.
- Shared GuardScreens, hidden non-hotbar inventory updates, protected ammunition, order buttons, string deposits, safe rejection of Bee Bottles and harvested Poison, and GUI scales 1/2/3.
- Private mercenary highlights reaching only the requesting client, resource reloads on both clients, item conservation across cursor/logout/world-drop/guard-pickup transitions, and reconnect synchronization.
- Twelve repeated inventory races with 96 additional competing quick-move requests, preserving 17 string per race and correcting the previously reproduced losing-client ghost stack.
- Actual bow/crossbow AI damage to guards, matching health updates on both clients, advancing render frames, valid model rotations, no missing Bee Bottle model, and bounded mixed combat/load.
- Clean client logout, with prompt server-side removal instead of a timeout.

A normal server stop/restart additionally preserved contracts, guard gear/supplies/orders/skins, spider owner/order/cooldowns, and player inventories. Both clients also mounted/dismounted an owned horse through the real guard GUI with matching mount-state updates. Those three checks preceded only the final sound-filter change; screenshots include the mounted model beside both spider types.

The final live results and load measurements are recorded in [live-results.json](compatibility-evidence/live-results.json). Screenshots in the evidence folder were visually inspected.

The clean final load run created **180 modified mobs plus 20 armed mercenaries** and sampled 723 server ticks over approximately 35 seconds with both clients connected. Mean tick processing was **14.57 ms**, the 95th percentile **44.73 ms**, and the maximum **148.72 ms**, sustaining approximately **20 TPS**. Fixture construction and concurrent Gradle/reference jobs were excluded. The RPC-based elapsed-rate estimate is approximate; the raw results retain the original counts and timing.

[Guard GUI](compatibility-evidence/mercenary-menu-scale-2.png), [GUI scale 3](compatibility-evidence/mercenary-menu-scale-3.png), [mounted guard with both spider types](compatibility-evidence/mercenary-mounted-world.png), and [final mixed combat/load](compatibility-evidence/mercenary-mixed-load-owner.png).

## Repeating the live fixture

Build the optional test helper with `./gradlew multiplayerTestJar --max-workers=2`. Provision a disposable local server and two matching clients named FFMOwner/FFMGuest, with the release, original Mercenaries, and helper JARs installed on all three. Use the test-only controls documented in [the earlier validation guide](VALIDATION.md#repeating-the-live-checks).

In the retained cloud fixture, start Xorg as documented there, then start the three roles with:

```sh
FFM_EXTRA_MOD=/absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar \
  python /workspace/multiplayer/launch.py server
# In separate processes, use owner and guest instead of server.
```

The launcher reads the current Mobs version from `gradle.properties`. Its disposable `mods` directories retain extra manually installed JARs, so inspect the actual loaded mod list when changing profiles.

Run `tools/multiplayer/control.py`, `suite.py`, `mercenaries.py`, and `mercenaries_races.py`. For restart verification, preserve `mercenaries-restart.json`, disconnect both clients, stop/save the server normally, restart the same world, and run `mercenaries_restart.py`. After all Gradle/reference-test work finishes, run `mercenaries_load.py`, then `logout.py`. These scripts modify fixture inventories/entities/world blocks; use the disposable test world. File control is local IPC, not a production network feature.

## Limits and observed environment messages

No mod crash, watchdog termination, mixin application failure, registry synchronization failure, inventory duplication, remaining observed inventory desync, missing model, invalid observed pose, or GUI defect was found in the completed final checks. A finite test run cannot guarantee their absence in every deployment.

The test server was bound only to `127.0.0.1:25577`, used offline test identities and disabled secure-profile enforcement, and used the user's explicit EULA acceptance for this local fixture. Online account authentication/secure chat were not tested: the cloud network blocked Minecraft's services-public-key request, producing authlib errors and missing-chat-key warnings. Software rendering used Xorg and Mesa llvmpipe at 1280×720; native-GPU/driver combinations, other operating systems, high latency/packet loss, other modpacks, and long-duration public-server load remain untested.

Loader asset-URL warnings and a vanilla shader-sampler warning were logged. One client also logged a nonfatal OpenAL `Stop: Invalid name parameter` cleanup error during resource reload on the null audio driver; reload, rendering, and inventory checks completed successfully. Physical audio output was not validated, and this message is not being represented as a clean audio-driver test. Test-harness corrections included holding the actual shift key for GUI interaction, counting disconnected cursor items across every legitimate holder (vanilla drops them and nearby guards can pick them up), and allowing guard hunger to advance naturally across running-server time. These were diagnosed rather than treated as product defects.

An earlier load measurement included a test-platform fill command during sampling (maximum tick 1,106 ms); it is preserved as fixture-contaminated evidence rather than a clean performance benchmark. The final measurement excludes setup commands and concurrent Gradle/reference work.
