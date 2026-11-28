package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.WolfPacks;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PathNavigation.class)
public abstract class WolfHuntNavigationMixin {
    @Shadow @Final protected Mob mob;

    @ModifyArg(method = "createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;"),
            index = 4)
    private float ffm$allowDistantSheepPaths(float range) {
        if (mob instanceof Wolf wolf && WolfPacks.handles(wolf, wolf.getTarget())) {
            return Math.max(range, WolfPacks.SHEEP_DETECTION_RANGE);
        }
        return range;
    }
}
