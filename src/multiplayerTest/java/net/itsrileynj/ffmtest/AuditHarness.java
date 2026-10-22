package net.itsrileynj.ffmtest;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Repeatable diagnostic fixtures only; never packaged in the released mod. */
public final class AuditHarness {
    public static Object fixture(MinecraftServer server,JsonObject q){
        var level=server.overworld();String kind=q.get("kind").getAsString();int count=q.get("count").getAsInt();
        var owner=server.getPlayerList().getPlayerByName("FFMProfiler");
        if(owner==null){
            var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.fromString("d57a0409-1ee8-49c0-a927-020000000016"),"FFMProfiler"),false);
            owner=new ServerPlayer(server,level,cookie.gameProfile(),cookie.clientInformation());
            var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
            server.getPlayerList().placeNewPlayer(connection,owner,cookie);
        }
        owner.setGameMode(GameType.CREATIVE);owner.setInvulnerable(true);owner.teleportTo(16,64,4);
        // Clean previous colonies before entities, so global UUID reservations are released.
        for(int x=-12;x<=40;x++)for(int z=-12;z<=28;z++){
            level.setBlock(new BlockPos(x,64,z),Blocks.AIR.defaultBlockState(),3);
            level.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
        }
        var entities=new ArrayList<Entity>();for(var e:level.getAllEntities())if(!(e instanceof ServerPlayer))entities.add(e);
        entities.forEach(Entity::discard);level.setDayTime(18000);
        for(int i=0;i<count;i++){
            if(kind.equals("bats")){
                var pos=new BlockPos(i%10*4-4,64,i/10*4-4);
                level.setBlock(pos,FantasyFeudalismMobs.BAT_ENCLOSURE.get().defaultBlockState(),3);
                var home=(BatEnclosureBlockEntity)level.getBlockEntity(pos);
                for(int n=0;n<3;n++){
                    Bat bat=EntityType.BAT.create(level);bat.getRandom().setSeed(i*3L+n);bat.moveTo(pos.getX()+.5,66,pos.getZ()+.5);level.addFreshEntity(bat);
                    var net=new ItemStack(FantasyFeudalismMobs.BAT_NET.get());owner.setItemInHand(InteractionHand.MAIN_HAND,net);
                    BatNetItem.capture(owner,InteractionHand.MAIN_HAND,bat);if(!home.insert(net))throw new IllegalStateException("Fixture bat insertion failed");
                }
            }else if(kind.equals("phantoms")){
                Phantom phantom=EntityType.PHANTOM.create(level);phantom.getRandom().setSeed(i);phantom.setPersistenceRequired();
                phantom.moveTo(4+i%12,67+i%3,-2+i/12);level.addFreshEntity(phantom);
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.ENDER_EYE));
                if(!PhantomPact.interact(owner,InteractionHand.MAIN_HAND,phantom).consumesAction())throw new IllegalStateException("Fixture pact failed");
            }else throw new IllegalArgumentException(kind);
        }
        if(kind.equals("phantoms"))for(int i=0;i<400;i++){
            var pig=EntityType.PIG.create(level);pig.setNoAi(true);pig.moveTo(-4+i%25,64,-4+i/25);level.addFreshEntity(pig);
        }
        return Map.of("kind",kind,"coloniesOrGuardians",count,"dayTime",level.getDayTime());
    }
    private AuditHarness(){}
}
