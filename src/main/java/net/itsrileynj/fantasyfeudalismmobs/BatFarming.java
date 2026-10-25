package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class BatFarming {
    public static final String NEXT="ffm_bat_next_fertilizer";
    public static final long COOLDOWN=5*60*20;
    private BatFarming(){}
    public static boolean ready(Bat bat){return bat.level().getGameTime()>=bat.getPersistentData().getLong(NEXT);}
    public static boolean canFeed(Bat bat){return ready(bat)||bat.getHealth()<bat.getMaxHealth();}
    public static void feed(Bat bat,Player player,InteractionHand hand,Vec3 at){
        var level=(ServerLevel)bat.level();boolean fertilizer=ready(bat);
        bat.heal(4);player.getItemInHand(hand).consume(1,player);
        level.sendParticles(ParticleTypes.HEART,at.x,at.y+.4,at.z,3,.18,.15,.18,0);
        if(fertilizer){
            bat.getPersistentData().putLong(NEXT,level.getGameTime()+COOLDOWN);
            var drop=new ItemEntity(level,at.x,at.y,at.z,new ItemStack(FantasyFeudalismMobs.FERTILIZER.get()));
            drop.setDefaultPickUpDelay();level.addFreshEntity(drop);
            EcosystemAchievements.grant(player,"natural_helpers");
        }
    }
    public static InteractionResult interact(Player player,InteractionHand hand,Bat bat){
        if(!player.getItemInHand(hand).is(Items.APPLE)||!BatResidents.housed(bat))return InteractionResult.PASS;
        if(player.isSpectator()||!canFeed(bat))return InteractionResult.FAIL;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        var claim=BatColonies.get((ServerLevel)player.level()).home(bat.getUUID());
        var marker=bat.getPersistentData().getCompound(BatResidents.HOME);
        if(claim==null||!claim.dimension().equals(marker.getString("Dimension"))||claim.pos().asLong()!=marker.getLong("Pos"))return InteractionResult.FAIL;
        feed(bat,player,hand,bat.position());return InteractionResult.CONSUME;
    }
}
