package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {
    protected MobMixin(EntityType<? extends LivingEntity> type, Level level) { super(type, level); }

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void ffm$successfulAttack(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) MobRules.afterHit((Mob)(Object)this, target);
    }

    @Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"), cancellable = true)
    private void ffm$armor(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (MobRules.armorEligible(mob)) { MobRules.equipArmor(mob, random, difficulty); ci.cancel(); }
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void ffm$armorForVariants(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                    SpawnGroupData group, CallbackInfoReturnable<SpawnGroupData> cir) {
        Mob mob = (Mob)(Object)this;
        MobRules.finalizeArmor(mob, mob.getRandom(), difficulty);
        SpawnHerd.spawned(mob, cir.getReturnValue());
    }

    @Inject(method = "enchantSpawnedArmor", at = @At("HEAD"), cancellable = true)
    private void ffm$enchantArmor(ServerLevelAccessor level, RandomSource random, EquipmentSlot slot,
                                  DifficultyInstance difficulty, CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (MobRules.armorEligible(mob)) ci.cancel(); // Already rolled exactly once in finalizeSpawn, including variants with overrides.
    }

    @Inject(method = "jumpInLiquidInternal", at = @At("HEAD"), cancellable = true)
    private void ffm$noSwimming(Runnable onSuper, CallbackInfo ci) {
        if ((Object)this instanceof Spider) ci.cancel();
    }

    @Inject(method = "canBeLeashed", at = @At("HEAD"), cancellable = true)
    private void ffm$companionLeash(CallbackInfoReturnable<Boolean> cir) {
        if (SpiderCompanion.isTamed((Entity)(Object)this)) cir.setReturnValue(true);
    }

    @Inject(method = "getMaxSpawnClusterSize", at = @At("HEAD"), cancellable = true)
    private void ffm$groupLimit(CallbackInfoReturnable<Integer> cir) {
        if ((Object)this instanceof Zombie || (Object)this instanceof Silverfish) cir.setReturnValue(9);
    }
}
