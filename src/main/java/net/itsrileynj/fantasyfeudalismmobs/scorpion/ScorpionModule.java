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
    public static final DeferredHolder<EntityType<?>,EntityType<Scorpion>> LAVA_SCORPION=ENTITIES.register("lava_scorpion",()->EntityType.Builder.of(Scorpion::new,MobCategory.MONSTER)
            .sized(.35F,.225F).eyeHeight(.105F).clientTrackingRange(8).build(id("lava_scorpion").toString()));
    public static final DeferredHolder<Item,DeferredSpawnEggItem> LAVA_EGG=ITEMS.register("lava_scorpion_spawn_egg",()->new DeferredSpawnEggItem(LAVA_SCORPION,0x252525,0xD77B19,new Item.Properties()));
    public static final DeferredHolder<Item,DeferredSpawnEggItem> EGG=ITEMS.register("scorpion_spawn_egg",()->new DeferredSpawnEggItem(SCORPION,0x76502A,0x69BC32,new Item.Properties()));
    public static final DeferredHolder<SoundEvent,SoundEvent> AMBIENT=sound("ambient"),HURT=sound("hurt"),DEATH=sound("death"),STEP=sound("step"),STING=sound("sting");
    private static DeferredHolder<SoundEvent,SoundEvent> sound(String name) {return SOUNDS.register("entity.scorpion."+name,()->SoundEvent.createVariableRangeEvent(id("entity.scorpion."+name)));}
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);ITEMS.register(bus);SOUNDS.register(bus);
        bus.addListener(ScorpionModule::attributes);bus.addListener(ScorpionModule::placements);bus.addListener(ScorpionModule::creative);
    }
    private static void attributes(EntityAttributeCreationEvent e) {e.put(SCORPION.get(),Scorpion.createAttributes().build());e.put(LAVA_SCORPION.get(),Scorpion.createAttributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent e) {e.register(LAVA_SCORPION.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,ScorpionModule::checkLavaSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);e.register(SCORPION.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    public static boolean checkLavaSpawnRules(EntityType<? extends Scorpion> type,net.minecraft.world.level.ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random) {
        if(!Monster.checkAnyLightMonsterSpawnRules(type,level,reason,pos,random)
                ||level.canSeeSky(pos)||pos.getY()>=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())-4)return false;
        // Search only during spawn attempts; never load neighboring chunks to find lava.
        return net.minecraft.core.BlockPos.findClosestMatch(pos,6,3,p->level.getLevel().hasChunkAt(p)&&level.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA)).isPresent();
    }
    private static void creative(BuildCreativeModeTabContentsEvent e) {if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)){e.accept(EGG.get());e.accept(LAVA_EGG.get());}}
    public static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,path);}
    private ScorpionModule() {}
}
