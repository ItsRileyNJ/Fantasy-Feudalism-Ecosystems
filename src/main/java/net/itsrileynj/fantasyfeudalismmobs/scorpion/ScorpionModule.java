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
    public static final DeferredHolder<Item,DeferredSpawnEggItem> EGG=ITEMS.register("scorpion_spawn_egg",()->new DeferredSpawnEggItem(SCORPION,0x76502A,0x69BC32,new Item.Properties()));
    public static final DeferredHolder<SoundEvent,SoundEvent> AMBIENT=sound("ambient"),HURT=sound("hurt"),DEATH=sound("death"),STEP=sound("step"),STING=sound("sting");
    private static DeferredHolder<SoundEvent,SoundEvent> sound(String name) {return SOUNDS.register("entity.scorpion."+name,()->SoundEvent.createVariableRangeEvent(id("entity.scorpion."+name)));}
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);ITEMS.register(bus);SOUNDS.register(bus);
        bus.addListener(ScorpionModule::attributes);bus.addListener(ScorpionModule::placements);bus.addListener(ScorpionModule::creative);
    }
    private static void attributes(EntityAttributeCreationEvent e) {e.put(SCORPION.get(),Scorpion.createAttributes().build());}
    private static void placements(RegisterSpawnPlacementsEvent e) {e.register(SCORPION.get(),SpawnPlacementTypes.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,RegisterSpawnPlacementsEvent.Operation.REPLACE);}
    private static void creative(BuildCreativeModeTabContentsEvent e) {if(e.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))e.accept(EGG.get());}
    public static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,path);}
    private ScorpionModule() {}
}
