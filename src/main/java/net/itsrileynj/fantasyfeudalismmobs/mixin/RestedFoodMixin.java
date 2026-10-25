package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class RestedFoodMixin {
    @Unique private Player ffm$owner;
    @Inject(method="tick",at=@At("HEAD"))
    private void ffm$bindPlayer(Player player,CallbackInfo ci){ffm$owner=player;}
    @Unique private boolean ffm$rested(){return ffm$owner!=null&&ffm$owner.hasEffect(FantasyFeudalismMobs.RESTED);}
    @ModifyVariable(method="addExhaustion",at=@At("HEAD"),argsOnly=true)
    private float ffm$halfExhaustion(float amount){return ffm$rested()?amount*.5F:amount;}
    // Both native regeneration branches, preserving their hunger/gamerule checks and timers.
    @ModifyArg(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;heal(F)V"),index=0,require=2)
    private float ffm$naturalHealing(float amount){return ffm$rested()?amount*1.35F:amount;}
}
