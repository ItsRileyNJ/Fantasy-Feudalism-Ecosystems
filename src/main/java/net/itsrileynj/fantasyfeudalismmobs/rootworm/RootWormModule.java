package net.itsrileynj.fantasyfeudalismmobs.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Call register once from the destination mod constructor, after retargeting MOD_ID to that mod. */
public final class RootWormModule {
    public static final String MOD_ID = "fantasy_feudalism_mobs";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RootWorm>> ROOT_WORM = ENTITIES.register("root_worm", () ->
            EntityType.Builder.of(RootWorm::new, MobCategory.CREATURE).sized(0.7F, 0.45F).clientTrackingRange(8)
                    .build(id("root_worm").toString()));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> ROOT_WORM_EGG = ITEMS.register("root_worm_spawn_egg", () ->
            new DeferredSpawnEggItem(ROOT_WORM, 0x59412D, 0xA58A5C, new Item.Properties()));

    public static void register(IEventBus bus, ModContainer container) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        // A separate filename allows the destination mod to keep its own existing COMMON config.
        container.registerConfig(ModConfig.Type.COMMON, RootWormConfig.SPEC, MOD_ID + "-root-worm-common.toml");
        bus.addListener(RootWormModule::attributes);
        bus.addListener(RootWormModule::placements);
        bus.addListener(RootWormModule::creativeItems);
        NeoForge.EVENT_BUS.addListener(RootWormPart::redirectPlayerAttack);
        NeoForge.EVENT_BUS.addListener(net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormHearing::onLivingJump);
        NeoForge.EVENT_BUS.addListener(net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWormHearing::onEquipmentChange);
    }
    private static void attributes(EntityAttributeCreationEvent event) { event.put(ROOT_WORM.get(), RootWorm.createAttributes().build()); }
    private static void placements(RegisterSpawnPlacementsEvent event) {
        // Normal Minecraft ground-spawn rules only. The destination biome's spawn list decides WHERE.
        event.register(ROOT_WORM.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mob::checkMobSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    private static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) event.accept(ROOT_WORM_EGG.get());
    }
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
    private RootWormModule() {}
}
