package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiderMovementTests {
    private static void floor(GameTestHelper h) { for(int x=0;x<24;x++)for(int z=0;z<24;z++)h.setBlock(x,1,z,Blocks.GRASS_BLOCK); }
    private static Spider spider(GameTestHelper h,int variant,int x,int z) {
        Spider b=variant<2?h.spawn(EntityType.SPIDER,x,2,z):h.spawn(EntityType.CAVE_SPIDER,x,2,z);
        if(variant%2==1)SpiderCompanion.tame(b,UUID.randomUUID());
        SpiderCompanion.setSitting(b,false);b.setNoAi(true);b.setOnGround(true);return b;
    }
    @GameTest(template="test_empty",batch="spider_normal_jump",timeoutTicks=70)
    public static void wanderingAndFollowingSpidersUseNormalJumpPhysics(GameTestHelper h) {
        floor(h);var spiders=new ArrayList<Spider>();var starts=new HashMap<Spider,Double>();var peaks=new HashMap<Spider,Double>();
        for(int i=0;i<4;i++) {
            var b=spider(h,i,5+(i%2)*7,5+(i/2)*7);b.goalSelector.removeAllGoals(g->true);b.targetSelector.removeAllGoals(g->true);b.setNoAi(false);spiders.add(b);
        }
        h.runAfterDelay(2,()->{for(var b:spiders) { starts.put(b,b.getY());peaks.put(b,b.getY());b.jumpFromGround(); }});
        h.onEachTick(()->{for(var b:spiders)if(peaks.containsKey(b))peaks.put(b,Math.max(peaks.get(b),b.getY()));});
        h.runAfterDelay(35,()->{
            for(var b:spiders) {
                double height=peaks.get(b)-starts.get(b);
                h.assertTrue(height>1.15&&height<1.4,"Wild/pet spiders and cave spiders use a normal approximately 1.25-block hop without an attack target: "+height);
            }
            h.succeed();
        });
    }
    @GameTest(template="test_empty",batch="spider_jump_transition")
    public static void combatJumpSwitchesImmediatelyAndPreservesJumpBoost(GameTestHelper h) {
        floor(h);
        for(int i=0;i<4;i++) {
            var b=spider(h,i,5+i*3,5);b.addEffect(new MobEffectInstance(MobEffects.JUMP,200,1));
            b.jumpFromGround();h.assertTrue(Math.abs(b.getDeltaMovement().y-.62)<.0001,"Ordinary movement preserves native Jump Boost");
            var target=h.spawn(EntityType.ZOMBIE,5+i*3,2,10);target.setNoAi(true);b.setTarget(target);
            b.jumpFromGround();h.assertTrue(Math.abs(b.getDeltaMovement().y-(SpiderGoals.JUMP_POWER+.2))<.0001,"A valid attack target enables the high jump without losing Jump Boost");
            target.setHealth(0);b.jumpFromGround();h.assertTrue(Math.abs(b.getDeltaMovement().y-.62)<.0001,"A dead target immediately restores the normal hop");
            b.setTarget(null);b.removeEffect(MobEffects.JUMP);b.jumpFromGround();h.assertTrue(Math.abs(b.getDeltaMovement().y-.42)<.0001,"Clearing combat and potion effects restores vanilla launch strength");b.discard();target.discard();
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="spider_jump_paths")
    public static void onlyAttackingSpidersPlanThreeBlockJumpEdges(GameTestHelper h) {
        floor(h);for(int x=8;x<=12;x++)for(int y=2;y<=4;y++)for(int z=2;z<=12;z++)h.setBlock(x,y,z,Blocks.STONE);
        var goal=h.absolutePos(new BlockPos(10,5,6));
        for(int i=0;i<4;i++) {
            var b=spider(h,i,5,6);var idle=b.getNavigation().createPath(goal,0);
            h.assertTrue(idle==null||!idle.canReach(),"Following/wandering paths cannot promise an impossible three-block jump");
            var target=h.spawn(EntityType.ZOMBIE,10,5,6);target.setNoAi(true);b.setTarget(target);
            var combat=b.getNavigation().createPath(goal,0);
            h.assertTrue(combat!=null&&combat.canReach(),"Combat retains three-block jump planning for both wild and pet variants");
            b.setTarget(null);b.getNavigation().stop();var after=b.getNavigation().createPath(goal,0);
            h.assertTrue(after==null||!after.canReach(),"New paths return to ordinary height as soon as combat ends");b.discard();target.discard();
        }
        h.succeed();
    }
}
