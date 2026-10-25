package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Action-driven native advancements: no extra entity polling or world scans. */
public final class EcosystemAchievements {
    public static final double MAX_SKELETON_RANGE=35;
    public static final double RANGE_TOLERANCE=.0001;
    private static final String SHOT="ffm_max_range_shot",FULL_HIT="ffm_full_health_enderman_hit";
    public static final Set<String> GOALS=Set.of("bottled_buzz","friendly_fangs","eight_legged_alliance","silk_supplier",
            "venom_on_tap","night_collector","full_house","natural_helpers","cat_nap","pest_control","night_pact",
            "watch_dog","new_arrival","shield_breaker","thats_where_id_be","everyones_got_a_plan_until");
    public static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"ecosystems/"+name);}
    public static void grant(Player player,String name){criterion(player,name,"earned");}
    private static void criterion(Player player,String name,String criterion){
        if(!(player instanceof ServerPlayer serverPlayer))return;
        var advancement=serverPlayer.server.getAdvancements().get(id(name));
        if(advancement!=null)serverPlayer.getAdvancements().award(advancement,criterion);
    }
    public static void owner(Entity entity,UUID owner,String name){
        if(owner!=null&&entity.getServer()!=null){var player=entity.getServer().getPlayerList().getPlayer(owner);if(player!=null)grant(player,name);}
    }
    public static void spiderTamed(Spider spider,UUID owner){
        if(spider.getServer()==null)return;
        var player=spider.getServer().getPlayerList().getPlayer(owner);if(player==null)return;
        grant(player,"friendly_fangs");criterion(player,"eight_legged_alliance",spider instanceof CaveSpider?"cave":"regular");
    }
    @SubscribeEvent public static void earned(AdvancementEvent.AdvancementEarnEvent event){
        var advancement=event.getAdvancement().id();
        if(!advancement.getNamespace().equals(FantasyFeudalismMobs.MOD_ID)||!advancement.getPath().startsWith("ecosystems/"))return;
        String name=advancement.getPath().substring("ecosystems/".length());
        if(GOALS.contains(name))criterion(event.getEntity(),"master_naturalist",name);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        for(String name:GOALS){
            var advancement=player.server.getAdvancements().get(id(name));
            if(advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone())criterion(player,"master_naturalist",name);
        }
    }
    public static boolean atMaximumRange(double distance){return distance>=MAX_SKELETON_RANGE-RANGE_TOLERANCE&&distance<=MAX_SKELETON_RANGE+1e-8;}
    public static void arrowFired(AbstractArrow arrow,AbstractSkeleton skeleton){
        if(!(skeleton.getTarget() instanceof ServerPlayer player)||arrow.getWeaponItem()==null
                ||!(arrow.getWeaponItem().getItem() instanceof BowItem||arrow.getWeaponItem().getItem() instanceof CrossbowItem))return;
        double distance=skeleton.distanceTo(player);if(!atMaximumRange(distance))return;
        var shot=new CompoundTag();shot.putUUID("Shooter",skeleton.getUUID());shot.putUUID("Target",player.getUUID());shot.putDouble("Distance",distance);
        arrow.getPersistentData().put(SHOT,shot);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void beforeDamage(LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        player.getPersistentData().remove(FULL_HIT);
        if(event.getSource().getDirectEntity() instanceof EnderMan enderman&&event.getNewDamage()>0
                &&player.getHealth()>=player.getMaxHealth()-.0001F){
            var hit=new CompoundTag();hit.putUUID("Attacker",enderman.getUUID());hit.putLong("Tick",player.level().getGameTime());
            player.getPersistentData().put(FULL_HIT,hit);
        }
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(player.getHealth()>0)player.getPersistentData().remove(FULL_HIT);
        if(event.getNewDamage()<=0||!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)||!arrow.getPersistentData().contains(SHOT))return;
        var shot=arrow.getPersistentData().getCompound(SHOT);
        if(!shot.hasUUID("Shooter")||!shot.hasUUID("Target")||!shot.getUUID("Target").equals(player.getUUID())||!atMaximumRange(shot.getDouble("Distance")))return;
        var shooter=arrow.getOwner();
        if(shooter!=null&&(!(shooter instanceof AbstractSkeleton)||!shooter.getUUID().equals(shot.getUUID("Shooter"))))return;
        grant(player,"thats_where_id_be");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(event.getEntity() instanceof FieldMouse&&event.getSource().getDirectEntity() instanceof Cat cat&&cat.isTame())owner(cat,cat.getOwnerUUID(),"pest_control");
        if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getSource().getDirectEntity() instanceof EnderMan enderman))return;
        var hit=player.getPersistentData().getCompound(FULL_HIT);player.getPersistentData().remove(FULL_HIT);
        if(hit.hasUUID("Attacker")&&hit.getUUID("Attacker").equals(enderman.getUUID())&&hit.getLong("Tick")==player.level().getGameTime())grant(player,"everyones_got_a_plan_until");
    }
    private EcosystemAchievements(){}
}
