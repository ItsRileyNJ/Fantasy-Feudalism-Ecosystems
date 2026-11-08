package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/** Native mock-player semantics, with NeoForge channels initialized before mod login callbacks. */
public final class TestPlayers {
    private TestPlayers() {}
    public static ServerPlayer creative(GameTestHelper helper) {
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"test-mock-player"),false);
        var player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),cookie.gameProfile(),cookie.clientInformation()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return true; }
        };
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        return player;
    }
}
