package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.regex.Pattern;
import com.mojang.logging.LogUtils;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

/** Opt-in test-only filtering; no class or filter is included in production artifacts. */
@EventBusSubscriber(modid=FantasyFeudalismMobs.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
public final class TargetedGameTests {
    @SubscribeEvent public static void loaded(FMLLoadCompleteEvent event) {
        String filter=System.getProperty("ffm.gameTestFilter");
        if(filter==null||!GameTestHooks.isGametestServer())return;
        event.enqueueWork(()->{
            GameTestHooks.registerGametests();
            var tests=GameTestRegistry.getAllTestFunctions();int total=tests.size();
            var pattern=Pattern.compile(filter,Pattern.CASE_INSENSITIVE);
            tests.removeIf(test->!pattern.matcher(test.testName()).find());
            if(tests.isEmpty())throw new IllegalArgumentException("No GameTests match "+filter);
            LogUtils.getLogger().info("Targeted Ecosystems GameTests: {} of {} match {}",tests.size(),total,filter);
        });
    }
}
