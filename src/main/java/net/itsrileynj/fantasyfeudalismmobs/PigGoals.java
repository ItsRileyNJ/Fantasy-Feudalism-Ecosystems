package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class PigGoals {
    private PigGoals() {}
    private static boolean free(Pig pig) {
        return pig.isAlive()&&!pig.isVehicle()&&!pig.isPassenger()&&!pig.isLeashed()&&!pig.isInWater()
                &&!pig.isPanicking()&&!pig.isInLove();
    }

    public static final class Dig extends Goal {
        private final Pig pig;
        private int remaining;
        private BlockPos soil;
        public Dig(Pig pig) { this.pig=pig;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP)); }
        @Override public boolean canUse() {
            if(pig.isBaby()||!free(pig)||!pig.onGround()||pig.level().getGameTime()<PigLife.nextDig(pig))return false;
            soil=pig.blockPosition().below();
            if(PigLife.mushroomChance(pig.level().getBlockState(soil))>0)return true;
            // No area search: unsupported ground is checked at most once per second.
            pig.getPersistentData().putLong(PigLife.NEXT_DIG,pig.level().getGameTime()+20);
            return false;
        }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public boolean canContinueToUse() { return remaining>0&&((PigState)pig).ffm$digging()&&!pig.isBaby()&&free(pig)&&pig.onGround(); }
        @Override public void start() {
            soil=pig.blockPosition().below();remaining=60;pig.getNavigation().stop();((PigState)pig).ffm$setDigging(true);
            // Persist the cooldown before any reward; reloading an interrupted dig cannot duplicate it.
            PigLife.schedule(pig);
        }
        @Override public void stop() { remaining=0;((PigState)pig).ffm$setDigging(false); }
        @Override public void tick() {
            if(remaining<=0||!((PigState)pig).ffm$digging()||!free(pig)||pig.isBaby())return;
            remaining--;
            if(remaining%12==0&&soil.equals(pig.blockPosition().below())) {
                var state=pig.level().getBlockState(soil);
                if(PigLife.mushroomChance(state)>0) {
                    pig.level().levelEvent(2001,soil,Block.getId(state));
                    pig.playSound(state.getSoundType().getHitSound(),.35F,.8F);
                }
            }
            if(remaining==12&&soil.equals(pig.blockPosition().below())) {
                double chance=PigLife.mushroomChance(pig.level().getBlockState(soil));
                if(pig.getRandom().nextDouble()<chance)
                    pig.spawnAtLocation(pig.getRandom().nextFloat()<.8F?Items.BROWN_MUSHROOM:Items.RED_MUSHROOM);
            }
        }
    }

    public static final class EatMushroom extends Goal {
        private final Pig pig;
        private ItemEntity food;
        private long nextScan,nextPath,finishAt;
        public EatMushroom(Pig pig) { this.pig=pig;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));nextScan=pig.getId()%20; }
        private boolean edible(ItemEntity item) {
            return item!=null&&item.isAlive()&&!item.hasPickUpDelay()&&item.getTarget()==null&&PigLife.mushroom(item.getItem());
        }
        @Override public boolean canUse() {
            long now=pig.level().getGameTime();
            if(!pig.isBaby()||!free(pig)||now<nextScan)return false;
            nextScan=now+20+Math.floorMod(pig.getId()-now,20);
            food=pig.level().getEntitiesOfClass(ItemEntity.class,pig.getBoundingBox().inflate(6,2,6),this::edible)
                    .stream().min(Comparator.comparingDouble(pig::distanceToSqr)).orElse(null);
            return food!=null;
        }
        @Override public boolean canContinueToUse() {
            return pig.isBaby()&&free(pig)&&edible(food)&&pig.distanceToSqr(food)<100&&pig.level().getGameTime()<finishAt;
        }
        @Override public void start() { nextPath=0;finishAt=pig.level().getGameTime()+120; }
        @Override public void tick() {
            if(!edible(food)||!pig.isBaby()||!free(pig))return;
            pig.getLookControl().setLookAt(food);
            long now=pig.level().getGameTime();
            if(now>=nextPath) { nextPath=now+20;pig.getNavigation().moveTo(food,1); }
            if(pig.distanceToSqr(food)<1.5&&PigLife.grow(pig)) {
                food.getItem().shrink(1);if(food.getItem().isEmpty())food.discard();
                food=null;finishAt=0;nextScan=now+20;
            }
        }
        @Override public void stop() { food=null;pig.getNavigation().stop();nextScan=pig.level().getGameTime()+20; }
    }

    public static final class FollowFamily extends Goal {
        private final Pig pig;
        private Pig parent;
        private long nextScan,nextPath;
        public FollowFamily(Pig pig) { this.pig=pig;setFlags(EnumSet.of(Flag.MOVE)); }
        private Pig rememberedParent() {
            Pig result=null;
            if(pig.level() instanceof ServerLevel level)for(String key:new String[]{PigLife.PARENT,PigLife.OTHER_PARENT}) {
                if(pig.getPersistentData().hasUUID(key)&&level.getEntity(pig.getPersistentData().getUUID(key)) instanceof Pig adult
                        &&adult.isAlive()&&!adult.isBaby()&&pig.distanceToSqr(adult)<48*48
                        &&(result==null||pig.distanceToSqr(adult)<pig.distanceToSqr(result)))result=adult;
            }
            return result;
        }
        @Override public boolean canUse() {
            long now=pig.level().getGameTime();
            if(!pig.isBaby()||!free(pig)||now<nextScan)return false;
            nextScan=now+40+Math.floorMod(pig.getId()-now,40);parent=rememberedParent();
            if(parent==null)parent=pig.level().getEntitiesOfClass(Pig.class,pig.getBoundingBox().inflate(16,4,16),p->p.isAlive()&&!p.isBaby())
                    .stream().min(Comparator.comparingDouble(pig::distanceToSqr)).orElse(null);
            return parent!=null&&pig.distanceToSqr(parent)>9;
        }
        @Override public boolean canContinueToUse() {
            return pig.isBaby()&&free(pig)&&parent!=null&&parent.isAlive()&&!parent.isBaby()
                    &&pig.distanceToSqr(parent)>4&&pig.distanceToSqr(parent)<48*48;
        }
        @Override public void start() { nextPath=0; }
        @Override public void tick() {
            long now=pig.level().getGameTime();
            if(now>=nextPath) { nextPath=now+20;pig.getNavigation().moveTo(parent,1.1); }
        }
        @Override public void stop() { parent=null;pig.getNavigation().stop(); }
    }

    public static final class LooseHerd extends Goal {
        private final Pig pig;
        private Pig leader;
        private long nextScan,nextPath;
        public LooseHerd(Pig pig) { this.pig=pig;setFlags(EnumSet.of(Flag.MOVE));nextScan=pig.getId()%100; }
        @Override public boolean canUse() {
            long now=pig.level().getGameTime();
            if(pig.isBaby()||!free(pig)||now<nextScan)return false;
            nextScan=now+100+Math.floorMod(pig.getId()-now,100);
            leader=pig.level().getEntitiesOfClass(Pig.class,pig.getBoundingBox().inflate(32,6,32),p->!p.isBaby()&&free(p))
                    .stream().min(Comparator.comparing(Pig::getUUID)).orElse(pig);
            return leader!=pig&&pig.distanceToSqr(leader)>225;
        }
        @Override public boolean canContinueToUse() {
            return !pig.isBaby()&&free(pig)&&leader!=null&&free(leader)&&!leader.isBaby()
                    &&pig.distanceToSqr(leader)>121&&pig.distanceToSqr(leader)<32*32;
        }
        @Override public void start() { nextPath=0; }
        @Override public void tick() {
            long now=pig.level().getGameTime();
            if(now>=nextPath) { nextPath=now+20;pig.getNavigation().moveTo(leader,1); }
        }
        @Override public void stop() { leader=null;pig.getNavigation().stop(); }
    }
}
