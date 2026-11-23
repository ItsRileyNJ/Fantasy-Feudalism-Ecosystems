package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import java.util.function.Consumer;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.minecraft.gametest.framework.*;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormSoundTests {
    @GameTest(template="test_empty")
    public static void rootWormAmbientClicksUseNativeSchedulerAndRespectSilence(GameTestHelper h) {
        var worm=h.spawn(RootWormModule.ROOT_WORM.get(),8,2,8);worm.setNoAi(true);int[] calls={0};
        Consumer<PlayLevelSoundEvent> listener=e->{
            if(e.getLevel()==h.getLevel()&&e.getSound()!=null&&e.getSound().value()==FantasyFeudalismMobs.ROOT_WORM_AMBIENT.get()) {
                calls[0]++;h.assertTrue(e.getSource()==SoundSource.NEUTRAL,"Root Worm ambience uses the native neutral-mob sound category");
                h.assertTrue(e instanceof PlayLevelSoundEvent.AtPosition pos&&pos.getPosition().distanceTo(worm.position())<.01,"Clicks originate at the worm's position");
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            worm.ambientSoundTime=1000;worm.baseTick();
            h.assertTrue(calls[0]==1&&worm.ambientSoundTime==-120,"Native ambient scheduler plays the registered clicks and resets the interval");
            worm.baseTick();h.assertTrue(calls[0]==1,"Ambient cooldown prevents a second immediate click");
            worm.setSilent(true);worm.ambientSoundTime=1000;worm.baseTick();h.assertTrue(calls[0]==1,"Silent worms do not play clicks");
        } finally {NeoForge.EVENT_BUS.unregister(listener);}
        h.succeed();
    }
}
