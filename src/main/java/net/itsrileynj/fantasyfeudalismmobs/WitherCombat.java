package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;

public final class WitherCombat {
    public static final String BOW_SHOT="ffm_wither_bow";
    private WitherCombat(){}
    public static void equip(WitherSkeleton skeleton,RandomSource random){
        boolean bow=skeleton.level().getDifficulty()==Difficulty.HARD&&random.nextInt(100)<10;
        int sword=bow?0:random.nextInt(100);
        skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(bow?Items.BOW:sword<80?Items.STONE_SWORD:sword<95?Items.IRON_SWORD:Items.GOLDEN_SWORD));
        skeleton.setItemSlot(EquipmentSlot.OFFHAND,!bow&&random.nextBoolean()?new ItemStack(Items.SHIELD):ItemStack.EMPTY);
    }
    public static void prepareArrow(AbstractArrow arrow){
        if(arrow.level().getDifficulty()==Difficulty.HARD&&arrow.getOwner() instanceof WitherSkeleton
                &&arrow.getWeaponItem()!=null&&arrow.getWeaponItem().getItem() instanceof BowItem)
            arrow.getPersistentData().putBoolean(BOW_SHOT,true);
    }
    public static boolean specialArrow(AbstractArrow arrow){
        return arrow.level().getDifficulty()==Difficulty.HARD&&arrow.getPersistentData().getBoolean(BOW_SHOT);
    }
    public static final class ShieldAdvance extends MeleeAttackGoal {
        private final WitherSkeleton skeleton;
        private boolean charging;
        private int repath;
        public ShieldAdvance(WitherSkeleton skeleton){super(skeleton,1.2,true);this.skeleton=skeleton;}
        private boolean eligible(){
            return skeleton.getMainHandItem().getItem() instanceof SwordItem&&skeleton.getOffhandItem().is(Items.SHIELD)
                    &&skeleton.getTarget() instanceof Player player&&player.isAlive()&&!player.isCreative()&&!player.isSpectator();
        }
        @Override public boolean canUse(){return eligible()&&super.canUse();}
        @Override public boolean canContinueToUse(){return eligible()&&super.canContinueToUse();}
        @Override public void start(){
            super.start();charging=skeleton.distanceToSqr(skeleton.getTarget())<=25;repath=0;stance();
        }
        private void stance(){
            if(charging){skeleton.stopUsingItem();skeleton.setShiftKeyDown(false);skeleton.setPose(Pose.STANDING);skeleton.setSprinting(true);}
            else{skeleton.setSprinting(false);skeleton.setShiftKeyDown(true);skeleton.setPose(Pose.CROUCHING);
                if(!skeleton.isUsingItem())skeleton.startUsingItem(InteractionHand.OFF_HAND);}
        }
        @Override public void tick(){
            var target=skeleton.getTarget();if(target==null)return;
            if(!charging&&skeleton.distanceToSqr(target)<=25)charging=true;
            stance();
            if(charging){super.tick();return;}
            skeleton.getLookControl().setLookAt(target,30,30);
            if(--repath<=0){repath=10;skeleton.getNavigation().moveTo(target,.65);}
        }
        @Override public void stop(){
            super.stop();skeleton.stopUsingItem();skeleton.setShiftKeyDown(false);skeleton.setPose(Pose.STANDING);skeleton.setSprinting(false);
        }
    }
}
