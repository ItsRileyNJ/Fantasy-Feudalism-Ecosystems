# Fantasy Feudalism - Ecosystems

Make Minecraft's wildlife, companions and monsters feel more connected. **Fantasy Feudalism - Ecosystems** adds animal herds, coordinated predators, livestock guardians, tamable spiders and new behaviors for familiar hostile mobs, using vanilla-style items, animations and mechanics.

## Wildlife that lives together

- **Cow and horse herds:** Animals that spawn together stay together, walking back to their herd when separated. Herd membership survives saving and reloading.
- **Horse grazing:** Horses eat grass using their existing head-down animation. Grazing consumes grass when mob griefing permits it.
- **Automatic horse breeding:** Eligible adults can breed after a **2-hour-30-minute cooldown**, triggered by grazing near another eligible horse. Each successful birth starts a new cooldown for both parents. Normal golden-apple and golden-carrot breeding remains available with vanilla rules.
- **Slower-growing foals:** Horses take **three times longer** to grow up naturally—about **60 minutes at normal tick speed**. Feeding still advances growth.
- **Horse meat:** Adult horses drop **1–2 Raw Horse**. Cook it in a furnace or smoker to make **Cooked Horse**, which has the same food values as cooked mutton.
- **Skittish chickens:** Chickens flee Survival and Adventure players who approach within **three blocks**, running at **95% of normal player sprint speed**.
- **Water-seeking frogs:** A visible player within **six blocks** makes a frog on land seek nearby water and jump toward it. Frogs search within twelve blocks and walk closer before jumping when needed. Creative players count; spectators do not. Breeding and egg laying take priority.
- **Village play:** Baby villagers occasionally chase wild village cats for about **eight seconds**, without hurting them.

## Coordinated predators and livestock guardians

**Wild wolves hunt sheep as a pack.** Pack members stay near one another, gather before a hunt and begin stalking together from up to fifteen blocks away. Their crouched stalking animation lowers the body, head and tail. Once a pack member gets within seven blocks, the pack charges at full speed.

When a wolf kills a sheep, its entire pack receives a **one-Minecraft-day hunting cooldown**—twenty minutes at normal tick speed. Foxes also receive a one-day hunting cooldown after a successful prey kill.

**Sitting tamed wolves protect livestock.** A seated dog within **24 blocks** of a sheep discourages wild wolves; a seated dog near chickens discourages foxes. Protection can interrupt a hunt already underway and starts the deterred predator's hunger cooldown, shared across a stopped wolf pack.

Stopping a pack or fox tires only the nearest guardian, costing **one heart** without a hurt flash, damage sound or forcing it to stand. Its lowered tail shows the health change, and normal feeding heals it. Fatigue cannot kill the dog. Ordinary tamed-wolf orders, owner defense and breeding remain available.

## Tame your own spider companions

Capture a bee by **right-clicking it with a glass bottle** to make a **Bee Bottle**. The bottle has a tiny black-and-yellow bee inside its vanilla-style artwork.

Feed a Bee Bottle to a regular or cave spider for a **33% taming chance**. Each feeding returns an empty bottle, including unsuccessful attempts.

Tamed spiders:

- Follow their owner, use familiar wolf-style owner teleporting, accept leashes and can be ordered to sit or follow. A sitting spider angles its abdomen downward.
- Defend their owner and help attack the owner’s targets, while respecting friendly pets and allies.
- Have **30 health points / 15 hearts** and move **20% faster than a wolf**.
- Heal when fed raw meat, raw fish or rotten flesh, showing vanilla heart particles. They cannot breed.
- Use a subtle **brown texture** for regular spiders and a **blue tint** for cave spiders.
- Avoid water, lava and fire. They cannot swim and take **25% extra fire damage**.
- Make their own sounds only when hostile mobs are within **sixteen blocks**.
- Make nearby wild spiders neutral toward their owner while following them.

**Combat companions:** Successful regular-spider attacks apply **Slowness I and Weakness I** to eligible hostile enemies for **six seconds**. Cave spiders also apply **Poison I**. Matching vanilla effect particles appear on the enemy. Skeleton, zombie and spider families are excluded; normal effect immunities still apply.

**Harvest renewable resources:** Right-click a tamed spider with shears for **1–4 vanilla string**, costing one shears durability, or use a glass bottle to collect **one vanilla Poison potion**. String and poison have **separate two-Minecraft-day cooldowns** per spider—forty minutes each at normal tick speed. Cooldowns survive saving and reloading.

**Wild spiders change too:** All spiders cannot swim, and spider-family spawn weights are halved. Regular spiders hunt bees and fully heal after killing one; cave spiders hunt silverfish. Wild and tamed spiders can navigate three-block obstacles and use forward attack pounces when around three blocks from a target. Both types ignore fall damage from falls **shorter than nine blocks**.

## Reworked monsters

- **Creepers:** Spawn weights are **60% lower**. Explosions still hurt entities but do not destroy terrain. Instead, they convert suitable blocks to mossy variants and scatter moss carpet, ferns and tall grass across roughly **75% of eligible positions**, favoring moss carpet and ferns. Killed creepers also drop **1–2 moss blocks** alongside normal gunpowder.
- **Skeleton families:** Detect and shoot targets from up to **35 blocks**, with **40% faster projectiles**. Ordinary skeleton equipment rolls are **80% bows, 15% crossbows and 5% swords**. Sword users use melee AI, with an **80% stone / 20% iron** sword split. Crossbows use charging and firing AI with pillager-style holding poses. Ranged skeletons retain valid targets unless threatened by a player within five blocks, and release players who enter Creative or Spectator mode.
- **Wither skeletons:** On Hard, **10%** use bows whose arrows deal **half direct damage** and apply **Wither I and Slowness I for five seconds**. Bow users never carry shields. Sword users wield stone, iron or gold swords; **50%** carry a shield. Shield users advance toward a player crouched and blocking, then stand and sprint into melee from **five blocks** away.
- **Zombie families:** Spawn in groups of **4–9**, with individual speed and attack damage ranging from **80–120%** of their normal values. Below **three hearts**, their individual movement speed doubles; healing restores the usual speed.
- **Endermen:** Deal **35% more damage to players**, then disengage and attempt to teleport away after one successful hit. They can be provoked again immediately. Block pickup is disabled by default.
- **Phantoms:** Attack players only when they have **four hearts or less**. Phantoms spawned on Hard receive permanent **Swiftness I**. Right-click one at night with an **Eye of Ender** to consume the eye and gain its defense until sunrise: it attacks creatures that damage you first. At sunrise the pact ends and it burns normally in daylight.
- **Blazes:** Can dash sideways up to **four blocks**, alternating direction and respecting obstacles. Dashes take half a second, have a minimum **five-second cooldown**, and are rolled randomly while fighting.
- **Ghasts:** Spawn weights are **20% lower**, with groups of three where space permits. Each killed ghast drops **two ghast tears**, retaining other normal drops.
- **Silverfish:** Spawn naturally in abandoned mineshafts in requested packs of **4–9**, using normal hostile spawning checks. They prefer zombies as prey and deal **double damage** to them, while fleeing wild and tamed spiders. Only the silverfish that lands a zombie's killing blow burrows; the rest keep roaming. The burrowed killer despawns after **thirty seconds**, leaving its hiding block unchanged.

**Undead equipment:** Zombie and skeleton families that normally wear armor receive a single armor eligibility roll: **20% on Easy, 35% on Normal and 60% on Hard**. Material weights are **50% leather, 25% chainmail, 20% iron and 5% gold**, with iron the strongest spawn tier. Each armor piece has a **35% chance of a level I enchantment**, a **15% chance of a level II enchantment**, or remains unenchanted. Armor slots follow vanilla selection rules.

Family changes apply to relevant variants, including strays, bogged, husks and drowned. Spawn weights change relative spawning chances; actual counts and pack sizes still depend on Minecraft's available space, lighting and mob caps.

## Keep a bat colony

Craft a **Bat Net** with string in every slot except an **iron nugget at the center** and a **stick at the bottom center**. Right-click a bat to capture it, then right-click a **Bat Enclosure** to move it inside. The same reusable net becomes empty again.

The wooden enclosure has a glass front, upper side vents and room for **three bats**. Craft it with **planks across the top and bottom**, **fences at middle-left and middle-right**, and an **apple in the center**. Only the bats actually inside appear hanging behind the glass. At night they leave to fly within **fifteen blocks**; in the morning they return. Their places stay reserved while outside.

Feed housed bats **apples** to heal them and show hearts. Each bat can drop **one Fertilizer every five minutes**, so three ready bats can produce three fertilizers for three apples. The cooldown follows the individual bat through capture, relocation and saves. Full-health bats on cooldown do not waste apples.

**Fertilizer** looks like yellow bone meal and instantly matures crops. Mature plants and obstructed growth do not spend it.

## Items, settings and multiplayer

The **Fantasy Feudalism - Ecosystems** creative tab contains the **Bee Bottle, Raw Horse, Cooked Horse, Bat Net, Bat Enclosure and Fertilizer**.

Server settings let you toggle:

- Creeper mossy-block conversion. Turning it off retains vegetation and protection against block destruction.
- Enderman block pickup.
- Blaze aerial dashing.

**Requires Minecraft 1.21.1, NeoForge 21.1.262 or later within Minecraft 1.21.1, and Java 21. Install the mod on both the server and every client.**

**Fantasy Feudalism: Mercenaries is optional.** Ecosystems includes compatibility for companion alliances, skeleton threat classification and synchronized guard inventories. Validation reports record dedicated-server GameTests and live checks with two graphical clients and Mercenaries **1.15.7**, covering item conservation, GUIs, combat, resource reloads, restarts and mob load. See the versioned reports in the repository for exact test counts and configuration limits.

Previously released as **Fantasy Feudal Mobs**. When updating, replace the old Mobs JAR with Ecosystems. Existing registry IDs and saved data are retained, so existing mod items and companions remain recognized.
