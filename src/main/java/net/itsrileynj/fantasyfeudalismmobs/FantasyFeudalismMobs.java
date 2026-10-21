package net.itsrileynj.fantasyfeudalismmobs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.Foods;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

@Mod(FantasyFeudalismMobs.MOD_ID)
public final class FantasyFeudalismMobs {
    public static final String MOD_ID = "fantasy_feudalism_mobs";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
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
                })
                .build());
    }

    public FantasyFeudalismMobs(IEventBus modEventBus, ModContainer container) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        modEventBus.addListener(FeudalEvents::spawnPlacements);
        container.registerConfig(ModConfig.Type.SERVER, FeudalConfig.SPEC);
        NeoForge.EVENT_BUS.register(FeudalEvents.class);
    }
}
