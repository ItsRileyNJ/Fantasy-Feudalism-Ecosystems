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
                })
                .build());
    }

    public FantasyFeudalismMobs(IEventBus modEventBus, ModContainer container) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(FeudalEvents::spawnPlacements);
        container.registerConfig(ModConfig.Type.SERVER, FeudalConfig.SPEC);
        NeoForge.EVENT_BUS.register(FeudalEvents.class);
    }
}
