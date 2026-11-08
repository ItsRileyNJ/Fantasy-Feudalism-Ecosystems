package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BirdFoliageTests {
    @GameTest(template="test_empty",batch="bird_foliage")
    public static void flightUsesPassableLeavesWithoutChangingStoneOrOtherMobs(GameTestHelper h) {
        boolean foliage=ModList.get().isLoaded("passablefoliage");
        h.setBlock(8,4,8,Blocks.OAK_LEAVES);h.setBlock(10,4,8,Blocks.STONE);
        var leaf=h.absolutePos(new BlockPos(8,4,8));
        for(var type:Birds.TYPES) {
            var b=h.spawn(type.get(),7,4,8);b.setNoAi(true);b.setFlying(true);
            boolean empty=h.getLevel().getBlockState(leaf).getCollisionShape(h.getLevel(),leaf,CollisionContext.of(b)).isEmpty();
            h.assertTrue(empty==foliage,"Flying bird leaf collision follows optional Passable Foliage presence");
            b.move(MoverType.SELF,new Vec3(1,0,0));
            h.assertTrue((b.getX()>leaf.getX()+.4)==foliage,"Actual flying movement passes through leaves only with Passable Foliage");
            b.moveTo(leaf.getX()+1.5,leaf.getY(),leaf.getZ()+.5);b.move(MoverType.SELF,new Vec3(1,0,0));
            h.assertTrue(b.getX()<leaf.getX()+2,"Stone still blocks every bird during flight");b.discard();
        }
        var pig=h.spawn(EntityType.PIG,7,4,8);pig.setNoAi(true);
        h.assertTrue(h.getLevel().getBlockState(leaf).getCollisionShape(h.getLevel(),leaf,CollisionContext.of(pig)).isEmpty()==foliage,"Other mobs retain the foliage mod's default pass-through behavior");
        pig.discard();h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_foliage")
    public static void allBirdsSettleOntoLeavesAndCanTakeOffThroughThemAgain(GameTestHelper h) {
        var leaf=h.absolutePos(new BlockPos(8,4,8));h.setBlock(8,4,8,Blocks.OAK_LEAVES);
        for(var type:Birds.TYPES) {
            var b=h.spawn(type.get(),8,6,8);b.setNoAi(false);b.setFlying(false);b.setDeltaMovement(Vec3.ZERO);
            for(int i=0;i<30;i++)b.travel(Vec3.ZERO);
            h.assertTrue(Math.abs(b.getY()-(leaf.getY()+1))<.001&&b.onGround(),"Gravity settles each species on the leaf top instead of dropping through: "+b.position());
            b.setFlying(true);b.move(MoverType.SELF,new Vec3(0,-1.5,0));
            h.assertTrue((b.getY()<leaf.getY()+.5)==ModList.get().isLoaded("passablefoliage"),"Taking flight immediately restores pass-through leaves when the foliage mod is installed");
            b.discard();
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_foliage")
    public static void perchingBirdsCanFindGroundPathsAcrossACanopy(GameTestHelper h) {
        for(int x=5;x<=12;x++)for(int z=5;z<=12;z++)h.setBlock(x,7,z,Blocks.OAK_LEAVES);
        h.setBlock(8,8,8,Blocks.OAK_LEAVES);
        h.assertFalse(BirdGoals.landingSpot(h.getLevel(),h.absolutePos(new BlockPos(8,8,8))),"The inside of passable foliage must not be mistaken for an exposed perch");
        h.setBlock(8,8,8,Blocks.AIR);
        var b=h.spawn(Birds.ROBIN.get(),8,8,8);b.setNoAi(true);b.setOnGround(true);
        var target=h.absolutePos(new BlockPos(10,8,8));
        h.assertTrue(b.getNavigation() instanceof GroundPathNavigation,"Perched birds use ground navigation");
        var path=b.getNavigation().createPath(target,0);
        h.assertTrue(path!=null&&path.canReach()&&path.getEndNode().y==target.getY(),"Ground paths remain on the canopy instead of searching for ground beneath passable leaves");
        h.succeed();
    }
}
