package net.itsrileynj.fantasyfeudalismmobs;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.registries.*;

public final class Birds {
    public static final DeferredHolder<EntityType<?>,EntityType<ForestBird>> ROBIN=entity(BirdKind.ROBIN,.38F,.53F);
    public static final DeferredHolder<EntityType<?>,EntityType<ForestBird>> BLUE_JAY=entity(BirdKind.BLUE_JAY,.4F,.64F);
    public static final DeferredHolder<EntityType<?>,EntityType<ForestBird>> GOLDFINCH=entity(BirdKind.GOLDFINCH,.34F,.48F);
    public static final DeferredHolder<EntityType<?>,EntityType<ForestBird>> CROW=entity(BirdKind.CROW,.48F,.66F);
    public static final List<DeferredHolder<EntityType<?>,EntityType<ForestBird>>> TYPES=List.of(ROBIN,BLUE_JAY,GOLDFINCH,CROW);
    public static final List<DeferredItem<DeferredSpawnEggItem>> EGGS=List.of(egg(ROBIN,"robin",0x77533B,0xC76625),egg(BLUE_JAY,"blue_jay",0x315DB8,0xE8E2D4),
            egg(GOLDFINCH,"goldfinch",0xEFC62B,0x26262B),egg(CROW,"crow",0x25252E,0x565661));
    public static final TagKey<Biome> WARM_FORESTS=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"warm_forest_birds"));
    public static final TagKey<Biome> SPRUCE_FORESTS=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"spruce_forest_birds"));
    private Birds() {}
    private static DeferredHolder<EntityType<?>,EntityType<ForestBird>> entity(BirdKind kind,float width,float height) {
        return FantasyFeudalismMobs.ENTITIES.register(kind.id,()->EntityType.Builder.<ForestBird>of((type,level)->new ForestBird(type,level,kind),MobCategory.CREATURE)
                .sized(width,height).eyeHeight(height*.82F).clientTrackingRange(8).updateInterval(2).build(FantasyFeudalismMobs.MOD_ID+":"+kind.id));
    }
    private static DeferredItem<DeferredSpawnEggItem> egg(DeferredHolder<EntityType<?>,EntityType<ForestBird>> type,String name,int base,int spots) {
        return FantasyFeudalismMobs.ITEMS.register(name+"_spawn_egg",()->new DeferredSpawnEggItem(type,base,spots,new Item.Properties()));
    }
    public static void bootstrap() { }
    public static void attributes(EntityAttributeCreationEvent event) {
        for(int i=0;i<TYPES.size();i++)event.put(TYPES.get(i).get(),ForestBird.attributes(BirdKind.values()[i]).build());
    }
    public static BirdKind kind(EntityType<?> type) {
        for(int i=0;i<TYPES.size();i++)if(TYPES.get(i).get()==type)return BirdKind.values()[i];
        throw new IllegalArgumentException("Not a forest bird: "+type);
    }
    public static boolean spawnRules(EntityType<ForestBird> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        if(reason!=MobSpawnType.NATURAL&&reason!=MobSpawnType.CHUNK_GENERATION)return true;
        if(!level.getBiome(pos).is(kind(type)==BirdKind.CROW?SPRUCE_FORESTS:WARM_FORESTS)||level.getRawBrightness(pos,0)<=8)return false;
        var support=level.getBlockState(pos.below());
        return (support.is(BlockTags.ANIMALS_SPAWNABLE_ON)||support.is(BlockTags.DIRT)||support.is(BlockTags.LEAVES)||support.is(Blocks.SNOW)||support.is(Blocks.SNOW_BLOCK))
                &&level.getFluidState(pos).isEmpty()&&level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();
    }
    public static void placements(RegisterSpawnPlacementsEvent event) {
        for(var type:TYPES)event.register(type.get(),SpawnPlacementTypes.NO_RESTRICTIONS,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Birds::spawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
