package net.itsrileynj.fantasyfeudalismmobs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.Foods;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

@Mod(FantasyFeudalismMobs.MOD_ID)
public final class FantasyFeudalismMobs {
    public static final String MOD_ID = "fantasy_feudalism_mobs";
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,MOD_ID);
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> MOUSE_AMBIENT=SOUNDS.register("entity.field_mouse.ambient",()->
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID,"entity.field_mouse.ambient")));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> MOUSE_HURT=SOUNDS.register("entity.field_mouse.hurt",()->
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID,"entity.field_mouse.hurt")));
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,MOD_ID);
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>,net.minecraft.world.entity.EntityType<FieldMouse>> FIELD_MOUSE=
            ENTITIES.register("field_mouse",()->net.minecraft.world.entity.EntityType.Builder.of(FieldMouse::new,net.minecraft.world.entity.MobCategory.CREATURE)
                    .sized(.35F,.22F).eyeHeight(.16F).clientTrackingRange(8).build(MOD_ID+":field_mouse"));
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,MOD_ID);
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect,RestedEffect> RESTED=EFFECTS.register("rested",RestedEffect::new);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredBlock<BatEnclosureBlock> BAT_ENCLOSURE=BLOCKS.register("bat_enclosure",()->
            new BatEnclosureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2).sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BatEnclosureBlockEntity>> BAT_ENCLOSURE_ENTITY=
            BLOCK_ENTITIES.register("bat_enclosure",()->BlockEntityType.Builder.of(BatEnclosureBlockEntity::new,BAT_ENCLOSURE.get()).build(null));
    public static final DeferredItem<Item> FERTILIZER=ITEMS.register("fertilizer",()->new FertilizerItem(new Item.Properties()));
    public static final DeferredItem<Item> BAT_NET=ITEMS.register("bat_net",()->new BatNetItem(new Item.Properties()));
    public static final DeferredItem<BlockItem> BAT_ENCLOSURE_ITEM=ITEMS.register("bat_enclosure",()->new BlockItem(BAT_ENCLOSURE.get(),new Item.Properties()));
    public static final DeferredItem<Item> BEE_BOTTLE = ITEMS.registerSimpleItem("bee_bottle");
    public static final DeferredItem<net.neoforged.neoforge.common.DeferredSpawnEggItem> FIELD_MOUSE_SPAWN_EGG=
            ITEMS.register("field_mouse_spawn_egg",()->new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                    FIELD_MOUSE,0xF5F2ED,0xEDA0B3,new Item.Properties()));
    public static final DeferredItem<Item> RAW_HORSE = ITEMS.registerSimpleItem("raw_horse", new Item.Properties().food(Foods.MUTTON));
    public static final DeferredItem<Item> COOKED_HORSE = ITEMS.registerSimpleItem("cooked_horse", new Item.Properties().food(Foods.COOKED_MUTTON));
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    static {
        CREATIVE_TABS.register("fantasy_feudal_mobs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + MOD_ID))
                .icon(() -> BEE_BOTTLE.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(BEE_BOTTLE.get());
                    output.accept(RAW_HORSE.get());
                    output.accept(COOKED_HORSE.get());
                    output.accept(FERTILIZER.get());
                    output.accept(BAT_NET.get());
                    output.accept(BAT_ENCLOSURE_ITEM.get());
                    output.accept(FIELD_MOUSE_SPAWN_EGG.get());
                })
                .build());
    }

    public FantasyFeudalismMobs(IEventBus modEventBus, ModContainer container) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        EFFECTS.register(modEventBus);
        SOUNDS.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(FIELD_MOUSE.get(),FieldMouse.createAttributes().build()));
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS))event.accept(FIELD_MOUSE_SPAWN_EGG.get());
        });
        modEventBus.addListener(FeudalEvents::spawnPlacements);
        container.registerConfig(ModConfig.Type.SERVER, FeudalConfig.SPEC);
        NeoForge.EVENT_BUS.register(FeudalEvents.class);
        NeoForge.EVENT_BUS.register(CatRest.class);
        NeoForge.EVENT_BUS.register(MouseVillages.class);
    }
}
