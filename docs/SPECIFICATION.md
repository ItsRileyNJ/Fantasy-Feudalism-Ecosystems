# User specification

Fantasy Feudal Mobs
Create a Minecraft mod named Fantasy Feudal Mobs.
Do not add, remove, reinterpret, or expand features beyond what is explicitly described in this specification.
If implementation requires information that is not provided here, ask for clarification instead of guessing.
General Requirements
• All new item textures should closely follow the visual style of vanilla Minecraft.
• All new features should use existing vanilla Minecraft systems and mechanics where possible.
• Any commands added by this mod must begin with:
/ffms
• Do not invent additional commands, configuration options, items, behaviors, effects, drops, recipes, advancements, sounds, particles, or mechanics unless specifically requested.
• Where this specification does not define a value or behavior, ask before choosing one.

───

Spider Changes
Wild Spider Behavior
• Spiders cannot swim.
• Spiders spawn 50% more rarely than normal.
• Spiders can jump up to 3 blocks.
• Spiders should intentionally use their 3-block jump when attacking.
• Spider pathfinding AI should also be capable of using this jump when determining how to reach a destination.
• Wild spiders will eat bees.
• Cave Spiders will eat silverfish.

───

Spider Taming
Both regular spiders and cave spiders can be tamed using a new item called the Bee Bottle.
Bee Bottle
Add a new item named:
Bee Bottle
The Bee Bottle is created by capturing a Minecraft bee in a bottle.
A Bee Bottle can be fed to:
• Spiders
• Cave Spiders
Feeding a Bee Bottle to either mob allows it to be tamed.
Tamed spiders should use the same general mechanics as a tamed wolf except where this specification explicitly changes their behavior.

───

Tamed Spider Behavior
General Behavior
• Tamed spiders follow their owner using the same general mechanics as a tamed wolf.
• Tamed spiders cannot swim.
• Tamed spiders avoid:
◦ Lava
◦ Water
◦ Fire
• Tamed spiders only make noise when hostile mobs are within 16 blocks.
• Tamed spiders display the vanilla heart effect when fed.
• Tamed spiders cannot breed.
• Tamed spiders take 25% more damage from fire.
• Tamed spiders can be healed by feeding them uncooked meat of any type.
Health and Speed
A tamed spider has:
• 75% of the health of a tamed wolf
• 20% greater movement speed than a wolf
Jumping
Tamed spiders can jump up to 3 blocks.
They should intentionally use this jump:
• While attacking enemies
• While pathfinding
The spider AI should account for its ability to jump three blocks when determining paths.

───

Spider Harvesting
Tamed spiders can be harvested for:
• Bottles of poison
• Silk
Poison
When the player is holding a bottle and right-clicks a tamed spider:
• The player harvests poison from the spider.
Silk
When the player right-clicks a tamed spider using shears:
• The player harvests silk from the spider.

───

Tamed Spider Combat Effects
When a tamed regular spider attacks a hostile mob, it applies:
• Slowness
• Weakness
These effects are not applied to:
• Skeletons
• Wither Skeletons
• Spiders
• Cave Spiders
• Zombies

───

Tamed Cave Spider Combat Effects
When a tamed cave spider attacks a hostile mob, it applies:
• Slowness
• Weakness
• Poison
These effects are not applied to:
• Skeletons
• Wither Skeletons
• Spiders
• Cave Spiders
• Zombies

───

Spider Neutrality
Regular spiders and cave spiders become neutral toward a player while that player has a tamed spider following them.

───

Creeper Changes
Spawning
Creepers should be less common than in vanilla Minecraft.
Explosions
Creeper explosions no longer break blocks.
Instead, when a creeper explodes:
• Moss carpet is left around the explosion area.
• Ferns are left around the explosion area.
• Tall grass is left around the explosion area.
• Blocks within the explosion radius are converted to their mossy variants when an applicable mossy variant exists.
The block-conversion portion of creeper explosions must be configurable.
Do not remove the other listed explosion behavior unless explicitly requested.
Drops
When killed, creepers drop:
• 1–2 Moss Blocks and the normal Gunpowder



───

Skeleton Changes
Targeting
Skeletons can detect and attack targets from up to 35 blocks away.
Skeletons should continue targeting the same target unless the skeleton itself is in danger.
Projectiles
Skeleton projectiles travel 40% faster than normal.

───

Skeleton Crossbows
Skeletons have a chance to spawn with a crossbow.
Skeletons equipped with crossbows must have AI capable of properly using the crossbow.
Crossbow-equipped skeletons should:
• Target enemies the same way as normal skeletons.
• Shoot at the same targeting distances as normal skeletons under this mod's modified skeleton behavior.

───

Enderman Changes
When an Enderman is provoked:
• It attacks the player only once.
• That attack deals 35% more damage than normal.
Endermen no longer pick up blocks.
This behavior must be configurable.

───

Zombie Changes
Group Spawning
Zombies spawn in groups of:
4–9 zombies
Individual Variation
Zombies have variations in:
• Movement speed
• Attack damage
Low-Health Behavior
When a zombie falls below 3 hearts:
• Its movement speed is doubled.

───

Phantom Changes
Phantoms only attack a player when that player has 4 hearts or less.
On Hard difficulty, phantoms spawn with:
Swiftness I

───

Hostile Mob Armor Changes
All enemies have a higher chance of spawning with armor depending on the current difficulty.
Armor spawn chance:
• Easy: 20%
• Normal: 35%
• Hard: 60%

───

Blaze Changes
Blazes can perform an aerial dash from side to side to avoid being hit.
Requirements:
• The dash moves the Blaze back and forth through the air.
• The purpose of the dash is to avoid incoming attacks.
• A Blaze can dash no more than once every 5 seconds.
• Blaze dashing must be configurable.

───

Ghast Changes
Ghasts are 20% more rare than normal.
When Ghasts spawn, they spawn in groups of:
3 Ghasts
When killed, a Ghast drops:
2 Ghast Tears

───

Silverfish Changes
Silverfish can naturally spawn in abandoned mineshafts.
They spawn in groups of:
3–9 Silverfish
Silverfish will attack zombies.
Zombies are weak to Silverfish.

───

Configuration Requirements
The following features must be configurable:
1. Creeper block conversion during explosions.
2. Endermen picking up blocks.
3. Blaze aerial dashing.
Do not create additional configuration options unless specifically requested.

───

Command Requirements
Any command introduced by Fantasy Feudal Mobs must use the prefix:
/ffms
Do not invent commands solely because the prefix exists. Only add commands when a command is required by a specified feature.

───

Implementation Restrictions
Codex should follow these rules while implementing the mod:
• Do not add mechanics that are not explicitly described.
• Do not remove mechanics that are explicitly described.
• Do not invent item drops beyond those listed.
• Do not invent mob spawning percentages beyond those listed.
• Prefer vanilla Minecraft mechanics and systems whenever they can reproduce the requested behavior.
• If any required implementation detail cannot be determined from this specification without making a design decision, stop and ask for clarification.



## Confirmed clarifications

1. Bee capture: right-click a bee with a glass bottle. Each Bee Bottle feeding has a 33% taming chance and returns an empty bottle, including on failure.
2. Healing includes all raw meats, raw fish, and rotten flesh. Use the specified general vanilla wolf mechanics for healing amounts.
3. Silk is vanilla string, 1–4 per harvest. Poison is one standard vanilla Poison potion. Each has a separate 48,000-game-tick (two Minecraft days) cooldown per spider. Shearing costs one durability per successful harvest.
4. Tamed-spider attack effects are all level I and last 120 ticks (six seconds).
5. Creeper spawning is reduced by 60%. Vegetation has 75% coverage of eligible positions, weighted 50% moss carpet / 35% ferns / 15% tall grass. The user authorized the biased vegetation split. Vanilla plant-survival rules restrict placement.
6. Skeleton equipment: 15% crossbows, 5% swords, 80% bows. Sword selection: 80% stone / 20% iron. Sword users use melee AI. Ranged skeletons may change targets when any player is within five blocks.
7. Endermen disengage and teleport using the vanilla teleport range after one successful hit. They can immediately be provoked again.
8. User delegated zombie variation: independently uniform 80–120% movement speed and attack damage. Low-health speed doubles the individual's resulting speed below six health points.
9. Hard-spawned phantoms receive infinite Swiftness I.
10. Armor only applies to undead mobs that normally wear it. Roll eligibility once per mob: Easy 20%, Normal 35%, Hard 60%. Material weights: leather 50%, chainmail 25%, iron 20%, gold 5%. Iron is the strongest possible spawn armor. Use vanilla armor-slot selection. Per piece: 50% unenchanted, 35% one level I enchantment, 15% one level II enchantment, using applicable vanilla enchanting-table enchantments.
11. Blaze dash: four blocks over ten ticks (eight blocks/second), no more than once every 100 ticks. User delegated chance and interval: 20% each second with an attack target, after cooldown. The dash alternates sides and respects collisions.
12. User delegated silverfish spawn/weakness values: mineshaft spawn weight 60, groups 3–9, vanilla hostile light rules; silverfish deal double damage to zombies. Silverfish also flee both regular and cave spiders, wild or tamed.
13. Changes include the corresponding vanilla mob variants, including strays, bogged, husks, and drowned.

Vanilla systems retained: spawn weighting and placement validity, wolf follow/sit/teleport thresholds, item/bottle inventory exchange and overflow, standard Poison potion contents, normal loot outside explicit overrides, and normal Minecraft equipment/drop behavior. No new commands are requested.


## Approved follow-up: creative tab and Bee Bottle artwork

The user requested a Fantasy Feudal Mobs creative inventory tab and a small black-and-yellow two-pixel bee inside the Bee Bottle. Version 0.1.2 adds the tab with the Bee Bottle as its icon and only mod item, and overlays exactly two opaque bee pixels on the vanilla glass bottle. This supersedes the original vanilla-only bottle artwork.


## Additional approved follow-ups in 0.1.2

- Tamed regular spiders retain the vanilla texture pattern with a subtle brown tint. Tamed cave spiders retain the vanilla pattern with a blue tint. Wild spiders retain vanilla textures.
- Spider combat jumps should favor a forward leap over a vertical jump. Use a low pounce on level ground while retaining the three-block obstacle/pathfinding capability.
- Skeletons holding crossbows should hold them like pillagers, including a two-handed idle pose and the vanilla charging animation.
- Skeletons must stop targeting players who change to Creative mode; Spectator mode also remains protected.
- Investigate reported zombie chase hesitation, with attention to pathfinding and repeated movement checks.


## Additional spider follow-ups in 0.1.2

- Verify the approved six-second level I companion combat effects and existing family exclusions against actual successful attacks; rejected hits must not apply them.
- Following companions must keep their combat target after surviving fall damage. Damage should wake a sitting companion without reapplying the follow order to one that is already following.
- Attack leaps should start only around three blocks from the target. Use a narrow 2.75–3.25-block distance window to accommodate moving entities and land within melee reach instead of passing the target. Navigation jumps for obstacles remain available separately.
- Both wild and tamed regular/cave spiders are immune to falls strictly shorter than nine blocks. Falls of nine blocks or more retain normal damage.
- A sitting tamed spider's large rear abdomen should lower slightly. Restore the normal pose on following; other players must see the same state.
- Investigate silverfish natural spawning in abandoned mineshafts. Preserve the agreed weight 60, groups 3–9, dark hostile placement rules, and mineshaft restriction.
- A wild spider that kills/eats a bee fully heals. A nonfatal hit does not count as eating it; tamed spiders retain their feeding rules.

## Sitting-pose correction in 0.1.3

The user clarified that the tamed spider's abdomen should angle downward slightly instead of being translated vertically. Apply a slight downward pitch to the rear abdomen of sitting regular and cave spiders, restoring its normal angle when following. Wild spiders retain their normal pose.
