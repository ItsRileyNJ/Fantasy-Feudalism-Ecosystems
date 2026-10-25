package net.itsrileynj.fantasyfeudalismmobs.gametest;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MouseActivityTests {
    private static void floor(GameTestHelper h){for(int x=0;x<30;x++)for(int z=0;z<24;z++)h.setBlock(x,1,z,Blocks.STONE);}
    private static FieldMouse fromEgg(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.CREATIVE);var egg=new ItemStack(FantasyFeudalismMobs.FIELD_MOUSE_SPAWN_EGG.get());
        p.setItemInHand(InteractionHand.MAIN_HAND,egg);var pos=h.absolutePos(new BlockPos(8,1,8));
        h.assertTrue(egg.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))).consumesAction(),"Native spawn egg is used");
        var mice=h.getLevel().getEntitiesOfClass(FieldMouse.class,new AABB(pos).inflate(3));
        h.assertTrue(mice.size()==1,"Egg creates exactly one mouse");return mice.getFirst();
    }
    @GameTest(template="test_empty",batch="mouse_egg_cat",timeoutTicks=420)
    public static void catReallyChasesAndKillsAnActiveSpawnEggMouse(GameTestHelper h){
        floor(h);var mouse=fromEgg(h);var cat=h.spawn(EntityType.CAT,4,2,8);
        h.succeedWhen(()->h.assertFalse(mouse.isAlive(),"Cat must catch an egg-spawned mouse: cat="+cat.position()+" mouse="+mouse.position()+" target="+cat.getTarget()));
    }
    @GameTest(template="test_empty",batch="mouse_egg_villager",timeoutTicks=200)
    public static void spawnEggMouseReallyRunsFromNearbyVillager(GameTestHelper h){
        floor(h);var mouse=fromEgg(h);mouse.getRandom().setSeed(4);var villager=h.spawn(EntityType.VILLAGER,10,2,8);villager.setNoAi(true);
        h.succeedWhen(()->h.assertTrue(mouse.distanceToSqr(villager)>25,"Egg mouse must flee its villager: "+mouse.position()+" distance="+mouse.distanceTo(villager)));
    }
    @GameTest(template="test_empty",batch="mouse_idle_crop",timeoutTicks=540)
    public static void fedMouseWandersAndTurnsItsHeadNearTheCropInsteadOfFreezing(GameTestHelper h){
        floor(h);h.setBlock(8,1,8,Blocks.FARMLAND);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var home=h.absolutePos(new BlockPos(8,2,8));var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setCropHome(home);mouse.getRandom().setSeed(27);
        var tag=mouse.saveWithoutId(new CompoundTag());tag.putLong("NextMeal",h.getLevel().getGameTime()+1800);mouse.load(tag);
        Vec3 initial=mouse.position();float firstHead=mouse.getYHeadRot();double[] maxMove={0},headTurn={0};
        h.onEachTick(()->{
            maxMove[0]=Math.max(maxMove[0],mouse.position().subtract(initial).horizontalDistance());
            headTurn[0]=Math.max(headTurn[0],Math.abs(net.minecraft.util.Mth.wrapDegrees(mouse.getYHeadRot()-firstHead)));
        });
        h.succeedWhen(()->{
            h.assertTrue(maxMove[0]>.5&&headTurn[0]>10,"Idle crop activity: moved="+maxMove[0]+" head="+headTurn[0]+" path="+mouse.getNavigation().getPath());
            h.assertTrue(mouse.distanceToSqr(Vec3.atCenterOf(home))<36,"Idle wandering remains near the remembered crop");
            h.assertTrue(h.getLevel().getBlockState(home).getValue(CropBlock.AGE)==7,"Idle activity cannot bypass the feeding cooldown");
        });
    }
    @GameTest(template="test_empty",batch="mouse_timer_migration")
    public static void oldTwoMinuteMealDeadlineUpgradesToNinetySecondsWithoutLosingElapsedTime(GameTestHelper h){
        var mouse=FantasyFeudalismMobs.FIELD_MOUSE.get().create(h.getLevel());var tag=mouse.saveWithoutId(new CompoundTag());
        long deadline=h.getLevel().getGameTime()+2400;tag.putLong("NextMeal",deadline);tag.remove("MealInterval");mouse.load(tag);
        h.assertTrue(mouse.nextMeal()==deadline-600,"Old feeding cycle is shortened by thirty seconds");
        var saved=mouse.saveWithoutId(new CompoundTag());mouse.load(saved);
        h.assertTrue(mouse.nextMeal()==deadline-600&&saved.getInt("MealInterval")==1800,"Reloading new data cannot shorten the cycle repeatedly");h.succeed();
    }
}
