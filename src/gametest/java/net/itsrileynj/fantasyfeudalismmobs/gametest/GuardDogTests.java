package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuardDogTests {
    private static void floor(GameTestHelper h){for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);}
    private static <T extends Mob> T still(GameTestHelper h,EntityType<T> type,int x,int z){var m=h.spawn(type,x,2,z);m.setNoAi(true);return m;}
    private static Wolf dog(GameTestHelper h,int x,int z){var w=still(h,EntityType.WOLF,x,z);w.setTame(true,true);sit(w,true);w.setHealth(w.getMaxHealth());return w;}
    private static void sit(Wolf w,boolean value){w.setOrderedToSit(value);w.setInSittingPose(value);}

    @GameTest(template="test_empty")
    public static void protectionUsesPreyCenteredSphericalTwentyFourBlockRange(GameTestHelper h){
        floor(h);var prey=still(h,EntityType.SHEEP,8,8);var dog=dog(h,5,8);
        dog.moveTo(prey.position().add(24,0,0));
        h.assertTrue(GuardDogs.guard(prey)==dog,"The exact 24-block boundary is protected");
        dog.moveTo(prey.position().add(24.01,0,0));h.assertTrue(GuardDogs.guard(prey)==null,"Protection ends outside 24 blocks");
        dog.moveTo(prey.position().add(18,0,18));h.assertTrue(GuardDogs.guard(prey)==null,"The radius is spherical rather than a 48-block-wide cube");
        dog.moveTo(prey.position().add(0,24,0));h.assertTrue(GuardDogs.guard(prey)==dog,"Vertical distance also uses the same sphere");
        dog.moveTo(prey.position().add(0,0,1));sit(dog,false);h.assertTrue(GuardDogs.guard(prey)==null,"Standing dogs do not guard");
        sit(dog,true);dog.setTame(false,true);h.assertTrue(GuardDogs.guard(prey)==null,"A sitting wild wolf does not guard");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void oneBlockedPackCostsOneSilentHeartAndFeedingRestoresIt(GameTestHelper h){
        floor(h);var a=still(h,EntityType.WOLF,2,6);var b=still(h,EntityType.WOLF,2,10);
        var g=a.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(a.blockPosition()),MobSpawnType.NATURAL,null);
        b.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.NATURAL,g);
        var prey=still(h,EntityType.SHEEP,15,8);var dog=dog(h,14,8);var other=dog(h,14,12);
        float health=dog.getHealth(),angle=dog.getTailAngle();var motion=dog.getDeltaMovement();int invulnerable=dog.invulnerableTime;
        a.setTarget(prey);b.setTarget(prey);
        h.assertTrue(a.getTarget()==null&&b.getTarget()==null,"Neither pack member acquires the guarded sheep");
        h.assertTrue(!WolfPacks.hungry(a)&&!WolfPacks.hungry(b)&&WolfPacks.deadline(a)==WolfPacks.deadline(b),"One attempted hunt feeds the whole pack");
        h.assertTrue(WolfPacks.deadline(a)==h.getLevel().getGameTime()+24000,"Deterrence uses the ordinary one-day pack deadline");
        h.assertTrue(dog.getHealth()==health-2&&other.getHealth()==other.getMaxHealth(),"Only the nearest guard loses one heart, once per pack");
        h.assertTrue(dog.getTailAngle()<angle,"The ordinary synchronized health-based tail drops one step");
        h.assertTrue(dog.hurtTime==0&&dog.invulnerableTime==invulnerable&&dog.getLastHurtByMob()==null
                &&dog.getDeltaMovement().equals(motion)&&dog.isOrderedToSit()&&dog.isInSittingPose(),"Fatigue does not invoke damage, knockback, retaliation or standing");
        for(int i=0;i<10;i++){a.setTarget(prey);b.setTarget(prey);}
        h.assertTrue(dog.getHealth()==health-2,"Repeated target scans during the cooldown cannot drain the guard");
        var owner=h.makeMockPlayer(GameType.SURVIVAL);dog.setOwnerUUID(owner.getUUID());owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF));
        dog.mobInteract(owner,InteractionHand.MAIN_HAND);
        h.assertTrue(dog.getHealth()==health&&dog.getTailAngle()==angle&&owner.getMainHandItem().isEmpty(),"Normal feeding restores health and tail angle and consumes one food");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=95)
    public static void sittingDownInterruptsAnAlreadyStalkingPack(GameTestHelper h){
        floor(h);var a=h.spawn(EntityType.WOLF,2,2,6);var b=h.spawn(EntityType.WOLF,2,2,10);
        var g=a.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(a.blockPosition()),MobSpawnType.NATURAL,null);
        b.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.NATURAL,g);
        a.setBaby(false);b.setBaby(false);var prey=still(h,EntityType.SHEEP,15,8);var dog=dog(h,14,12);sit(dog,false);
        h.runAfterDelay(40,()->{
            h.assertTrue(((WolfState)a).ffm$stalking()&&((WolfState)b).ffm$stalking(),"The real pack begins an unguarded stalk");sit(dog,true);
        });
        h.runAfterDelay(55,()->{
            for(var w:List.of(a,b))h.assertTrue(w.getTarget()==null&&!WolfPacks.hungry(w)&&!((WolfState)w).ffm$stalking(),"Sitting the guard ends every member's existing hunt");
            h.assertTrue(prey.getHealth()==prey.getMaxHealth()&&dog.getHealth()==dog.getMaxHealth()-2,"The sheep is unharmed and the guard is charged only once");h.succeed();
        });
    }

    @GameTest(template="test_empty")
    public static void foxDeterrencePersistsAndOtherPreyWaitForItsMealCooldown(GameTestHelper h){
        floor(h);var fox=still(h,EntityType.FOX,2,8);var chicken=still(h,EntityType.CHICKEN,12,8);var dog=dog(h,13,8);
        fox.setTarget(chicken);
        h.assertTrue(fox.getTarget()==null&&!GuardDogs.foxHungry(fox)&&dog.getHealth()==dog.getMaxHealth()-2,"A guard stops chicken hunting and charges one fatigue event");
        long until=GuardDogs.foxDeadline(fox);var tag=fox.saveWithoutId(new CompoundTag());fox.discard();
        var loaded=EntityType.FOX.create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(GuardDogs.foxDeadline(loaded)==until,"Fox meal deadline survives entity save/load");
        var rabbit=still(h,EntityType.RABBIT,10,10);loaded.setTarget(rabbit);
        h.assertTrue(loaded.getTarget()==null,"A fed fox does not immediately switch to another food animal");
        var enemy=still(h,EntityType.ZOMBIE,10,12);loaded.setTarget(enemy);
        h.assertTrue(loaded.getTarget()==enemy,"Fox defensive non-food targets remain available");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void newGuardStopsAnExistingFoxStalkAndCannotBeKilledByFatigue(GameTestHelper h){
        floor(h);var fox=still(h,EntityType.FOX,2,8);var chicken=still(h,EntityType.CHICKEN,12,8);var dog=dog(h,13,8);sit(dog,false);
        fox.setTarget(chicken);fox.setIsCrouching(true);fox.setIsInterested(true);
        h.assertTrue(fox.getTarget()==chicken,"A standing dog permits a chicken hunt");
        sit(dog,true);dog.setHealth(1);GuardDogs.beforeTick(fox);
        h.assertTrue(fox.getTarget()==null&&!fox.isCrouching()&&!fox.isInterested()&&!GuardDogs.foxHungry(fox),"An active fox stalk is cleared when guarding starts");
        h.assertTrue(dog.isAlive()&&dog.getHealth()==1&&dog.hurtTime==0,"Fatigue never kills or visibly hurts an exhausted dog");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void guardedMeleeCannotBypassTargetChecks(GameTestHelper h){
        floor(h);var dog=dog(h,12,8);var prey=still(h,EntityType.SHEEP,10,8);var wolf=still(h,EntityType.WOLF,9,8);
        h.assertFalse(wolf.doHurtTarget(prey),"A direct wild melee hit is canceled even without target acquisition");
        h.assertTrue(prey.getHealth()==prey.getMaxHealth()&&prey.hurtTime==0&&!WolfPacks.hungry(wolf),"The sheep receives no damage or hurt reaction and the attacker is fed");
        var fox=still(h,EntityType.FOX,9,10);var chicken=still(h,EntityType.CHICKEN,10,10);
        h.assertFalse(fox.doHurtTarget(chicken),"A direct fox attack is also canceled");
        h.assertTrue(chicken.getHealth()==chicken.getMaxHealth()&&dog.getHealth()==dog.getMaxHealth()-4,"Two independent hunters cost two hearts, without harming livestock");h.succeed();
    }

    @GameTest(template="test_empty")
    public static void guardingDoesNotTaxIdleDogsOrBlockTamedOwnerDefense(GameTestHelper h){
        floor(h);var dog=dog(h,12,8);var prey=still(h,EntityType.SHEEP,10,8);
        var pet=dog(h,3,8);sit(pet,false);pet.setTarget(prey);
        h.assertTrue(pet.getTarget()==prey&&dog.getHealth()==dog.getMaxHealth(),"Tamed combat is not treated as a wild hunt");
        var wolf=still(h,EntityType.WOLF,3,12);var enemy=still(h,EntityType.ZOMBIE,8,12);wolf.setTarget(enemy);
        h.assertTrue(wolf.getTarget()==enemy&&WolfPacks.hungry(wolf),"A guardian does not alter unrelated wild defensive combat");
        for(int i=0;i<20;i++)GuardDogs.beforeTick(wolf);
        h.assertTrue(dog.getHealth()==dog.getMaxHealth(),"An idle guard is never taxed merely for nearby animals");h.succeed();
    }

    @GameTest(template="test_empty",timeoutTicks=140,batch="ffm_guard_retreat")
    public static void deterredPredatorsWalkAwayFromGuardedLivestock(GameTestHelper h){
        for(int x=0;x<40;x++)for(int z=0;z<40;z++)h.setBlock(x,1,z,Blocks.STONE);
        var sheep=still(h,EntityType.SHEEP,12,8);var chicken=still(h,EntityType.CHICKEN,12,28);
        var first=dog(h,12,12);var second=dog(h,12,32);
        var wolf=h.spawn(EntityType.WOLF,8,2,8);wolf.setBaby(false);
        var fox=h.spawn(EntityType.FOX,8,2,28);
        fox.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(fox.blockPosition()),MobSpawnType.COMMAND,null);
        GuardDogs.install(wolf);GuardDogs.install(fox); // Rejoining a level must not install duplicates.
        h.assertTrue(wolf.goalSelector.getAvailableGoals().stream().filter(g->g.getGoal() instanceof GuardDogs.AvoidProtected<?>).count()==1,
                "The guard avoidance goal is installed exactly once");
        h.succeedWhen(()->{
            h.assertTrue(wolf.distanceToSqr(sheep)>64&&fox.distanceToSqr(chicken)>64,"Both predators walk away from the guarded livestock through ordinary navigation");
            h.assertTrue(sheep.getHealth()==sheep.getMaxHealth()&&chicken.getHealth()==chicken.getMaxHealth(),"Retreat causes no livestock damage");
        });
    }

    @GameTest(template="test_empty",timeoutTicks=25)
    public static void foxMealFromARealKillExpiresWithoutGuardSideEffects(GameTestHelper h){
        floor(h);var fox=still(h,EntityType.FOX,8,8);var prey=still(h,EntityType.CHICKEN,9,8);prey.setHealth(.1F);
        fox.doHurtTarget(prey);h.assertFalse(GuardDogs.foxHungry(fox),"A successful ordinary prey kill starts the fox hunger cooldown");
        h.assertTrue(GuardDogs.foxDeadline(fox)==h.getLevel().getGameTime()+24000,"Fox meal length matches wild wolves");
        // Shorten only the fixture's saved deadline to exercise expiration without waiting twenty minutes.
        fox.getPersistentData().putLong("ffm_fox_meal_until",h.getLevel().getGameTime()+6);
        h.runAfterDelay(8,()->{var next=still(h,EntityType.CHICKEN,10,8);fox.setTarget(next);h.assertTrue(GuardDogs.foxHungry(fox)&&fox.getTarget()==next,"A fox can hunt again after its saved deadline");h.succeed();});
    }
}
