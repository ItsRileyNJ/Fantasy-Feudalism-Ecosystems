package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(RootWormModule.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormBurrowTests {
    private static RootWorm fixture(GameTestHelper h,Block soil) {
        for(int x=2;x<=29;x++)for(int z=2;z<=29;z++) {
            h.setBlock(x,0,z,soil);h.setBlock(x,1,z,soil);
            for(int y=2;y<=4;y++)h.setBlock(x,y,z,Blocks.AIR);
        }
        var worm=h.spawn(RootWormModule.ROOT_WORM.get(),16,2,16);
        worm.goalSelector.removeAllGoals(g->true);worm.targetSelector.removeAllGoals(g->true);worm.setOnGround(true);return worm;
    }
    private static void frighten(GameTestHelper h,RootWorm worm) {worm.setHealth(8);worm.invulnerableTime=0;worm.hurt(h.getLevel().damageSources().generic(),1);}
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void rootWormBurrowRetreatsBelowGroundAndEmergesWithoutChangingBlocks(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);var from=worm.position();frighten(h,worm);
        h.assertTrue(worm.isBurrowing(),"Accepted damage below half health triggers retreat");
        h.runAfterDelay(25,()->{
            h.assertTrue(worm.isBurrowing()&&worm.getY()<from.y-.7,"The worm physically travels below the surface");
            h.assertTrue(worm.getHealth()==7&&!worm.isInWall()&&!worm.canBite(worm),"Burrowing prevents suffocation and interrupts attacks");
            h.assertTrue(!worm.getParts()[0].isPickable(),"Underground body cannot be hand-targeted");
        });
        h.runAfterDelay(65,()->{
            h.assertTrue(!worm.isBurrowing()&&Math.abs(worm.getY()-from.y)<.01,"The worm returns to the surface");
            double distance=worm.position().distanceTo(from);h.assertTrue(distance>=3.99&&distance<=6.01,"The escape is four to six blocks long");
            h.assertTrue(worm.getHealth()==7&&worm.getParts()[0].isPickable(),"Surfaced worm keeps health and normal targeting");
            for(int x=2;x<=29;x++)for(int z=2;z<=29;z++)for(int y=0;y<=1;y++)h.assertTrue(h.getBlockState(new BlockPos(x,y,z)).is(Blocks.DIRT),"Retreat does not destroy or replace terrain");
            h.succeed();
        });
    }
    @GameTest(template="test_root_worm")
    public static void rootWormBurrowRequiresBelowHalfAndSuccessfulDamage(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);worm.setHealth(9);worm.hurt(h.getLevel().damageSources().generic(),1);
        h.assertTrue(worm.getHealth()==8&&!worm.isBurrowing(),"Exactly half health does not trigger retreat");
        worm.setInvulnerable(true);worm.setHealth(7);worm.invulnerableTime=0;worm.hurt(h.getLevel().damageSources().generic(),1);
        h.assertTrue(!worm.isBurrowing(),"Rejected damage cannot trigger burrowing");
        worm.setInvulnerable(false);frighten(h,worm);h.assertTrue(worm.isBurrowing(),"A later accepted hit below half starts the retreat");h.succeed();
    }
    @GameTest(template="test_root_worm")
    public static void rootWormBurrowStaysWithinOneSoilLayerAboveStone(GameTestHelper h) {
        var worm=fixture(h,Blocks.STONE);frighten(h,worm);h.assertTrue(!worm.isBurrowing(),"Stone is not diggable");
        for(int x=2;x<=29;x++)for(int z=2;z<=29;z++)h.setBlock(x,1,z,Blocks.GRASS_BLOCK);
        frighten(h,worm);h.assertTrue(worm.isBurrowing(),"A shallow retreat can use a single soil layer without digging into the stone below");
        double surface=worm.getY();
        h.runAfterDelay(25,()->{
            h.assertTrue(worm.isBurrowing()&&worm.getY()<surface-.7,"The worm still hides beneath the soil");
            for(var part:worm.getParts())h.assertTrue(part.getBoundingBox().minY>surface-1,"Every part stays above the underlying stone");
            h.succeed();
        });
    }
    @GameTest(template="test_root_worm")
    public static void rootWormBurrowCannotCrossStoneWallsOrUseWetBlockedExits(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);
        for(int x=2;x<=29;x++)for(int z=2;z<=29;z++)if(x<=13||x>=19||z<=13||z>=19)h.setBlock(x,1,z,Blocks.STONE);
        frighten(h,worm);h.assertTrue(!worm.isBurrowing(),"No four-block route crosses a ring of stone");
        for(int x=2;x<=29;x++)for(int z=2;z<=29;z++){h.setBlock(x,1,z,Blocks.DIRT);h.setBlock(x,2,z,Blocks.WATER);}
        frighten(h,worm);h.assertTrue(!worm.isBurrowing(),"Water blocks burrowing and resurfacing");
        for(int x=2;x<=29;x++)for(int z=2;z<=29;z++)h.setBlock(x,2,z,Blocks.STONE);
        frighten(h,worm);h.assertTrue(!worm.isBurrowing(),"Occupied exit space blocks the retreat");h.succeed();
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void rootWormBurrowPrefersAwayFromAttackerAndHasCooldown(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);var from=worm.position();var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setPos(from.add(-2,0,0));
        worm.setHealth(8);worm.hurt(h.getLevel().damageSources().playerAttack(player),1);
        h.runAfterDelay(65,()->{
            h.assertTrue(worm.getX()>from.x+3.9,"Retreat moves away from the attacker");
            worm.invulnerableTime=0;worm.hurt(h.getLevel().damageSources().playerAttack(player),1);
            h.assertTrue(!worm.isBurrowing(),"Cooldown prevents a new dive on every hit");h.succeed();
        });
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void rootWormBurrowLoadingMidRetreatResurfacesAndKeepsCooldown(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);double y=worm.getY();frighten(h,worm);
        h.runAfterDelay(25,()->{
            var tag=new CompoundTag();worm.saveWithoutId(tag);var copy=RootWormModule.ROOT_WORM.get().create(h.getLevel());copy.load(tag);
            h.assertTrue(!copy.isBurrowing()&&Math.abs(copy.getY()-y)<.01,"Reloaded worm safely starts on the surface, never stuck underground");
            copy.setOnGround(true);copy.invulnerableTime=0;copy.hurt(h.getLevel().damageSources().generic(),1);
            h.assertTrue(!copy.isBurrowing(),"Reload preserves the remaining retreat cooldown");h.succeed();
        });
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void rootWormBurrowChangingTerrainAbortsBeforeEnteringStone(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);double y=worm.getY();frighten(h,worm);
        h.runAfterDelay(10,()->{
            for(int x=2;x<=29;x++)for(int z=2;z<=29;z++)if(x<=13||x>=19||z<=13||z>=19)h.setBlock(x,1,z,Blocks.STONE);
        });
        h.runAfterDelay(35,()->{
            h.assertTrue(!worm.isBurrowing()&&Math.abs(worm.getY()-y)<.01,"New stone causes an immediate safe resurfacing");
            h.assertTrue(worm.position().distanceTo(h.absoluteVec(new Vec3(16.5,2,16.5)))<2,"Retreat stops before crossing the new barrier");h.succeed();
        });
    }
    @GameTest(template="test_root_worm",timeoutTicks=130)
    public static void rootWormBurrowRetriesOnLandingAfterAnAirborneHit(GameTestHelper h) {
        var worm=fixture(h,Blocks.DIRT);worm.setPos(worm.getX(),worm.getY()+2,worm.getZ());worm.setOnGround(false);frighten(h,worm);
        h.assertTrue(!worm.isBurrowing(),"The worm cannot dig while airborne");
        boolean[] seen={false};h.onEachTick(()->{if(worm.isBurrowing())seen[0]=true;});
        h.runAfterDelay(90,()->{
            h.assertTrue(seen[0]&&!worm.isBurrowing(),"An airborne hit queues a retreat which starts after landing and finishes normally");
            h.assertTrue(worm.getHealth()==7,"Landing and the shallow escape do not inflict extra damage");h.succeed();
        });
    }

}
