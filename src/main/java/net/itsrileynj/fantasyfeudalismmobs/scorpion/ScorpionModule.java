package net.itsrileynj.fantasyfeudalismmobs.scorpion;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.*;

public final class ScorpionModule {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,FantasyFeudalismMobs.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,FantasyFeudalismMobs.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,FantasyFeudalismMobs.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<Scorpion>> SCORPION=ENTITIES.register("scorpion",()->EntityType.Builder.of(Scorpion::new,MobCategory.MONSTER)
            .sized(1.4F,.9F).eyeHeight(.42F).clientTrackingRange(8).build(id("scorpion").toString()));
    public static final DeferredHolder<EntityType<?>,EntityType<Scorpion>> CRYSTAPID=ENTITIES.register("lava_scorpion",()->EntityType.Builder.of(Scorpion::new,MobCategory.MONSTER)
            .fireImmune().sized(.35F,.225F).eyeHeight(.105F).clientTrackingRange(8).build(id("lava_scorpion").toString()));
    public static final DeferredHolder<Item,DeferredSpawnEggItem> CRYSTAPID_EGG=ITEMS.register("lava_scorpion_spawn_egg",()->new DeferredSpawnEggItem(CRYSTAPID,0x4B4B4E,0xA078D0,new Item.Properties()));
    public static final DeferredHolder<Item,DeferredSpawnEggItem> EGG=ITEMS.register("scorpion_spawn_egg",()->new DeferredSpawnEggItem(SCORPION,0x76502A,0x69BC32,new Item.Properties()));
    public static final DeferredHolder<SoundEvent,SoundEvent> AMBIENT=sound("ambient"),HURT=sound("hurt"),DEATH=sound("death"),STEP=sound("step"),STING=sound("sting");
    public static final DeferredHolder<SoundEvent,SoundEvent> CRYSTAPID_AMBIENT=crystalSound("ambient"),CRYSTAPID_HURT=crystalSound("hurt"),CRYSTAPID_DEATH=crystalSound("death"),CRYSTAPID_STEP=crystalSound("step"),CRYSTAPID_STING=crystalSound("sting");
    private static DeferredHolder<SoundEvent,SoundEvent> crystalSound(String name) {return SOUNDS.register("entity.crystapid."+name,()->SoundEvent.createVariableRangeEvent(id("entity.crystapid."+name)));}
    private static DeferredHolder<SoundEvent,SoundEvent> sound(String name) {return SOUNDS.register("entity.scorpion."+name,()->SoundEvent.createVariableRangeEvent(id("entity.scorpion."+name)));}
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);ITEMS.register(bus);SOUNDS.register(bus);
        bus.addListener(ScorpionModule::attributes);bus.addListener(ScorpionModule::placements);bus.addListener(ScorpionModule::creative);
    }
    private static void attributes(EntityAttributeCreationEvent e) {e.put(SCORPION.get(),Scorpion.createAttributes().build());e.put(CRYSTAPID.get(),Scorpion.createAttributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent e) {e.register(CRYSTAPID.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScorpionModule::checkCrystapidSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);e.register(SCORPION.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScorpionModule::checkNormalSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    public static boolean checkNormalSpawnRules(EntityType<? extends Scorpion> type,net.minecraft.world.level.ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random) {
        if(level.getBiome(pos).is(net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS)) {
            // Daylight is allowed here; retain the native block-light limit for spawnproofing.
            return Monster.checkAnyLightMonsterSpawnRules(type,level,reason,pos,random)
                    &&(MobSpawnType.ignoresLightRequirements(reason)||level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,pos)<=level.dimensionType().monsterSpawnBlockLightLimit());
        }
        return Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
    }
    private static boolean amethyst(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.AMETHYST_BLOCK)||state.is(net.minecraft.world.level.block.Blocks.BUDDING_AMETHYST);
    }
    public static boolean checkCrystapidSpawnRules(EntityType<? extends Scorpion> type,net.minecraft.world.level.ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random) {
        if(!Monster.checkAnyLightMonsterSpawnRules(type,level,reason,pos,random)
                ||level.canSeeSky(pos)||pos.getY()>=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())-4)return false;
        if(amethyst(level.getBlockState(pos.below())))return true;
        // Geodes are generated features, not tracked structures: recognize their inner shell.
        int crystalSides=0;
        for(var direction:new net.minecraft.core.Direction[]{net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST,net.minecraft.core.Direction.UP}) {
            for(int step=1;step<=6;step++) {
                var sample=pos.relative(direction,step);
                if(!level.getLevel().hasChunkAt(sample))break;
                var state=level.getBlockState(sample);
                if(amethyst(state)){crystalSides++;break;}
                if(state.isCollisionShapeFullBlock(level,sample))break;
            }
        }
        return crystalSides>=2&&net.minecraft.core.BlockPos.findClosestMatch(pos,6,4,p->level.getLevel().hasChunkAt(p)&&level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.CALCITE)).isPresent();
    }
    private static void creative(BuildCreativeModeTabContentsEvent e) {if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)){e.accept(EGG.get());e.accept(CRYSTAPID_EGG.get());}}
    public static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,path);}
    private ScorpionModule() {}
}
