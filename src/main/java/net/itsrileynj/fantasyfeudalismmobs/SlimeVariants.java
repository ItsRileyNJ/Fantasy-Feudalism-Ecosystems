package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SlimeVariants {
    public static final DeferredHolder<EntityType<?>, EntityType<VariantSlime>> FLAME =
            FantasyFeudalismMobs.ENTITIES.register("flame_slime", () -> slime().fireImmune().build(id("flame_slime").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VariantSlime>> SNOW =
            FantasyFeudalismMobs.ENTITIES.register("snow_slime", () -> slime().build(id("snow_slime").toString()));

    public static final DeferredItem<Item> BLUE_SLIME = FantasyFeudalismMobs.ITEMS.registerSimpleItem("blue_slime");
    private static final DeferredItem<DeferredSpawnEggItem> FLAME_EGG = FantasyFeudalismMobs.ITEMS.register(
            "flame_slime_spawn_egg", () -> new DeferredSpawnEggItem(FLAME, 0xD95814, 0xFFD850, new Item.Properties()));
    private static final DeferredItem<DeferredSpawnEggItem> SNOW_EGG = FantasyFeudalismMobs.ITEMS.register(
            "snow_slime_spawn_egg", () -> new DeferredSpawnEggItem(SNOW, 0x2456A9, 0xA0EAFF, new Item.Properties()));

    static final DeferredHolder<MobEffect, FreezingEffects.InstantFreezing> FREEZING =
            FantasyFeudalismMobs.EFFECTS.register("freezing", FreezingEffects.InstantFreezing::new);
    static final DeferredHolder<MobEffect, FreezingEffects.Frozen> FROZEN =
            FantasyFeudalismMobs.EFFECTS.register("frozen", FreezingEffects.Frozen::new);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, FantasyFeudalismMobs.MOD_ID);
    public static final DeferredHolder<Potion, Potion> FREEZING_POTION = POTIONS.register("freezing",
            () -> new Potion(new MobEffectInstance(FREEZING, 1)));

    private SlimeVariants() {}

    private static EntityType.Builder<VariantSlime> slime() {
        return EntityType.Builder.of(VariantSlime::new, MobCategory.MONSTER).sized(.52F, .52F).clientTrackingRange(8);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID, path);
    }

    static void register(IEventBus modBus) {
        POTIONS.register(modBus);
        modBus.addListener(SlimeVariants::attributes);
        modBus.addListener(SlimeVariants::placements);
        modBus.addListener(SlimeVariants::spawnEggTab);
        NeoForge.EVENT_BUS.addListener(SlimeVariants::brewing);
        NeoForge.EVENT_BUS.addListener(SlimeVariants::snowSlimeHit);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(FLAME.get(), Monster.createMonsterAttributes().build());
        event.put(SNOW.get(), Monster.createMonsterAttributes().build());
    }

    private static void placements(RegisterSpawnPlacementsEvent event) {
        event.register(FLAME.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                SlimeVariants::canSpawnFlame, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SNOW.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
                        && (MobSpawnType.ignoresLightRequirements(reason) || Monster.isDarkEnoughToSpawn(level, pos, random))
                        && Mob.checkMobSpawnRules(type, level, reason, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    static boolean canSpawnFlame(EntityType<VariantSlime> type, ServerLevelAccessor level, MobSpawnType reason,
                                 BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        return level.getFluidState(pos).is(FluidTags.LAVA)
                && level.isEmptyBlock(pos.above())
                && level.isEmptyBlock(pos.above(2))
                && !level.canSeeSky(pos.above())
                && pos.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 4;
    }

    static void addToCreativeTab(CreativeModeTab.Output output) {
        output.accept(FLAME_EGG.get());
        output.accept(SNOW_EGG.get());
        output.accept(BLUE_SLIME.get());
        for (Item item : new Item[]{Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW}) {
            output.accept(PotionContents.createItemStack(item, FREEZING_POTION));
        }
    }

    private static void spawnEggTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(FLAME_EGG.get());
            event.accept(SNOW_EGG.get());
        }
    }

    private static void brewing(RegisterBrewingRecipesEvent event) {
        event.getBuilder().addMix(Potions.AWKWARD, BLUE_SLIME.get(), FREEZING_POTION);
    }

    private static void snowSlimeHit(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0 && event.getSource().getDirectEntity() instanceof VariantSlime slime
                && !slime.isFlame() && event.getEntity().canFreeze()) {
            var target = event.getEntity();
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze()));
        }
    }
}
