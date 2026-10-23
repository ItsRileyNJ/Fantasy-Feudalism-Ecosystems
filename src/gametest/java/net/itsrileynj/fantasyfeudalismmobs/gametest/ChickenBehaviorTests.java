package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChickenBehaviorTests {
    private static void floor(GameTestHelper h) {
        for (int x=0;x<18;x++) for(int z=0;z<18;z++) h.setBlock(x,1,z,Blocks.STONE);
    }
    private static AvoidEntityGoal<?> fleeing(Chicken chicken) {
        return chicken.goalSelector.getAvailableGoals().stream().map(g->g.getGoal())
                .filter(g->g instanceof AvoidEntityGoal<?>).map(g->(AvoidEntityGoal<?>)g)
                .findFirst().orElseThrow();
    }

    @GameTest(template="test_empty")
    public static void adultsAndChicksFleeOnlyNearbySurvivalPlayers(GameTestHelper h) {
        floor(h);
        var player=h.makeMockPlayer(GameType.SURVIVAL);h.getLevel().addFreshEntity(player);
        try {
            for(boolean baby:new boolean[]{false,true}) {
                var chicken=h.spawn(EntityType.CHICKEN,8,2,8);chicken.setOnGround(true);chicken.setBaby(baby);
                var goal=fleeing(chicken);
                player.moveTo(chicken.position().add(3.1,0,0));
                for(int i=0;i<30;i++) h.assertFalse(goal.canUse(),"Players outside three blocks do not trigger fleeing");
                player.moveTo(chicken.position().add(2.9,0,0));
                boolean found=false;
                for(int i=0;i<100&&!found;i++) found=goal.canUse();
                h.assertTrue(found,"Adults and chicks find an escape route from a player within three blocks");
                goal.start();
                h.assertFalse(chicken.getNavigation().isDone(),"Fleeing starts a server navigation path");
                var end=chicken.getNavigation().getPath().getEndNode();
                h.assertTrue(player.distanceToSqr(end.x,end.y,end.z)>player.distanceToSqr(chicken),"The escape route leads away from the player");
                chicken.discard();
            }
        } finally {player.discard();}
        h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=100)
    public static void normalAiFleesEvenWhenPlayerHoldsSeeds(GameTestHelper h) {
        floor(h);
        var chicken=h.spawn(EntityType.CHICKEN,8,2,8);chicken.setOnGround(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS));
        player.moveTo(chicken.position().add(2.5,0,0));h.getLevel().addFreshEntity(player);
        h.succeedWhen(()->{
            h.assertTrue(chicken.distanceToSqr(player)>25,"Normal AI must flee instead of following held seeds; chicken="
                    +h.relativeVec(chicken.position())+", player="+h.relativeVec(player.position())
                    +", path="+chicken.getNavigation().getPath()+", target="+chicken.getTarget());
            player.discard();
        });
    }

    @GameTest(template="test_empty")
    public static void fleeingGroundVelocityIsNinetyFivePercentOfPlayerSprint(GameTestHelper h) {
        floor(h);
        var chicken=h.spawn(EntityType.CHICKEN,8,2,8);chicken.setOnGround(true);
        var player=h.makeMockPlayer(GameType.SURVIVAL);h.getLevel().addFreshEntity(player);
        try {
            player.moveTo(chicken.position().add(2,0,0));
            var goal=fleeing(chicken);boolean found=false;
            for(int i=0;i<100&&!found;i++) found=goal.canUse();
            h.assertTrue(found,"An escape route is available");goal.start();
            chicken.setOnGround(true);
            chicken.getNavigation().tick();
            chicken.getMoveControl().tick();chicken.setYRot(0);player.setYRot(0);player.setSprinting(true);
            chicken.setDeltaMovement(Vec3.ZERO);player.setDeltaMovement(Vec3.ZERO);
            Vec3 origin=chicken.position();double chickenStep=0,playerStep=0;
            // Exercise vanilla ground acceleration and friction, including Mob's
            // speed-scaled input. Reset positions only to keep both on the test floor.
            for(int i=0;i<60;i++) {
                chicken.setPos(origin);player.setPos(origin.add(2,0,0));
                chicken.setOnGround(true);player.setOnGround(true);
                chicken.travel(new Vec3(0,0,chicken.getSpeed()*0.98F));
                player.travel(new Vec3(0,0,0.98F));
                chickenStep=chicken.getZ()-origin.z;playerStep=player.getZ()-origin.z;
            }
            double ratio=chickenStep/playerStep;
            h.assertTrue(Math.abs(ratio-0.95)<0.002,"Measured chicken/player ground sprint ratio must be 0.95, got "+ratio);
        } finally {player.discard();chicken.discard();}
        h.succeed();
    }
}
