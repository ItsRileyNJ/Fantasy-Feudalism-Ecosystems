package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BabyCatPlayTests {
    private static BlockPos village(GameTestHelper h) {
        for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);
        h.setBlock(8,2,4,Blocks.WHITE_BED);
        h.setBlock(new BlockPos(8,2,3),Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD));
        var pos=h.absolutePos(new BlockPos(8,2,3));var manager=h.getLevel().getPoiManager();
        manager.add(pos,h.getLevel().registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE).getHolderOrThrow(PoiTypes.HOME));
        manager.take(type->type.is(PoiTypes.HOME),(type,at)->at.equals(pos),pos,1);
        h.assertTrue(h.getLevel().isVillage(pos),"Fixture registers an occupied village bed");return pos;
    }
    private static Villager child(GameTestHelper h) {
        var child=h.spawn(EntityType.VILLAGER,5,2,8);child.setBaby(true);child.setNoAi(true);
        child.getBrain().setActiveActivityIfPossible(Activity.PLAY);child.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);return child;
    }
    private static Cat cat(GameTestHelper h){var cat=h.spawn(EntityType.CAT,10,2,8);cat.setNoAi(true);return cat;}
    private static void winningRoll(Villager child){long seed=0;while(RandomSource.create(seed).nextInt(12)!=0)seed++;child.getRandom().setSeed(seed);}

    @GameTest(template="test_empty")
    public static void chaseLastsEightSecondsAndDoesNotImmediatelyRepeat(GameTestHelper h){
        var bed=village(h);var child=child(h);var cat=cat(h);var play=new BabyCatPlay();winningRoll(child);
        long now=h.getLevel().getGameTime();
        h.assertTrue(play.tryStart(h.getLevel(),child,now),"An eligible rare roll starts harmless cat play");
        h.assertTrue(BabyCatPlay.chasing(child,cat),"Only the selected untamed cat is marked as chased");
        h.assertTrue(child.getTarget()==null&&cat.getTarget()==null,"Playing creates no combat target");
        play.tickOrStop(h.getLevel(),child,now+159);
        h.assertTrue(play.getStatus()==net.minecraft.world.entity.ai.behavior.Behavior.Status.RUNNING,"Play continues through the eighth second");
        play.tickOrStop(h.getLevel(),child,now+160);
        h.assertTrue(((VillagerPlayState)child).ffm$playCat()==null,"Play state clears after exactly 160 ticks");
        h.assertFalse(child.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET),"The play walk memory is released");
        winningRoll(child);h.assertFalse(play.tryStart(h.getLevel(),child,now+161),"A successful play cannot immediately repeat");
        h.getLevel().getPoiManager().remove(bed);h.succeed();
    }

    @GameTest(template="test_empty")
    public static void adultVillagersTamedCatsAndPanicAreExcluded(GameTestHelper h){
        var bed=village(h);var child=child(h);var cat=cat(h);
        cat.setTame(true,true);winningRoll(child);
        h.assertFalse(new BabyCatPlay().tryStart(h.getLevel(),child,h.getLevel().getGameTime()),"Owned cats are never selected");
        cat.setTame(false,false);child.setAge(0);child.getBrain().setActiveActivityIfPossible(Activity.PLAY);winningRoll(child);
        h.assertFalse(new BabyCatPlay().tryStart(h.getLevel(),child,h.getLevel().getGameTime()),"Adults do not play-chase cats");
        child.setBaby(true);child.getBrain().setActiveActivityIfPossible(Activity.PLAY);
        child.getBrain().setMemory(MemoryModuleType.NEAREST_HOSTILE,h.spawn(EntityType.ZOMBIE,12,2,8));winningRoll(child);
        h.assertFalse(new BabyCatPlay().tryStart(h.getLevel(),child,h.getLevel().getGameTime()),"Danger takes precedence over play");
        h.getLevel().getPoiManager().remove(bed);h.succeed();
    }

    @GameTest(template="test_empty")
    public static void tamingCatOrEndingPlayReleasesFleeingAndLeavesHealthUntouched(GameTestHelper h){
        var bed=village(h);var child=child(h);var cat=cat(h);var play=new BabyCatPlay();winningRoll(child);
        long now=h.getLevel().getGameTime();float childHealth=child.getHealth(),catHealth=cat.getHealth();
        h.assertTrue(play.tryStart(h.getLevel(),child,now),"The child begins a valid play chase");
        cat.setTame(true,true);play.tickOrStop(h.getLevel(),child,now+1);
        h.assertTrue(((VillagerPlayState)child).ffm$playCat()==null,"Taming the cat immediately ends chasing");
        h.assertTrue(child.getHealth()==childHealth&&cat.getHealth()==catHealth,"Playing inflicts no damage on either participant");
        h.getLevel().getPoiManager().remove(bed);h.succeed();
    }
}
