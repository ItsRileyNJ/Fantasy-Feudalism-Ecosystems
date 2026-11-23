package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.ScorpionModule;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArthropodWaterTests {
    private static Mob mob(GameTestHelper h,int variant,int x,int z) {
        Mob mob=variant<2?h.spawn(EntityType.SPIDER,x,2,z):variant<4?h.spawn(EntityType.CAVE_SPIDER,x,2,z)
                :h.spawn(variant==4?ScorpionModule.SCORPION.get():ScorpionModule.LAVA_SCORPION.get(),x,2,z);
        mob.setNoAi(true);mob.setOnGround(true);
        if(variant<4&&variant%2==1){SpiderCompanion.tame((Spider)mob,UUID.randomUUID());SpiderCompanion.setSitting((Spider)mob,false);}
        return mob;
    }
    @GameTest(template="test_root_worm",batch="arthropod_water_routes")
    public static void arthropodWaterAvoidanceRoutesAllVariantsAroundAPond(GameTestHelper h) {
        for(int x=0;x<24;x++)for(int z=0;z<24;z++)h.setBlock(x,1,z,Blocks.STONE);
        for(int x=9;x<=11;x++)for(int z=7;z<=15;z++)h.setBlock(x,2,z,Blocks.WATER);
        for(int variant=0;variant<6;variant++) {
            Mob mob=mob(h,variant,5,11);mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(40);
            var destination=h.absolutePos(new BlockPos(17,2,11));var path=mob.getNavigation().createPath(destination,0);
            h.assertTrue(path!=null&&path.canReach(),"Dry detour remains reachable, variant "+variant);
            for(int i=0;i<path.getNodeCount();i++) {
                Vec3 point=path.getEntityPosAtNode(mob,i);
                h.assertTrue(!h.getLevel().containsAnyLiquid(mob.getBoundingBox().move(point.subtract(mob.position()))),"Every route node keeps the body dry, variant "+variant);
            }
            h.assertTrue(mob.getPathfindingMalus(PathType.WATER)<0,"Dry routes forbid water nodes");mob.discard();
        }h.succeed();
    }
    @GameTest(template="test_root_worm",batch="arthropod_water_escape",timeoutTicks=280)
    public static void arthropodWaterAvoidanceLetsAllVariantsLeaveShallowWater(GameTestHelper h) {
        var mobs=new ArrayList<Mob>();
        for(int variant=0;variant<6;variant++) {
            int offset=variant*12;
            for(int x=offset;x<offset+12;x++)for(int z=0;z<12;z++){h.setBlock(x,1,z,Blocks.STONE);h.setBlock(x,2,z,variant<4?Blocks.STONECUTTER:Blocks.STONE);}
            for(int x=offset+3;x<=offset+7;x++)for(int z=3;z<=7;z++)h.setBlock(x,2,z,Blocks.WATER);
            Mob mob=mob(h,variant,offset+5,5);
            mob.goalSelector.removeAllGoals(g->!(g instanceof AvoidWaterGoal)&&!(g instanceof FloatGoal));
            mob.targetSelector.removeAllGoals(g->true);mobs.add(mob);
        }
        h.runAfterDelay(3,()->mobs.forEach(m->m.setNoAi(false)));
        h.runAfterDelay(240,()->{
            for(int i=0;i<mobs.size();i++)h.assertTrue(!mobs.get(i).isInWater()&&mobs.get(i).getPathfindingMalus(PathType.WATER)<0,"Leaves shallows and restores water avoidance, variant "+i+" pos="+mobs.get(i).position());
            h.succeed();
        });
    }
    @GameTest(template="test_root_worm",batch="arthropod_water_pursuit",timeoutTicks=100)
    public static void arthropodWaterAvoidanceStopsSpiderClimbingFallbackFromCrossingWater(GameTestHelper h) {
        for(int x=0;x<=26;x++)for(int z=0;z<=20;z++) {
            h.setBlock(x,1,z,Blocks.STONE);h.setBlock(x,2,z,x>=10&&x<=14?Blocks.WATER:Blocks.STONE);
            if(x==0||x==26||z==0||z==20)for(int y=2;y<=6;y++)h.setBlock(x,y,z,Blocks.STONE);
        }
        var spiders=new ArrayList<Mob>();boolean[] touched={false};
        for(int variant=0;variant<4;variant++) {
            Mob spider=mob(h,variant,5,3+variant*4);spider.setPos(spider.getX(),h.absolutePos(new BlockPos(0,3,0)).getY(),spider.getZ());
            spider.goalSelector.removeAllGoals(g->true);spider.targetSelector.removeAllGoals(g->true);spider.setNoAi(false);
            var target=h.spawn(EntityType.CREEPER,20,3,3+variant*4);target.setNoAi(true);spider.getNavigation().moveTo(target,1);spiders.add(spider);
        }
        h.onEachTick(()->{for(var spider:spiders)touched[0]|=spider.isInWater();});
        h.runAfterDelay(60,()->{h.assertTrue(!touched[0],"Spider climbing fallback never walks into the separating water");h.succeed();});
    }
    @GameTest(template="test_empty",batch="arthropod_water_leaps")
    public static void arthropodWaterAvoidanceBlocksSpiderPouncesAcrossWater(GameTestHelper h) {
        for(int x=0;x<12;x++)for(int z=0;z<12;z++)h.setBlock(x,1,z,Blocks.STONE);
        for(int z=3;z<=7;z++)h.setBlock(6,2,z,Blocks.WATER);
        var target=h.spawn(EntityType.CREEPER,8,2,5);target.setNoAi(true);
        for(int variant=0;variant<4;variant++) {
            Spider spider=(Spider)mob(h,variant,5,5);spider.setTarget(target);
            var leap=spider.goalSelector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(g->g instanceof LeapAtTargetGoal).findFirst().orElseThrow();
            for(int attempt=0;attempt<100;attempt++)h.assertTrue(!leap.canUse(),"Pounces must not bypass a water obstacle, variant "+variant);
            h.assertTrue(!spider.getNavigation().canFloat(),"Existing non-swimming rule is retained");spider.discard();
        }h.succeed();
    }
}
