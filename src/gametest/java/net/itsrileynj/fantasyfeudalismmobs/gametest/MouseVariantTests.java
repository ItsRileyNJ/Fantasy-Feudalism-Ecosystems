package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MouseVariantTests {
    @GameTest(template="test_empty",batch="mouse_variants")
    public static void bothCoatsSpawnFromNaturalEggAndCommandPathsAndSurviveSaving(GameTestHelper h){
        for(var reason:new MobSpawnType[]{MobSpawnType.NATURAL,MobSpawnType.SPAWN_EGG,MobSpawnType.COMMAND}){
            int white=0,amber=0;
            for(int i=0;i<40;i++){
                var mouse=FantasyFeudalismMobs.FIELD_MOUSE.get().create(h.getLevel());
                mouse.getRandom().setSeed(i*173L+19);
                mouse.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(h.absolutePos(new net.minecraft.core.BlockPos(8,2,8))),reason,null);
                if(mouse.getVariant()==FieldMouse.AMBER)amber++;else white++;
                var loaded=FantasyFeudalismMobs.FIELD_MOUSE.get().create(h.getLevel());
                loaded.load(mouse.saveWithoutId(new CompoundTag()));
                h.assertTrue(loaded.getVariant()==mouse.getVariant(),"Saved coat remains stable: "+reason);
                var clientCopy=FantasyFeudalismMobs.FIELD_MOUSE.get().create(h.getLevel());
                var metadata=mouse.getEntityData().getNonDefaultValues();
                if(metadata!=null)clientCopy.getEntityData().assignValues(metadata);
                h.assertTrue(clientCopy.getVariant()==mouse.getVariant(),"Spawn metadata transmits coat: "+reason);
            }
            h.assertTrue(white>0&&amber>0,"Both coats occur in ordinary spawn path: "+reason+" white="+white+" amber="+amber);
        }h.succeed();
    }
    @GameTest(template="test_empty",batch="mouse_legacy_variant")
    public static void legacyMiceRemainWhiteAndInvalidSavedCoatsFallBackSafely(GameTestHelper h){
        var mouse=FantasyFeudalismMobs.FIELD_MOUSE.get().create(h.getLevel());mouse.setVariant(FieldMouse.AMBER);
        var saved=mouse.saveWithoutId(new CompoundTag());saved.remove("Variant");mouse.load(saved);
        h.assertTrue(mouse.getVariant()==FieldMouse.WHITE,"Existing mice without a variant remain white");
        for(int invalid:new int[]{-1,2,Integer.MAX_VALUE}){
            saved.putInt("Variant",invalid);mouse.load(saved);
            h.assertTrue(mouse.getVariant()==FieldMouse.WHITE,"Unknown saved coat cannot select an invalid texture");
        }h.succeed();
    }
    @GameTest(template="test_empty",batch="mouse_player_modes")
    public static void miceIgnoreCreativeAndSpectatorButStillFearSurvivalAdventureAndAnimals(GameTestHelper h){
        var p=AdvancementTests.player(h);
        try{
            for(var mode:GameType.values()){
                p.setGameMode(mode);
                h.assertTrue(FieldMouse.threat(p)==(mode==GameType.SURVIVAL||mode==GameType.ADVENTURE),"Threat policy for "+mode);
            }
            var cat=h.spawn(EntityType.CAT,8,2,8);var villager=h.spawn(EntityType.VILLAGER,9,2,8);
            h.assertTrue(FieldMouse.threat(cat)&&FieldMouse.threat(villager),"Cats and villagers remain threats");h.succeed();
        }finally{AdvancementTests.remove(p);}
    }
    @GameTest(template="test_empty",batch="mouse_mode_switch",timeoutTicks=120)
    public static void activeEscapeStopsWhenItsPlayerChangesToCreativeOrSpectator(GameTestHelper h){
        for(int x=0;x<24;x++)for(int z=0;z<24;z++)h.setBlock(x,1,z,Blocks.STONE);
        var p=AdvancementTests.player(h);var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setNoAi(true);mouse.setOnGround(true);
        p.moveTo(mouse.position().add(2,0,0));mouse.getRandom().setSeed(4);
        var flee=mouse.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g.getClass().getSimpleName().equals("FleeGoal")).findFirst().orElseThrow();
        h.succeedWhen(()->{
            h.assertTrue(flee.canUse(),"Nearby survival player starts a real escape path: player="+p.position()+" mouse="+mouse.position()+" ground="+mouse.onGround()+" threats="+h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,mouse.getBoundingBox().inflate(3),FieldMouse::threat)+" mode="+p.gameMode.getGameModeForPlayer());
            try{flee.start();
            h.assertTrue(flee.canContinueToUse(),"Survival escape is active");
            p.setGameMode(GameType.CREATIVE);h.assertFalse(flee.canContinueToUse(),"Changing to Creative ends the active escape");
            p.setGameMode(GameType.ADVENTURE);h.assertTrue(flee.canContinueToUse(),"Adventure remains a threat");
            p.setGameMode(GameType.SPECTATOR);h.assertFalse(flee.canContinueToUse(),"Changing to Spectator ends the active escape");flee.stop();
            h.assertTrue(mouse.getNavigation().isDone(),"Stopping escape clears its path");h.succeed();
            }finally{AdvancementTests.remove(p);}
        });
    }
}
