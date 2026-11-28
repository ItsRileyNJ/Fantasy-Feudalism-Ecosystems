package net.itsrileynj.fantasyfeudalismmobs;

import javax.annotation.Nullable;
import net.minecraft.world.entity.animal.Pig;

public interface PigState {
    boolean ffm$digging();
    void ffm$setDigging(boolean digging);
    float ffm$digAmount(float partialTick);
    @Nullable Pig ffm$herdTarget();
    void ffm$setHerdTarget(@Nullable Pig target);
}
