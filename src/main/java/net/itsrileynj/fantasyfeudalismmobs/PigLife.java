package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class PigLife {
    public static final String NEXT_DIG="ffm_pig_next_dig";
    public static final String PARENT="ffm_pig_parent",OTHER_PARENT="ffm_pig_other_parent";
    public static final int DIG_INTERVAL=2400;
    private PigLife() {}

    public static double mushroomChance(BlockState soil) {
        if(soil.is(Blocks.PODZOL))return .80;
        if(soil.is(Blocks.ROOTED_DIRT))return .50;
        if(soil.is(Blocks.COARSE_DIRT))return .15;
        return 0;
    }
    public static boolean mushroom(ItemStack stack) { return stack.is(Items.RED_MUSHROOM)||stack.is(Items.BROWN_MUSHROOM); }
    public static void schedule(Pig pig) {
        pig.getPersistentData().putLong(NEXT_DIG,pig.level().getGameTime()+DIG_INTERVAL+pig.getRandom().nextInt(401)-200);
    }
    public static long nextDig(Pig pig) {
        if(!pig.getPersistentData().contains(NEXT_DIG))schedule(pig);
        return pig.getPersistentData().getLong(NEXT_DIG);
    }
    public static boolean grow(Pig pig) {
        if(!pig.isAlive()||!pig.isBaby()||pig.level().isClientSide)return false;
        int remaining=-pig.getAge();
        pig.setAge(pig.getAge()+Math.max(1,(int)Math.ceil(remaining*.20)));
        if(pig.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,pig.getX(),pig.getY()+.4,pig.getZ(),5,.2,.2,.2,0);
        pig.playSound(SoundEvents.GENERIC_EAT,.6F,1.2F);
        return true;
    }
    public static void born(Animal first,Animal second,AgeableMob child) {
        if(first instanceof Pig && second instanceof Pig && child instanceof Pig pig) {
            pig.getPersistentData().putUUID(PARENT,first.getUUID());
            pig.getPersistentData().putUUID(OTHER_PARENT,second.getUUID());
        }
    }
}
