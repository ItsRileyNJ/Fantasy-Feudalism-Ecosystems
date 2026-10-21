package net.itsrileynj.fantasyfeudalismmobs.gametest;

import com.mojang.logging.LogUtils;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/** Read-only diagnostics for a supplied test that assumes the vanilla skeleton weapon. */
@EventBusSubscriber(modid=FantasyFeudalismMobs.MOD_ID)
public final class CompatibilityDiagnostics {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void spawned(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractSkeleton skeleton) || !GameTestHooks.isGametestServer()) return;
        boolean mountedFixture=StackWalker.getInstance().walk(frames->frames.anyMatch(frame->
                frame.getClassName().equals("dev.villagerscontract.MountedGuardGameTests")
                && frame.getMethodName().equals("mountedCrossbowRetreatRestartsAcrossGrassyStep")));
        if(mountedFixture) LogUtils.getLogger().info("Compatibility diagnostic: native mounted-retreat fixture skeleton holds {} at {}",
                skeleton.getMainHandItem(),skeleton.position());
    }
}
