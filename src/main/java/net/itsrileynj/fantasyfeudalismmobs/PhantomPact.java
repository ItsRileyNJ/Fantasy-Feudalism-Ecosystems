package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Comparator;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** A temporary defensive pact, not permanent taming or permission to attack unprovoked targets. */
public final class PhantomPact {
    public static final String PACT="ffm_phantom_pact";
    private PhantomPact(){}
    public static boolean bound(Entity entity){return entity instanceof Phantom&&entity.getPersistentData().getCompound(PACT).hasUUID("Owner");}
    @Nullable public static UUID ownerId(Phantom phantom){return bound(phantom)?phantom.getPersistentData().getCompound(PACT).getUUID("Owner"):null;}
    @Nullable public static Player owner(Phantom phantom){return bound(phantom)?phantom.level().getPlayerByUUID(ownerId(phantom)):null;}
    public static InteractionResult interact(Player player,InteractionHand hand,Phantom phantom){
        var eye=player.getItemInHand(hand);if(!eye.is(Items.ENDER_EYE))return InteractionResult.PASS;
        if(player.isSpectator()||!phantom.isAlive()||!NightCycle.isNight(player.level())||bound(phantom))return InteractionResult.FAIL;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        var tag=new CompoundTag();tag.putUUID("Owner",player.getUUID());
        tag.putLong("Sunrise",NightCycle.nextSunrise(player.level()));tag.put("Threats",new CompoundTag());
        phantom.getPersistentData().put(PACT,tag);phantom.setTarget(null);phantom.setLastHurtByMob(null);eye.consume(1,player);
        return InteractionResult.CONSUME;
    }
    public static boolean allied(Phantom phantom,Entity other){
        UUID id=ownerId(phantom);if(id==null||other==null)return false;
        return other.getUUID().equals(id)||(other instanceof Phantom guard&&id.equals(ownerId(guard)))
                ||(other instanceof TamableAnimal pet&&id.equals(pet.getOwnerUUID()))
                ||(SpiderCompanion.isTamed(other)&&id.equals(SpiderCompanion.data(other).getUUID("owner")));
    }
    public static boolean mayAttack(Phantom phantom,LivingEntity target){
        if(!bound(phantom))return true;
        if(target==null||target==phantom||!target.isAlive()||allied(phantom,target)||target.isAlliedTo(phantom))return false;
        if(target instanceof Player player&&(player.isCreative()||player.isSpectator()))return false;
        Player owner=owner(phantom);if(owner==null||!owner.isAlive())return false;
        if(target instanceof Player player&&!owner.canHarmPlayer(player))return false;
        return phantom.getPersistentData().getCompound(PACT).getCompound("Threats").getBoolean(target.getUUID().toString());
    }
    public static void playerHurt(Player player,LivingEntity attacker){
        if(!(player.level() instanceof ServerLevel level)||!attacker.isAlive()||attacker==player)return;
        for(var phantom:level.getEntitiesOfClass(Phantom.class,player.getBoundingBox().inflate(64),p->player.getUUID().equals(ownerId(p)))){
            var pact=phantom.getPersistentData().getCompound(PACT);
            pact.getCompound("Threats").putBoolean(attacker.getUUID().toString(),true);
            if(mayAttack(phantom,attacker))phantom.setTarget(attacker);
        }
    }
    /** Called after native goals so the idle orbit follows the owner while vanilla sweep attacks remain intact. */
    @Nullable public static BlockPos tick(Phantom phantom){
        if(!bound(phantom))return null;
        var pact=phantom.getPersistentData().getCompound(PACT);
        if(!NightCycle.isNight(phantom.level())||phantom.level().getDayTime()>=pact.getLong("Sunrise")){
            phantom.getPersistentData().remove(PACT);phantom.setTarget(null);return null;
        }
        Player player=owner(phantom);
        if(player==null||!player.isAlive()||player.isSpectator()){phantom.setTarget(null);return null;}
        if(phantom.getTarget()!=null&&!mayAttack(phantom,phantom.getTarget()))phantom.setTarget(null);
        if(phantom.getTarget()==null&&phantom.tickCount%10==0){
            phantom.level().getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(64),target->mayAttack(phantom,target))
                    .stream().min(Comparator.comparingDouble(phantom::distanceToSqr)).ifPresent(phantom::setTarget);
        }
        return player.blockPosition().above(10);
    }
}
