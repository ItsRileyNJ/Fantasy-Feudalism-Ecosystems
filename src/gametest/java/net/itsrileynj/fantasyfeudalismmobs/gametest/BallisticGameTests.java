package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public class BallisticGameTests {
    private static void hit(GameTestHelper h,double distance,boolean crossbow) {
        var shooter=h.spawn(EntityType.SKELETON,3,2,8);shooter.setNoAi(true);shooter.setNoGravity(true);
        var target=h.spawn(EntityType.PILLAGER,8,2,8);target.setNoAi(true);target.setNoGravity(true);
        target.moveTo(shooter.position().add(distance,0,0));shooter.setTarget(target);
        var weapon=new ItemStack(crossbow?Items.CROSSBOW:Items.BOW);shooter.setItemSlot(EquipmentSlot.MAINHAND,weapon);
        Arrow arrow=new Arrow(h.getLevel(),shooter,new ItemStack(Items.ARROW),weapon);
        double dx=target.getX()-arrow.getX(),dz=target.getZ()-arrow.getZ(),horizontal=Math.hypot(dx,dz);
        // Exact vanilla nominal aim, with zero spread so this tests trajectory rather than random misses.
        arrow.shoot(dx,target.getY(1.0/3)-arrow.getY()+horizontal*.2,dz,crossbow?3.15F:1.6F,0);
        double speed=arrow.getDeltaMovement().length();h.getLevel().addFreshEntity(arrow);
        h.assertTrue(Math.abs(arrow.getDeltaMovement().length()-speed*1.4)<.00001,"Speed remains exactly 40% faster");
        float health=target.getHealth();var path=new ArrayList<Vec3>();h.onEachTick(()->{if(path.size()<30)path.add(h.relativeVec(arrow.position()));});
        h.succeedWhen(()->h.assertTrue(target.getHealth()<health,"Faster "+(crossbow?"crossbow":"bow")+" arrow must hit at "+distance+" blocks; trajectory="+path));
    }
    @GameTest(template="test_empty",batch="ffm_ballistics",timeoutTicks=40) public static void fasterBowHitsAtTenBlocks(GameTestHelper h){hit(h,10,false);}
    @GameTest(template="test_empty",batch="ffm_ballistics",timeoutTicks=40) public static void fasterBowHitsAtTwentyBlocks(GameTestHelper h){hit(h,20,false);}
    @GameTest(template="test_empty",batch="ffm_ballistics",timeoutTicks=40) public static void fasterBowHitsAtThirtyFourBlocks(GameTestHelper h){hit(h,34,false);}
    @GameTest(template="test_empty",batch="ffm_ballistics",timeoutTicks=40) public static void fasterCrossbowHitsAtTwentyBlocks(GameTestHelper h){hit(h,20,true);}
}
