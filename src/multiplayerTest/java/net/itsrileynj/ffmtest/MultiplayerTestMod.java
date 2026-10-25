package net.itsrileynj.ffmtest;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("ffm_multiplayer_test")
public final class MultiplayerTestMod {
    public MultiplayerTestMod() { NeoForge.EVENT_BUS.register(ServerHarness.class);NeoForge.EVENT_BUS.register(MousePerformanceHarness.class); }
}
