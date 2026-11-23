package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/** Optional integration: no Farmer's Delight classes are linked when it is absent. */
public final class BarkCompatibility {
    private BarkCompatibility() {}
    public static boolean usesFarmersDelight() { return ModList.get().isLoaded("farmersdelight"); }
    public static void addPacks(net.neoforged.neoforge.event.AddPackFindersEvent event) {
        if(usesFarmersDelight())event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"compat/farmersdelight"),
                net.minecraft.server.packs.PackType.SERVER_DATA,
                net.minecraft.network.chat.Component.literal("Ecosystems: Farmer's Delight bark compatibility"),
                net.minecraft.server.packs.repository.PackSource.BUILT_IN,true,
                net.minecraft.server.packs.repository.Pack.Position.TOP);
    }
    public static Item food() {
        return usesFarmersDelight()
                ? BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight","tree_bark"))
                : FantasyFeudalismMobs.BARK.get();
    }
}
