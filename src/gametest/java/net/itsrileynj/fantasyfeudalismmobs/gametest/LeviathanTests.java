package net.itsrileynj.fantasyfeudalismmobs.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.Leviathan;
import net.itsrileynj.fantasyfeudalismmobs.Leviathans;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LeviathanTests {
    @GameTest(template = "test_empty", batch = "leviathan_hunt", timeoutTicks = 380)
    public static void leviathanHuntPausesLungesBitesAndStartsAgain(GameTestHelper h) {
        for (int x = 1; x < 39; x++) for (int z = 1; z < 39; z++) {
            h.setBlock(x, 1, z, Blocks.STONE);
            for (int y = 2; y <= 8; y++) h.setBlock(x, y, z, Blocks.WATER);
        }
        var level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "LeviathanPrey"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.setNoGravity(true);
        Vec3 playerPos = h.absoluteVec(new Vec3(29.5, 4, 19.5));
        player.teleportTo(playerPos.x, playerPos.y, playerPos.z);
        var fish = h.spawn(Leviathans.ENTITY.get(), 10, 4, 19);
        var seen = new HuntObservation();
        h.succeedWhen(() -> {
            if (fish.getTarget() == player) seen.acquired = true;
            if (fish.huntPhase() == Leviathan.STARE) {
                if (seen.bit) seen.secondStare = true;
                else {
                    seen.stareTicks++;
                    seen.maximumStareMotion = Math.max(seen.maximumStareMotion, fish.getDeltaMovement().length());
                    var direction = player.position().subtract(fish.position()).multiply(1, 0, 1).normalize();
                    if (seen.stareTicks >= 6) seen.facing = Vec3.directionFromRotation(0, fish.getYRot()).dot(direction);
                }
            }
            if (fish.huntPhase() == Leviathan.LUNGE) seen.maximumLungeSpeed = Math.max(seen.maximumLungeSpeed, fish.getDeltaMovement().length());
            if (fish.huntPhase() == Leviathan.SNAP) seen.bit = true;
            h.assertTrue(seen.acquired && seen.secondStare,
                    "The fish should find a submerged player, bite, then begin a second hunt. Phase "
                            + fish.huntPhase() + ", acquired " + seen.acquired + ", stare ticks " + seen.stareTicks
                            + ", bite " + seen.bit + ", fish " + fish.position() + " (water " + fish.isInWater()
                            + "), prey " + player.position() + " (water " + player.isInWater() + ")");
            h.assertTrue(seen.stareTicks >= 19 && seen.maximumStareMotion < .05 && seen.facing > .95,
                    "The fish faces the player without drifting for about one second");
            h.assertTrue(seen.maximumLungeSpeed > .95 && player.getHealth() > 0 && player.getHealth() < 12,
                    "The fish accelerates and lands a single bite before the next lunge");
        });
    }

    @AfterBatch(batch = "leviathan_hunt")
    public static void removePlayer(ServerLevel level) {
        var players = level.getServer().getPlayerList();
        var player = players.getPlayerByName("LeviathanPrey");
        if (player != null) players.remove(player);
    }

    private static final class HuntObservation {
        boolean acquired;
        boolean bit;
        boolean secondStare;
        int stareTicks;
        double facing;
        double maximumStareMotion;
        double maximumLungeSpeed;
    }
}
