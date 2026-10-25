package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** FoodData applies this benefit only to exhaustion and native food-based regeneration. */
public final class RestedEffect extends MobEffect {
    public static final int DURATION=6*60*20;
    public RestedEffect(){super(MobEffectCategory.BENEFICIAL,0x78B9F2);}
}
