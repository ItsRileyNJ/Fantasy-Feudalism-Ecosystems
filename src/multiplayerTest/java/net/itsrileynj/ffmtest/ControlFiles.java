package net.itsrileynj.ffmtest;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.itsrileynj.fantasyfeudalismmobs.*;

/** File control is local only. Gameplay interactions still use the real Minecraft connection. */
public final class ControlFiles {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static long completed = -1;
    private static EntityDataAccessor<List<ParticleOptions>> effectParticleData;

    public static Map<String,Object> particle(ParticleOptions option) {
        var info = new LinkedHashMap<String,Object>();
        info.put("type", BuiltInRegistries.PARTICLE_TYPE.getKey(option.getType()).toString());
        if (option instanceof ColorParticleOption color) {
            info.put("rgb", (Math.round(color.getRed()*255)<<16) | (Math.round(color.getGreen()*255)<<8) | Math.round(color.getBlue()*255));
            info.put("alpha", color.getAlpha());
        }
        return info;
    }

    // Named development runtime only; these diagnostics never enter the release JAR.
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> effectParticles(LivingEntity living) {
        try {
            if (effectParticleData == null) {
                var field = LivingEntity.class.getDeclaredField("DATA_EFFECT_PARTICLES");
                field.setAccessible(true);
                effectParticleData = (EntityDataAccessor<List<ParticleOptions>>) field.get(null);
            }
            return living.getEntityData().get(effectParticleData).stream().map(ControlFiles::particle).toList();
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    public static Path root() { return Path.of(System.getProperty("ffm.test.control", "/workspace/multiplayer/control/unused")); }
    public static JsonObject command() {
        Path file = root().resolve("command.json");
        if (!Files.exists(file)) return null;
        try {
            JsonObject request = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            long id = request.get("id").getAsLong();
            if (id <= completed) return null;
            completed = id;
            return request;
        } catch (Exception ignored) { return null; } // Atomic writer may not have committed the request yet.
    }
    public static void respond(JsonObject request, Object result) { write(request.get("id").getAsLong(), result, null); }
    public static void fail(JsonObject request, Throwable error) { write(request.get("id").getAsLong(), null, error.toString()); }
    private static void write(long id, Object result, String error) {
        try {
            Files.createDirectories(root()); JsonObject response = new JsonObject(); response.addProperty("id", id);
            response.add("result", GSON.toJsonTree(result)); if (error != null) response.addProperty("error", error);
            Path temp = root().resolve("response.tmp"); Files.writeString(temp, GSON.toJson(response), StandardCharsets.UTF_8);
            Files.move(temp, root().resolve("response.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception errorWriting) { throw new IllegalStateException(errorWriting); }
    }
    public static Map<String,Object> stack(ItemStack stack) {
        var info = new LinkedHashMap<String,Object>();
        info.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()); info.put("count", stack.getCount());
        info.put("damage", stack.getDamageValue());
        var bat=BatNetItem.captured(stack);
        if(bat!=null){info.put("batUUID",bat.getUUID("UUID").toString());info.put("batHealth",bat.getFloat("Health"));info.put("batFertilizerUntil",bat.getCompound("NeoForgeData").getLong(BatFarming.NEXT));}
        var potion = stack.get(DataComponents.POTION_CONTENTS);
        if (potion != null) info.put("potion", potion.potion().map(h -> h.getRegisteredName()).orElse("custom"));
        return info;
    }
    public static Map<String,Object> player(Player player) {
        var info = entity(player);
        info.put("name", player.getName().getString()); info.put("health", player.getHealth());
        info.put("food",player.getFoodData().getFoodLevel());info.put("saturation",player.getFoodData().getSaturationLevel());
        info.put("exhaustion",player.getFoodData().getExhaustionLevel());info.put("sleeping",player.isSleeping());info.put("sleepTimer",player.getSleepTimer());
        info.put("selected", player.getInventory().selected);
        var inventory = new ArrayList<Map<String,Object>>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) inventory.add(stack(player.getInventory().getItem(i)));
        info.put("inventory", inventory); info.put("cursor", stack(player.containerMenu.getCarried()));
        info.put("menu", menu(player.containerMenu));
        return info;
    }
    public static Map<String,Object> entity(Entity entity) {
        var info = new LinkedHashMap<String,Object>(); info.put("id", entity.getId()); info.put("uuid", entity.getUUID().toString());
        info.put("type", BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        info.put("position", List.of(entity.getX(),entity.getY(),entity.getZ()));
        info.put("pose",entity.getPose().name());info.put("onGround",entity.onGround());info.put("inWater",entity.isInWater());
        info.put("alive", entity.isAlive()); info.put("removed", entity.isRemoved());
        info.put("invisible",entity.isInvisible());info.put("silent",entity.isSilent());
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity item) info.put("item",stack(item.getItem()));
        if(entity instanceof LivingEntity living) {
            info.put("hurtTime",living.hurtTime);info.put("health",living.getHealth());info.put("maxHealth",living.getMaxHealth());
            info.put("effects",living.getActiveEffects().stream().map(e->e.getEffect().getRegisteredName()).toList());
            info.put("effectDetails",living.getActiveEffects().stream().map(e->Map.of(
                    "effect",e.getEffect().getRegisteredName(),"duration",e.getDuration(),"amplifier",e.getAmplifier(),
                    "visible",e.isVisible(),"ambient",e.isAmbient(),"particle",particle(e.getParticleOptions()))).toList());
            info.put("effectParticles",effectParticles(living));
        }
        if(entity instanceof net.minecraft.world.entity.animal.Cat cat){
            info.put("lying",cat.isLying());info.put("tamed",cat.isTame());info.put("owner",String.valueOf(cat.getOwnerUUID()));info.put("sitting",cat.isOrderedToSit());
        }
        if(entity instanceof FieldMouse mouse){info.put("nextMeal",mouse.nextMeal());info.put("cropHome",String.valueOf(mouse.cropHome()));}
        if(entity instanceof Mob mob) {
            info.put("spawnType",mob.getSpawnType()==null?"":mob.getSpawnType().name());
            info.put("target",mob.getTarget()==null?-1:mob.getTarget().getId());info.put("noAi",mob.isNoAi());
            info.put("mainHand",stack(mob.getMainHandItem()));info.put("offHand",stack(mob.getOffhandItem()));
            if(mob instanceof net.minecraft.world.entity.monster.WitherSkeleton)info.put("shieldDisabledUntil",mob.getPersistentData().getLong(WitherCombat.SHIELD_DISABLED));
            info.put("blocking",mob.isBlocking());info.put("usingItem",mob.isUsingItem());info.put("sprinting",mob.isSprinting());
        }
        if (entity instanceof net.minecraft.world.entity.animal.Wolf wolf) {
            info.put("tailAngle",wolf.getTailAngle());info.put("tamed",wolf.isTame());info.put("sitting",wolf.isInSittingPose());
            info.put("stalking",((WolfState)wolf).ffm$stalking());
            info.put("crouch",((WolfState)wolf).ffm$crouch(1));
            if(!wolf.level().isClientSide) { info.put("firstStalk",ServerHarness.firstStalk(wolf.getId()));info.put("pack",WolfPacks.pack(wolf).toString());info.put("mealUntil",WolfPacks.deadline(wolf)); }
        }
        if(entity instanceof net.minecraft.world.entity.animal.Fox fox && !fox.level().isClientSide) {
            info.put("mealUntil",GuardDogs.foxDeadline(fox));info.put("hungry",GuardDogs.foxHungry(fox));
        }
        if(entity instanceof net.minecraft.world.entity.npc.Villager villager) {
            info.put("baby",villager.isBaby());
            if(!villager.level().isClientSide) {
                var state=(VillagerPlayState)villager;info.put("playCat",state.ffm$playCat()==null?"":state.ffm$playCat().toString());info.put("playUntil",state.ffm$playUntil());
            }
        }
        if (entity instanceof net.minecraft.world.entity.animal.horse.Horse horse) {
            info.put("baby",horse.isBaby());info.put("tamed",horse.isTamed());
            info.put("grazing",horse.isEating());info.put("eatAnim",horse.getEatAnim(1));
            if (!horse.level().isClientSide) {
                info.put("age",horse.getAge());info.put("nextBreed",HorseLife.nextBreed(horse));
                info.put("herd",horse.getPersistentData().hasUUID("ffm_horse_herd") ? horse.getPersistentData().getUUID("ffm_horse_herd").toString() : "");
            }
        }
        if (entity instanceof net.minecraft.world.entity.monster.Silverfish fish && !entity.level().isClientSide) {
            info.put("mineshaft",FeudalEvents.inMineshaft((net.minecraft.server.level.ServerLevel)entity.level(),entity.blockPosition()));
            info.put("killedZombie",SilverfishBurrowing.hasKill(fish));
            info.put("burrowed",SilverfishBurrowing.isBurrowed(fish));
            info.put("burrowUntil",SilverfishBurrowing.deadline(fish));
        }
        if (entity instanceof net.minecraft.world.entity.monster.Spider spider) {
            info.put("owner", ((SpiderState)spider).ffm$owner().map(UUID::toString).orElse(""));
            info.put("sitting", SpiderCompanion.isSitting(spider)); info.put("health", spider.getHealth()); info.put("maxHealth", spider.getMaxHealth());
            info.put("speed", spider.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
            if (!spider.level().isClientSide) info.put("cooldowns", SpiderCompanion.data(spider).toString());
        }
        if(entity instanceof net.minecraft.world.entity.ambient.Bat bat){info.put("resting",bat.isResting());info.put("home",bat.getPersistentData().getCompound(BatResidents.HOME).toString());info.put("fertilizerUntil",bat.getPersistentData().getLong(BatFarming.NEXT));}
        if(entity instanceof net.minecraft.world.entity.monster.Phantom phantom){info.put("pactOwner",String.valueOf(PhantomPact.ownerId(phantom)));info.put("pact",phantom.getPersistentData().getCompound(PhantomPact.PACT).toString());}
        if(MercenaryHarness.isGuard(entity)) info.put("guard",MercenaryHarness.state(entity));
        return info;
    }
    public static Map<String,Object> block(net.minecraft.world.level.Level level,JsonObject q){
        var pos=new net.minecraft.core.BlockPos(q.get("x").getAsInt(),q.get("y").getAsInt(),q.get("z").getAsInt());var state=level.getBlockState(pos);
        var info=new LinkedHashMap<String,Object>();info.put("block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        var properties=new TreeMap<String,String>();state.getValues().forEach((key,value)->properties.put(key.getName(),value.toString()));info.put("properties",properties);
        if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container){var slots=new ArrayList<Map<String,Object>>();for(int i=0;i<container.getContainerSize();i++)slots.add(stack(container.getItem(i)));info.put("slots",slots);}
        return info;
    }
    public static Map<String,Object> enclosure(net.minecraft.world.level.Level level,JsonObject q){
        var pos=new net.minecraft.core.BlockPos(q.get("x").getAsInt(),q.get("y").getAsInt(),q.get("z").getAsInt());
        var home=(BatEnclosureBlockEntity)level.getBlockEntity(pos);
        return Map.of("residents",home.residentCount(),"inside",home.insideCount(),"ids",home.residentIds().stream().map(UUID::toString).toList(),"display",home.displayBats().size());
    }
    public static Map<String,Object> menu(AbstractContainerMenu menu) {
        var info=new LinkedHashMap<String,Object>();info.put("type",menu.getClass().getSimpleName());
        info.put("id",menu.containerId);info.put("state",menu.getStateId());
        info.put("slots",menu.slots.stream().map(s->stack(s.getItem())).toList());
        if(menu.getClass().getName().equals("dev.villagerscontract.GuardMenu")) {
            for(String key:List.of("health","maxHealth","hunger","order","mountState"))
                info.put(key,MercenaryHarness.call(menu,key,new Class<?>[0]));
        }
        return info;
    }
    private ControlFiles() {}
}
