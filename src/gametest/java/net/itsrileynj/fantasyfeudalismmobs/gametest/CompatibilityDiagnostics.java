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
    private static int lastDone=-1,lastProgress;
    private static String pendingName="";
    private static long pendingTick=Long.MIN_VALUE;
    @SubscribeEvent public static void progress(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(!(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer server)||server.getTickCount()%200!=0)return;
        try{
            var field=net.minecraft.gametest.framework.GameTestServer.class.getDeclaredField("testTracker");field.setAccessible(true);
            var tracker=(net.minecraft.gametest.framework.MultipleTestTracker)field.get(server);if(tracker==null)return;
            if(tracker.getDoneCount()!=lastDone){lastDone=tracker.getDoneCount();lastProgress=server.getTickCount();return;}
            var testsField=net.minecraft.gametest.framework.MultipleTestTracker.class.getDeclaredField("tests");testsField.setAccessible(true);
            for(Object value:(java.util.Collection<?>)testsField.get(tracker)){
                var test=(net.minecraft.gametest.framework.GameTestInfo)value;if(test.isDone()||test.getStructureBlockPos()==null)continue;
                var startField=net.minecraft.gametest.framework.GameTestInfo.class.getDeclaredField("startTick");startField.setAccessible(true);
                var tickField=net.minecraft.gametest.framework.GameTestInfo.class.getDeclaredField("tickCount");tickField.setAccessible(true);
                long tick=tickField.getLong(test);
                if(!pendingName.equals(test.getTestName())||pendingTick!=tick){pendingName=test.getTestName();pendingTick=tick;lastProgress=server.getTickCount();return;}
                if(server.getTickCount()-lastProgress<1000)return;
                LogUtils.getLogger().warn("Test harness progress diagnostic: name={}, started={}, worldTime={}, startTime={}, testTick={}, timeout={}, structure={}",
                        test.getTestName(),test.hasStarted(),test.getLevel().getGameTime(),startField.getLong(test),tickField.getLong(test),test.getTimeoutTicks(),test.getStructureBlockPos());
                break;
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
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
