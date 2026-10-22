package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FrogWaterEscapeTests {
    private static Frog fixture(GameTestHelper h,int pond) {
        for(int x=0;x<24;x++)for(int z=0;z<16;z++){h.setBlock(x,0,z,Blocks.STONE);h.setBlock(x,1,z,Blocks.GRASS_BLOCK);}
        if(pond>0)for(int x=pond;x<pond+4;x++)for(int z=6;z<11;z++)h.setBlock(x,1,z,Blocks.WATER);
        var frog=h.spawn(EntityType.FROG,4,2,8);
        frog.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(frog.blockPosition()),MobSpawnType.NATURAL,null);
        frog.getBrain().setMemory(MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS,10000);
        frog.setOnGround(true);return frog;
    }
    private static void escape(GameTestHelper h,int pond) {
        var frog=fixture(h,pond);double startX=frog.getX(),startY=frog.getY();
        var player=h.makeMockServerPlayerInLevel();player.moveTo(h.absoluteVec(new Vec3(2,2,8)));
        boolean[] jumped={false},rose={false},walked={false};
        h.onEachTick(()->{
            if(frog.getPose()==Pose.LONG_JUMPING)jumped[0]=true;
            if(frog.getY()>startY+.15)rose[0]=true;
            if(frog.onGround()&&frog.getX()>startX+1)walked[0]=true;
        });
        h.succeedWhen(()->{
            h.assertTrue(jumped[0]&&rose[0],"Real world ticks launch the frog with an airborne jumping pose");
            h.assertTrue(frog.isInWaterOrBubble()&&frog.getPose()==Pose.STANDING,"The frog reaches water and restores its normal pose");
            if(pond>10)h.assertTrue(walked[0],"Distant water requires walking closer before the jump");
        });
    }
    @GameTest(template="test_empty",timeoutTicks=260,batch="ffm_frog_near")
    public static void approachingPlayerMakesFrogJumpIntoNearbyWater(GameTestHelper h){escape(h,8);}
    @GameTest(template="test_empty",timeoutTicks=300,batch="ffm_frog_far")
    public static void frogWalksCloserThenJumpsIntoDistantWater(GameTestHelper h){escape(h,15);}
    @GameTest(template="test_empty",timeoutTicks=120,batch="ffm_frog_fallback")
    public static void noWaterAndDistantPlayersLeaveNormalAiAvailable(GameTestHelper h){
        var frog=fixture(h,0);frog.setNoAi(true);
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ffm-frog-observer"),false);
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.moveTo(h.absoluteVec(new Vec3(2,2,8)));
        var escape=new FrogWaterEscape();
        h.assertTrue(FrogWaterEscape.threat(frog)==player,"A nearby player is detected");
        h.assertFalse(escape.tick(frog),"No water leaves the native brain available");
        player.setGameMode(GameType.SPECTATOR);
        h.assertTrue(FrogWaterEscape.threat(frog)==null,"Spectators do not trigger escape");
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(FrogWaterEscape.threat(frog)==player,"Creative players also approach frogs");
        player.moveTo(h.absoluteVec(new Vec3(18,2,8)));
        h.assertTrue(FrogWaterEscape.threat(frog)==null,"Distant players do not trigger an escape");
        h.assertTrue(frog.getPose()==Pose.STANDING,"Fallback does not leave a jumping pose");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_frog_protected")
    public static void breedingAndCoveredWaterDoNotInterruptNativeAi(GameTestHelper h){
        var frog=fixture(h,8);frog.setNoAi(true);
        var player=h.makeMockServerPlayerInLevel();player.moveTo(h.absoluteVec(new Vec3(2,2,8)));
        frog.setInLove(player);
        h.assertFalse(new FrogWaterEscape().tick(frog),"Love mode keeps native breeding AI available");
        frog.resetLove();frog.getBrain().setMemory(MemoryModuleType.IS_PREGNANT,net.minecraft.util.Unit.INSTANCE);
        h.assertFalse(new FrogWaterEscape().tick(frog),"Egg laying takes precedence over player escape");
        frog.getBrain().eraseMemory(MemoryModuleType.IS_PREGNANT);
        for(int x=8;x<12;x++)for(int z=6;z<11;z++)h.setBlock(x,2,z,Blocks.STONE);
        h.assertFalse(new FrogWaterEscape().tick(frog),"Water under a solid roof is rejected without freezing the brain");
        h.assertTrue(frog.getPose()==Pose.STANDING,"Protected and blocked frogs retain a normal pose");h.succeed();
    }
    @AfterBatch(batch="ffm_frog_protected") public static void cleanupProtected(ServerLevel level){cleanup(level);}
    @AfterBatch(batch="ffm_frog_near") public static void cleanupNear(ServerLevel level){cleanup(level);}
    @AfterBatch(batch="ffm_frog_far") public static void cleanupFar(ServerLevel level){cleanup(level);}
    @AfterBatch(batch="ffm_frog_fallback") public static void cleanupFallback(ServerLevel level){cleanup(level);}
    private static void cleanup(ServerLevel level){
        for(var player:List.copyOf(level.players()))if((player.getGameProfile().getName().equals("test-mock-player")||player.getGameProfile().getName().equals("ffm-frog-observer")))
            level.getServer().getPlayerList().remove(player);
    }
}
