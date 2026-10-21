package net.itsrileynj.fantasyfeudalismmobs.mixin;

import java.util.UUID;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerCatPlayMixin implements VillagerPlayState {
    @Unique private UUID ffm$cat;
    @Unique private long ffm$until;
    @Inject(method="registerBrainGoals",at=@At("TAIL"))
    private void ffm$play(Brain<Villager> brain,CallbackInfo ci) {
        if(((Villager)(Object)this).isBaby())brain.addActivity(Activity.PLAY,ImmutableList.of(Pair.of(1,new BabyCatPlay())));
    }
    @Override public UUID ffm$playCat(){return ffm$cat;}
    @Override public long ffm$playUntil(){return ffm$until;}
    @Override public void ffm$playCat(UUID cat,long until){ffm$cat=cat;ffm$until=until;}
}
