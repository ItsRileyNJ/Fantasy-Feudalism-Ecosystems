# Ecosystems 0.1.21 — achievements

Adds seventeen native Minecraft advancements. Progress is saved by Minecraft and synchronizes to clients; completion uses normal toasts and chat announcements. There are no new item or experience rewards. The three rejected proposals are excluded.

| Achievement | Earn it by |
| --- | --- |
| Bottled Buzz | Capture a bee in a bottle. |
| Friendly Fangs | Successfully tame a spider. |
| Eight-Legged Alliance | Tame both a regular spider and a cave spider. |
| Silk Supplier | Successfully harvest string from a tamed spider. |
| Venom on Tap | Successfully harvest poison from a tamed spider. |
| Night Collector | Capture a bat in a Bat Net. |
| Full House | Successfully insert the third resident into a Bat Enclosure. |
| Natural Helpers | Feed a housed bat an apple that produces fertilizer. |
| Cat Nap | Wake with Rested after sleeping beside an eligible cat. |
| Pest Control | Have your tamed cat kill a Field Mouse. |
| A Pact in the Dark | Successfully bind a phantom with an Eye of Ender at night. |
| Watch Dog | Have your sitting tamed wolf prevent a livestock hunt. |
| New Arrival | Have an owned horse produce a foal through automatic grazing-triggered breeding. |
| Shield Breaker | Disable a raised wither skeleton shield with an axe. |
| That's where I'd be | Take damage from a skeleton arrow fired at the skeleton's maximum 35-block range. |
| Everyone's got a plan until... | Die to one direct Enderman hit while at full health. |
| Master Naturalist | Complete all sixteen other achievements. |

The tree is anchored on Bottled Buzz. Earning another achievement can reveal the tab without automatically granting Bottled Buzz. There is no achievement just for joining. Pet/horse actions credit their online owners; harvesting and capture credit the acting player. Both spider species are remembered separately.

Skeleton range is measured between the skeleton and its intended player target at firing time, using the same entity-position distance as the mod's firing limit. A 0.0001-block tolerance accommodates floating-point positions. Shots over the limit do not count. The recorded shooter, intended target, and distance survive arrow saving/loading; walking closer after firing cannot turn a short shot into a qualifying shot. Intercepting an arrow intended for another player does not count.

Previously a standard Enderman hit on Hard dealt 14.175 damage to an unarmored player. Hard direct melee now deals 21 damage before normal armor/effect mitigation. Easy and Normal retain their previous 6.075 and 9.45 damage. The achievement requires an actual death from full maximum health, including custom maximum health. A nonfatal hit, a fatal hit from partial health, or a successful Totem of Undying does not qualify. Damage cancellation and normal mitigation still apply.

Award checks run on existing successful actions and damage/advancement events. No new per-tick entity searches or AI goals are added. Master Naturalist also reconciles existing completion once on login.

## Validation

- **190/190 standalone native GameTests passed**, including all seventeen advancement definitions, successful action hooks, failed-action controls, per-owner credit, both spider criteria, automatic-versus-manual breeding, and all sixteen Master Naturalist prerequisites.
- **203/203 Ecosystems and integration GameTests passed with Mercenaries 1.15.7 installed.** This includes an actual skeleton arrow flying 35 blocks and hitting its target, recorded launch-distance boundaries, arrow save/load, full/partial-health Enderman attacks, iron armor, totems, unchanged Easy/Normal damage, cat sleep, and raised-shield axe disabling.
- **Dedicated server plus two graphical clients**, running both release JARs: real client bee capture, separate cat ownership, no join reward, native advancement screens on both clients, native Hard Enderman death and respawn, and progress preserved through both players disconnecting and reconnecting.
- Confirmed the guest can see the Ecosystems tree after earning Pest Control without earning Bottled Buzz. Inspected the native tree on both clients; translated titles, icons, backgrounds, completed frames, and descriptions rendered.
- The release contains exactly seventeen advancement JSON files and excludes GameTests, their structure, and the separate multiplayer harness. Build and `git diff --check` passed.

Evidence: [test summaries](achievements-0.1.21-evidence.txt), [multiplayer observations](achievements-0.1.21-live-evidence.json), [owner screen](achievements-0.1.21-owner.png), [guest screen](achievements-0.1.21-guest.png). The live test is reproducible with `tools/multiplayer/achievements_0_1_21.py` in the existing disposable multiplayer environment.

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.21.jar` (334,105 bytes), SHA-256 `1bda5ce29fef6040bffdc821103eb917d3a85e34b22c0a586828b01d64a7c11a`.

The separate Mercenaries native suite was not rerun for this change; its previously documented mounted-retreat failure and pre-start chunk-readiness stall remain outside these passing Ecosystems results. Offline test identities could not contact Minecraft authentication services; both clients used the existing disposable offline server. This is not a test of every possible modpack.
