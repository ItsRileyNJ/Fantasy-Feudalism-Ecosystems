package net.itsrileynj.fantasyfeudalismmobs;

import javax.annotation.Nullable;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

final class FreezingEffects {
    private static final int COLOR = 0x7ACFFF;
    private static final int DURATION = 60;

    private FreezingEffects() {}

    /** Apply a fixed timer even when vanilla scales effects from arrows or lingering clouds. */
    static final class InstantFreezing extends InstantenousMobEffect {
        InstantFreezing() {
            super(MobEffectCategory.HARMFUL, COLOR);
        }

        @Override
        public void onEffectStarted(LivingEntity target, int amplifier) {
            freeze(target, null);
        }

        @Override
        public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity owner,
                                           LivingEntity target, int amplifier, double intensity) {
            freeze(target, owner == null ? source : owner);
        }

        private void freeze(LivingEntity target, @Nullable Entity source) {
            if (!target.level().isClientSide && target.isAlive() && target.canFreeze()) {
                target.addEffect(new MobEffectInstance(SlimeVariants.FROZEN, DURATION), source);
            }
        }
    }

    static final class Frozen extends MobEffect {
        Frozen() {
            super(MobEffectCategory.HARMFUL, COLOR);
            var id = SlimeVariants.id("frozen");
            addAttributeModifier(Attributes.MOVEMENT_SPEED, id, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.FLYING_SPEED, id, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            addAttributeModifier(Attributes.JUMP_STRENGTH, id, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }

        @Override
        public void onEffectStarted(LivingEntity target, int amplifier) {
            if (!target.level().isClientSide) holdFrost(target);
        }

        @Override
        public boolean applyEffectTick(LivingEntity target, int amplifier) {
            if (!target.canFreeze()) {
                target.setTicksFrozen(0);
                return false;
            }
            var effect = target.getEffect(SlimeVariants.FROZEN);
            if (effect != null && effect.getDuration() == 1 && !target.isInPowderSnow) {
                target.setTicksFrozen(0);
            } else {
                holdFrost(target);
            }
            return true;
        }

        private static void holdFrost(LivingEntity target) {
            // Outside powder snow, vanilla thaws two ticks of frost each tick.
            target.setTicksFrozen(target.getTicksRequiredToFreeze() + 2);
        }
    }
}
