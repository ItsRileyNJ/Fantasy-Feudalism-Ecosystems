package net.itsrileynj.fantasyfeudalismmobs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

@Mod(FantasyFeudalismMobs.MOD_ID)
public final class FantasyFeudalismMobs {
    public static final String MOD_ID = "fantasy_feudalism_mobs";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> BEE_BOTTLE = ITEMS.registerSimpleItem("bee_bottle");

    public FantasyFeudalismMobs(IEventBus modEventBus, ModContainer container) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(FeudalEvents::spawnPlacements);
        container.registerConfig(ModConfig.Type.SERVER, FeudalConfig.SPEC);
        NeoForge.EVENT_BUS.register(FeudalEvents.class);
    }
}
