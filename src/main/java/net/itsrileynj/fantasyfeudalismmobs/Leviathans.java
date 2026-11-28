package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class Leviathans {
    public static final String NAME = "leviathan";
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID, NAME);
    public static final DeferredHolder<EntityType<?>, EntityType<Leviathan>> ENTITY = FantasyFeudalismMobs.ENTITIES.register(NAME,
            () -> EntityType.Builder.of(Leviathan::new, MobCategory.MONSTER).sized(1.65F, 2.25F)
                    .eyeHeight(1.42F).clientTrackingRange(10).updateInterval(2).build(ID.toString()));
    public static final DeferredItem<DeferredSpawnEggItem> EGG =
            FantasyFeudalismMobs.ITEMS.register("leviathan_spawn_egg",
                    () -> new DeferredSpawnEggItem(ENTITY, 0x233044, 0xA6A0AB, new Item.Properties()));

    private Leviathans() {}

    static void register(IEventBus bus) {
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(ENTITY.get(), Leviathan.attributes().build()));
        bus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(ENTITY.get(), SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR, (type, level, reason, pos, random) -> canSpawn(level, reason, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }

    private static boolean canSpawn(ServerLevelAccessor level, MobSpawnType reason, BlockPos pos) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) return true;
        return level.getBiome(pos).is(BiomeTags.IS_DEEP_OCEAN) && pos.getY() <= level.getSeaLevel() - 22
                && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                && level.getFluidState(pos.above(2)).is(FluidTags.WATER);
    }
}
