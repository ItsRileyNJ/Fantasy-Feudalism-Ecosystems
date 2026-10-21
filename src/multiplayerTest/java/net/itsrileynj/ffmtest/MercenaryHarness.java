package net.itsrileynj.ffmtest;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.*;

/** Optional observation/setup only. Actual menu and contract interactions come from real clients. */
public final class MercenaryHarness {
    public static Object call(Object receiver,String name,Class<?>[] types,Object...args) {
        try{return receiver.getClass().getMethod(name,types).invoke(receiver,args);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public static boolean isGuard(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(ResourceLocation.parse("villagerscontract:defender"));
    }
    private static List<Map<String,Object>> items(Container container) {
        var result=new ArrayList<Map<String,Object>>();for(int i=0;i<container.getContainerSize();i++)result.add(ControlFiles.stack(container.getItem(i)));return result;
    }
    public static Map<String,Object> state(Entity guard) {
        var result=new LinkedHashMap<String,Object>();
        for(String name:List.of("needsContract","isGuardCaptain","hunger","skin"))result.put(name,call(guard,name,new Class<?>[0]));
        if(!guard.level().isClientSide) {
            result.put("order",call(guard,"order",new Class<?>[0]).toString());
            result.put("supplies",items((Container)call(guard,"supplies",new Class<?>[0])));
            result.put("weapons",items((Container)call(guard,"weapons",new Class<?>[0])));
            Object owner=call(guard,"redeemer",new Class<?>[0]);result.put("redeemer",owner==null?"":owner.toString());
        }
        return result;
    }
    public static Object execute(MinecraftServer server,JsonObject q) {
        var level=server.overworld();String op=q.get("op").getAsString();
        if(op.equals("patrol")) {
            var result=new ArrayList<Map<String,Object>>();int count=q.has("count")?q.get("count").getAsInt():2;
            for(int i=0;i<count;i++) {
                Pillager mob=EntityType.PILLAGER.create(level);mob.setNoAi(true);mob.setPersistenceRequired();
                mob.moveTo(2+i*1.5,64,1,0,0);mob.setPatrolTarget(new BlockPos(12,64,12));
                if(i==0) {mob.setPatrolLeader(true);mob.setItemSlot(EquipmentSlot.HEAD,Raid.getLeaderBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));}
                mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));level.addFreshEntity(mob);result.add(ControlFiles.entity(mob));
            }
            return result;
        }
        if(op.equals("op")){var p=server.getPlayerList().getPlayerByName(q.get("player").getAsString());server.getPlayerList().op(p.getGameProfile());return true;}
        Entity guard=level.getEntity(q.get("entity").getAsInt());if(guard==null)throw new IllegalArgumentException("Entity not loaded");
        switch(op) {
            case "classify": {
                Entity other=level.getEntity(q.get("other").getAsInt());
                return Map.of("hostile",call(guard,"isHostile",new Class<?>[]{LivingEntity.class},other),
                        "allied",guard.isAlliedTo(other),"canAttack",((Mob)guard).canAttack((LivingEntity)other));
            }
            case "supplies": {
                Container supplies=(Container)call(guard,"supplies",new Class<?>[0]);supplies.clearContent();
                for(var element:q.getAsJsonArray("slots")) {
                    var slot=element.getAsJsonObject();supplies.setItem(slot.get("slot").getAsInt(),new ItemStack(
                        BuiltInRegistries.ITEM.get(ResourceLocation.parse(slot.get("item").getAsString())),slot.get("count").getAsInt()));
                }
                return state(guard);
            }
            case "ai":((Mob)guard).setNoAi(!q.get("value").getAsBoolean());return true;
            default:throw new IllegalArgumentException("Unknown mercenary operation "+op);
        }
    }
    private MercenaryHarness() {}
}
