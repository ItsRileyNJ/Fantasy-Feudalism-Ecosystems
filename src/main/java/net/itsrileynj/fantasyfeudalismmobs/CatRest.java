package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/** Only cats within four blocks when sleep starts can earn the benefit for that sleep. */
public final class CatRest {
    private static final String SESSION="ffm_cat_sleep";
    public static boolean allowed(Cat cat,Player player){
        return cat.isAlive()&&!cat.isOrderedToSit()&&(!cat.isTame()||player.getUUID().equals(cat.getOwnerUUID()));
    }
    public static void begin(Player player){
        if(!(player.level() instanceof ServerLevel))return;
        var state=new CompoundTag();var candidates=new ListTag();
        for(Cat cat:player.level().getEntitiesOfClass(Cat.class,player.getBoundingBox().inflate(4),
                cat->allowed(cat,player)&&cat.distanceToSqr(player)<=16))candidates.add(NbtUtils.createUUID(cat.getUUID()));
        state.put("Cats",candidates);
        player.getSleepingPos().ifPresent(pos->state.putLong("Bed",pos.asLong()));
        player.getPersistentData().put(SESSION,state);
    }
    public static boolean candidate(Cat cat,Player player){
        if(!player.isSleeping()||!allowed(cat,player))return false;
        var state=player.getPersistentData().getCompound(SESSION);
        for(Tag entry:state.getList("Cats",Tag.TAG_INT_ARRAY))if(cat.getUUID().equals(NbtUtils.loadUUID(entry)))return true;
        return false;
    }
    public static boolean sharingBed(Cat cat,Player player){
        if(!candidate(cat,player)||!cat.isLying()||cat.distanceToSqr(player)>2.5)return false;
        BlockPos bed=BlockPos.of(player.getPersistentData().getCompound(SESSION).getLong("Bed"));
        // A cat on the floor or on a neighboring unrelated bed does not count.
        return player.level().getBlockState(bed).is(BlockTags.BEDS)
                &&cat.getY()>=bed.getY()+.35&&cat.getY()<bed.getY()+1.5
                &&(cat.level().getBlockState(cat.blockPosition()).is(BlockTags.BEDS)
                    ||cat.level().getBlockState(cat.blockPosition().below()).is(BlockTags.BEDS));
    }
    @SubscribeEvent public static void wake(PlayerWakeUpEvent event){
        Player player=event.getEntity();
        if(!(player.level() instanceof ServerLevel level))return;
        boolean earned=false;
        if(player.isAlive()&&player.isSleeping()&&player.getSleepTimer()>=100){
            var state=player.getPersistentData().getCompound(SESSION);
            for(Tag entry:state.getList("Cats",Tag.TAG_INT_ARRAY)){
                var entity=level.getEntity(NbtUtils.loadUUID(entry));
                if(entity instanceof Cat cat&&sharingBed(cat,player)){earned=true;break;}
            }
        }
        player.getPersistentData().remove(SESSION);
        if(earned)player.addEffect(new MobEffectInstance(FantasyFeudalismMobs.RESTED,RestedEffect.DURATION,0,false,false,true));
    }
    private CatRest(){}
}
