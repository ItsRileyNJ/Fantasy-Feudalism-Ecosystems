package net.itsrileynj.ffmtest;

import com.google.gson.*;
import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class ServerHarness {
    private static final Map<Integer,Long> HORSE_FIRST_GRAZE=new HashMap<>();
    private static final Map<Integer,Integer> HORSE_GRAZES=new HashMap<>();
    private static final Set<Integer> HORSE_EATING=new HashSet<>();
    private static final Map<Integer,Long> HORSE_BIRTHS=new HashMap<>();
    private static long tickStart;
    private static final Map<Integer,Long> WOLF_STARTS=new HashMap<>();
    public static long firstStalk(int id){return WOLF_STARTS.getOrDefault(id,-1L);}
    private static final ArrayDeque<Double> TIMES = new ArrayDeque<>();
    private static final ArrayDeque<Map<String,Object>> NATURAL_SILVERFISH = new ArrayDeque<>();
    private static final ArrayDeque<Map<String,Object>> SILVERFISH_KILLS = new ArrayDeque<>();
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        if(event.getEntity() instanceof Zombie zombie && event.getSource().getEntity() instanceof Silverfish fish
                && !fish.level().isClientSide) {
            SILVERFISH_KILLS.add(Map.of("killer",fish.getId(),"zombie",zombie.getUUID().toString(),"gameTime",fish.level().getGameTime()));
            if(SILVERFISH_KILLS.size()>1000)SILVERFISH_KILLS.remove();
        }
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if(!event.getLevel().isClientSide && !event.loadedFromDisk() && event.getEntity() instanceof net.minecraft.world.entity.animal.horse.Horse horse
                && horse.isBaby())HORSE_BIRTHS.put(horse.getId(),horse.level().getGameTime());
        if (!event.getLevel().isClientSide && !event.loadedFromDisk()
                && event.getEntity() instanceof Silverfish fish && fish.getSpawnType()==MobSpawnType.NATURAL) {
            NATURAL_SILVERFISH.add(Map.of("tick",fish.getServer().getTickCount(),"entity",ControlFiles.entity(fish)));
            if (NATURAL_SILVERFISH.size()>1000) NATURAL_SILVERFISH.remove();
        }
    }
    @SubscribeEvent public static void pre(ServerTickEvent.Pre event) { tickStart = System.nanoTime(); }
    @SubscribeEvent public static void post(ServerTickEvent.Post event) {
        TIMES.add((System.nanoTime() - tickStart) / 1_000_000.0); if (TIMES.size() > 1200) TIMES.remove();
        if (System.getProperty("ffm.test.control") == null) return;
        for(var entity:event.getServer().overworld().getAllEntities())
            if(entity instanceof net.minecraft.world.entity.animal.Wolf wolf && ((net.itsrileynj.fantasyfeudalismmobs.WolfState)wolf).ffm$stalking())
                WOLF_STARTS.putIfAbsent(wolf.getId(),wolf.level().getGameTime());
        for(var entity:event.getServer().overworld().getAllEntities())
            if(entity instanceof net.minecraft.world.entity.animal.horse.Horse horse) {
                if(horse.isEating()) {
                    if(HORSE_EATING.add(horse.getId())) {
                        HORSE_FIRST_GRAZE.putIfAbsent(horse.getId(),horse.level().getGameTime());
                        HORSE_GRAZES.merge(horse.getId(),1,Integer::sum);
                    }
                } else HORSE_EATING.remove(horse.getId());
            }
        JsonObject request = ControlFiles.command(); if (request == null) return;
        try { ControlFiles.respond(request, execute(event.getServer(), request)); }
        catch (Throwable error) { error.printStackTrace(); ControlFiles.fail(request, error); }
    }
    private static ServerPlayer player(MinecraftServer server, JsonObject q) {
        ServerPlayer p = server.getPlayerList().getPlayerByName(q.get("player").getAsString());
        if (p == null) throw new IllegalArgumentException("Player not connected"); return p;
    }
    private static Entity entity(ServerLevel level, JsonObject q) {
        Entity entity = level.getEntity(q.get("entity").getAsInt());
        if (entity == null) throw new IllegalArgumentException("Entity is not loaded"); return entity;
    }
    private static double number(JsonObject q, String key, double fallback) { return q.has(key) ? q.get(key).getAsDouble() : fallback; }
    public static Object execute(MinecraftServer server, JsonObject q) {
        ServerLevel level = server.overworld(); String action = q.get("action").getAsString();
        switch (action) {
            case "mouseProfile": {var prepared=MousePerformanceHarness.fixture(server,q);TIMES.clear();return prepared;}
            case "mouseProfileMetrics": return MousePerformanceHarness.metrics(server);
            case "auditFixture": return AuditHarness.fixture(server,q);
            case "block": return ControlFiles.block(level,q);
            case "enclosure": return ControlFiles.enclosure(level,q);
            case "command": server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),q.get("command").getAsString()); return true;
            case "mineshaft": {
                var found=level.findNearestMapStructure(net.minecraft.tags.StructureTags.MINESHAFT,new BlockPos(0,64,0),100,false);
                if(found==null)throw new IllegalStateException("No generated mineshaft found");
                level.getChunkAt(found);
                var starts=level.structureManager().startsForStructure(new ChunkPos(found),s->s instanceof net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure);
                var pieces=new ArrayList<Map<String,Object>>();
                for(var start:starts)for(var piece:start.getPieces()) {
                    var b=piece.getBoundingBox();
                    pieces.add(Map.of("min",List.of(b.minX(),b.minY(),b.minZ()),"max",List.of(b.maxX(),b.maxY(),b.maxZ())));
                }
                return Map.of("location",List.of(found.getX(),found.getY(),found.getZ()),"pieces",pieces);
            }
            case "mercenary": return MercenaryHarness.execute(server,q);
            case "snapshot": return snapshot(server);
            case "advancementState": {
                var p=player(server,q);var progress=new TreeMap<String,Object>();
                var names=new TreeSet<>(EcosystemAchievements.GOALS);names.add("master_naturalist");
                for(var name:names){var holder=server.getAdvancements().get(EcosystemAchievements.id(name));
                    if(holder==null)throw new IllegalStateException("Missing advancement: "+name);
                    var status=p.getAdvancements().getOrStartProgress(holder);
                    progress.put(name,Map.of("done",status.isDone(),"remaining",status.getRemainingCriteria(),"completed",status.getCompletedCriteria()));
                }return progress;
            }
            case "naturalSilverfish": return List.copyOf(NATURAL_SILVERFISH);
            case "horseObservation": return Map.of("firstGrazes",Map.copyOf(HORSE_FIRST_GRAZE),"grazes",Map.copyOf(HORSE_GRAZES),"births",Map.copyOf(HORSE_BIRTHS));
            case "pasture": {
                int grass=0,dirt=0;
                for(int x=0;x<32;x++)for(int z=0;z<32;z++) {
                    var block=level.getBlockState(new BlockPos(x,63,z));
                    if(block.is(Blocks.GRASS_BLOCK))grass++;
                    if(block.is(Blocks.DIRT))dirt++;
                }
                return Map.of("grass",grass,"dirt",dirt);
            }
            case "silverfishKills": return List.copyOf(SILVERFISH_KILLS);
            case "zombiePosition": {
                LivingEntity hunter=(LivingEntity)entity(level,q);
                Zombie probe=EntityType.ZOMBIE.create(level);
                BlockPos origin=hunter.blockPosition();
                for(int radius=1;radius<=6;radius++)for(int dy:new int[]{0,1,-1})
                    for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
                        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                        BlockPos at=origin.offset(dx,dy,dz);
                        if(!level.getBlockState(at.below()).isFaceSturdy(level,at.below(),Direction.UP))continue;
                        probe.moveTo(at.getX()+0.5,at.getY(),at.getZ()+0.5);
                        if(level.noCollision(probe)&&hunter.hasLineOfSight(probe))
                            return Map.of("x",probe.getX(),"y",probe.getY(),"z",probe.getZ());
                    }
                throw new IllegalStateException("No unobstructed visible zombie position near the hunter");
            }
            case "setup":
                level.setDayTime(18000); level.setWeatherParameters(1000000, 0, false, false);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
                level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
                for (int x=-12;x<=40;x++) for (int z=-12;z<=20;z++) {
                    level.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
                    for(int y=64;y<=70;y++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
                }
                return true;
            case "teleport": {
                ServerPlayer p=player(server,q);p.teleportTo(level,number(q,"x",0),number(q,"y",64),number(q,"z",0),Set.of(),
                        (float)number(q,"yaw",0),(float)number(q,"pitch",0));return true;
            }
            case "inventory": {
                ServerPlayer p=player(server,q); p.getInventory().clearContent(); p.containerMenu.setCarried(ItemStack.EMPTY);
                if(q.has("fill")) for(int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
                if(q.has("slots")) for(JsonElement e:q.getAsJsonArray("slots")) {
                    JsonObject s=e.getAsJsonObject(); Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(s.get("item").getAsString()));
                    p.getInventory().setItem(s.get("slot").getAsInt(),new ItemStack(item,s.has("count")?s.get("count").getAsInt():1));
                }
                p.containerMenu.broadcastChanges(); p.inventoryMenu.broadcastChanges(); return true;
            }
            case "wolfPack": {
                net.minecraft.world.entity.SpawnGroupData group=null;var pack=new ArrayList<Map<String,Object>>();
                for(int i=0;i<q.get("count").getAsInt();i++) {
                    var wolf=EntityType.WOLF.create(level);wolf.moveTo(number(q,"x",2),number(q,"y",64),number(q,"z",2)+i*2,0,0);
                    group=wolf.finalizeSpawn(level,level.getCurrentDifficultyAt(wolf.blockPosition()),net.minecraft.world.entity.MobSpawnType.NATURAL,group);
                    wolf.setAge(0);wolf.setPersistenceRequired();level.addFreshEntity(wolf);pack.add(ControlFiles.entity(wolf));
                }
                return pack;
            }
            case "villagePlay": {
                var pos=new BlockPos(20,64,4);
                level.setBlock(pos.south(),Blocks.WHITE_BED.defaultBlockState(),2);
                level.setBlock(pos,Blocks.WHITE_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD),3);
                var manager=level.getPoiManager();manager.add(pos,level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.POINT_OF_INTEREST_TYPE)
                        .getHolderOrThrow(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME));
                manager.take(type->type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),(type,at)->at.equals(pos),pos,1);
                var cat=EntityType.CAT.create(level);cat.moveTo(23,64,8);cat.setPersistenceRequired();level.addFreshEntity(cat);
                var child=EntityType.VILLAGER.create(level);child.moveTo(17,64,8);child.setBaby(true);child.setPersistenceRequired();level.addFreshEntity(child);
                var brain=child.getBrain();brain.setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.PLAY);
                brain.eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
                var play=new net.itsrileynj.fantasyfeudalismmobs.BabyCatPlay();
                brain.addActivity(net.minecraft.world.entity.schedule.Activity.PLAY,com.google.common.collect.ImmutableList.of(com.mojang.datafixers.util.Pair.of(1,play)));
                long seed=0;while(RandomSource.create(seed).nextInt(12)!=0)seed++;child.getRandom().setSeed(seed);
                if(!play.tryStart(level,child,level.getGameTime()))throw new IllegalStateException("Village play fixture did not start");
                return Map.of("child",ControlFiles.entity(child),"cat",ControlFiles.entity(cat));
            }
            case "horseHerd": {
                net.minecraft.world.entity.SpawnGroupData group=null;
                var herd=new ArrayList<Map<String,Object>>();
                for(int i=0;i<q.get("count").getAsInt();i++) {
                    var horse=EntityType.HORSE.create(level);
                    horse.moveTo(number(q,"x",2)+i*2,number(q,"y",64),number(q,"z",2),0,0);
                    group=horse.finalizeSpawn(level,level.getCurrentDifficultyAt(horse.blockPosition()),net.minecraft.world.entity.MobSpawnType.NATURAL,group);
                    horse.setAge(0);horse.setPersistenceRequired();level.addFreshEntity(horse);
                    herd.add(ControlFiles.entity(horse));
                }
                return herd;
            }
            case "food": {
                var p=server.getPlayerList().getPlayerByName(q.get("player").getAsString());
                p.getFoodData().setFoodLevel(q.get("level").getAsInt());
                p.getFoodData().setSaturation((float)number(q,"saturation",0));return true;
            }
            case "spawn": {
                EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(q.get("type").getAsString()));
                Entity e=type.create(level); if(e==null)throw new IllegalArgumentException("Cannot create entity");
                if (e instanceof LivingEntity living && q.has("maxHealth")) {
                    living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(q.get("maxHealth").getAsDouble());
                    living.setHealth(living.getMaxHealth());
                }
                e.moveTo(number(q,"x",2),number(q,"y",64),number(q,"z",2),0,0);
                if(e instanceof Mob mob) {mob.setPersistenceRequired();mob.setNoAi(!q.has("ai")||!q.get("ai").getAsBoolean());}
                if(q.has("invulnerable"))e.setInvulnerable(q.get("invulnerable").getAsBoolean());
                if(e instanceof net.minecraft.world.entity.animal.Fox fox && q.has("ai")&&q.get("ai").getAsBoolean())
                    fox.finalizeSpawn(level,level.getCurrentDifficultyAt(fox.blockPosition()),MobSpawnType.COMMAND,null);
                if(e instanceof net.minecraft.world.entity.animal.frog.Frog frog && q.has("ai")&&q.get("ai").getAsBoolean())
                    frog.finalizeSpawn(level,level.getCurrentDifficultyAt(frog.blockPosition()),MobSpawnType.COMMAND,null);
                level.addFreshEntity(e);
                if(MercenaryHarness.isGuard(e)&&q.has("owner")) {
                    var owner=server.getPlayerList().getPlayerByName(q.get("owner").getAsString());
                    MercenaryHarness.call(e,"bind",new Class<?>[]{BlockPos.class,UUID.class,BlockPos.class},null,owner.getUUID(),e.blockPosition());
                    ((Mob)e).setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));
                    MercenaryHarness.call(e,"initializeMercenary",new Class<?>[]{boolean.class},false);
                }
                if(e instanceof net.minecraft.world.entity.animal.Wolf wolf && q.has("owner")) {
                    var p=server.getPlayerList().getPlayerByName(q.get("owner").getAsString());
                    wolf.setOwnerUUID(p.getUUID());wolf.setTame(true,true);
                }
                if(e instanceof net.minecraft.world.entity.animal.Cat cat&&q.has("owner")){
                    var owner=server.getPlayerList().getPlayerByName(q.get("owner").getAsString());
                    cat.setOwnerUUID(owner.getUUID());cat.setTame(true,true);cat.setOrderedToSit(false);
                }
                if(e instanceof Spider spider && q.has("owner")) {
                    ServerPlayer p=server.getPlayerList().getPlayerByName(q.get("owner").getAsString());
                    SpiderCompanion.tame(spider,p.getUUID());SpiderCompanion.setSitting(spider,false);
                }
                if(e instanceof AbstractSkeleton skeleton && q.has("weapon")) skeleton.setItemSlot(EquipmentSlot.MAINHAND,
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.get("weapon").getAsString()))));
                if(e instanceof Mob mob && q.has("shield"))mob.setItemSlot(EquipmentSlot.OFFHAND,q.get("shield").getAsBoolean()?new ItemStack(Items.SHIELD):ItemStack.EMPTY);
                if(e instanceof Mob mob && q.has("seed")) mob.getRandom().setSeed(q.get("seed").getAsLong());
                return ControlFiles.entity(e);
            }
            case "seedTame": {
                Mob mob=(Mob)entity(level,q);long seed=0;while(RandomSource.create(seed).nextInt(100)>=33)seed++;mob.getRandom().setSeed(seed);return seed;
            }
            case "resetHarvest": {
                Spider spider=(Spider)entity(level,q);SpiderCompanion.data(spider).putLong(SpiderCompanion.SILK_READY,0);
                SpiderCompanion.data(spider).putLong(SpiderCompanion.POISON_READY,0);return true;
            }
            case "health": { ((LivingEntity)entity(level,q)).setHealth((float)number(q,"health",0)); return true; }
            case "hit": {
                Mob attacker=(Mob)entity(level,q);
                Entity victim=level.getEntity(q.get("target").getAsInt());
                if (victim==null) throw new IllegalArgumentException("Victim is not loaded");
                boolean hit=attacker.doHurtTarget(victim);
                return Map.of("successful",hit,"target",ControlFiles.entity(victim));
            }
            case "remove": { entity(level,q).discard();return true; }
            case "target": {
                Mob mob=(Mob)entity(level,q);mob.setNoAi(false);mob.setTarget((LivingEntity)level.getEntity(q.get("target").getAsInt()));return true;
            }
            case "save": {server.saveEverything(false,true,true);return snapshot(server);}
            case "invulnerable": {for(ServerPlayer p:server.getPlayerList().getPlayers())p.setInvulnerable(q.get("value").getAsBoolean());return true;}
            case "clear": {
                List<Entity> entities=new ArrayList<>();for(Entity e:level.getAllEntities())if(!(e instanceof ServerPlayer)
                        && (!q.has("except") || !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(q.get("except").getAsString())))entities.add(e);
                for(Entity e:entities)e.discard();return entities.size();
            }
            case "resetTimings": TIMES.clear();return true;
            case "stress": {
                int count=q.get("count").getAsInt(); EntityType<?>[] types={EntityType.SPIDER,EntityType.CAVE_SPIDER,EntityType.SKELETON,
                    EntityType.BOGGED,EntityType.STRAY,EntityType.ZOMBIE,EntityType.HUSK,EntityType.SILVERFISH,EntityType.BLAZE};
                ServerPlayer owner=server.getPlayerList().getPlayers().getFirst();
                for(int i=0;i<count;i++) {
                    Mob mob=(Mob)types[i%types.length].create(level);mob.setPersistenceRequired();
                    mob.moveTo(-8+i%30,64,-8+(i/30)%25,0,0);level.addFreshEntity(mob);
                    if(mob instanceof Spider spider) {SpiderCompanion.tame(spider,owner.getUUID());SpiderCompanion.setSitting(spider,false);}
                }
                TIMES.clear();return count;
            }
            default:throw new IllegalArgumentException("Unknown action "+action);
        }
    }
    private static Object snapshot(MinecraftServer server) {
        var result=new LinkedHashMap<String,Object>();result.put("tick",server.getTickCount());
        result.put("gameTime",server.overworld().getGameTime());
        result.put("players",server.getPlayerList().getPlayers().stream().map(ControlFiles::player).toList());
        var entities=new ArrayList<Map<String,Object>>();
        for(Entity e:server.overworld().getAllEntities()) {
            if(e instanceof ServerPlayer)continue;
            var info=ControlFiles.entity(e);if(e instanceof net.minecraft.world.entity.item.ItemEntity item)info.put("stack",ControlFiles.stack(item.getItem()));entities.add(info);
        }
        result.put("entities",entities);result.putAll(timings());return result;
    }
    public static Map<String,Object> timings(){
        var sorted=TIMES.stream().sorted().toList();var data=new LinkedHashMap<String,Object>();
        data.put("tickSamples",sorted.size());data.put("meanTickMs",sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0));
        for(int percentile:new int[]{50,95,99})data.put("p"+percentile+"TickMs",sorted.isEmpty()?0:sorted.get(Math.min(sorted.size()-1,(int)(sorted.size()*percentile/100.0))));
        data.put("maxTickMs",sorted.isEmpty()?0:sorted.getLast());data.put("ticksOver50Ms",sorted.stream().filter(ms->ms>50).count());return data;
    }
}
