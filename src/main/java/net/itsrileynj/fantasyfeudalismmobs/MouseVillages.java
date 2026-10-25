package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** One bounded census each morning. Saved reservations include unloaded mice and survive restart. */
public final class MouseVillages extends SavedData {
    private static final Factory<MouseVillages> FACTORY=new Factory<>(MouseVillages::new,MouseVillages::load);
    private final Map<UUID,BlockPos> residents=new HashMap<>();
    private final ArrayDeque<PendingVillage> pending=new ArrayDeque<>();
    private long lastMorning=Long.MIN_VALUE;
    private long lastWorkTick=Long.MIN_VALUE;
    public static MouseVillages get(ServerLevel level){return level.getDataStorage().computeIfAbsent(FACTORY,"ffm_field_mice");}
    public static int quota(int cats){return Math.max(0,cats)/3;}
    public long lastMorning(){return lastMorning;}
    public int population(List<BlockPos> homes){return (int)residents.values().stream().filter(pos->
            homes.stream().anyMatch(home->home.distSqr(pos)<=96*96)).count();}
    public void reserve(FieldMouse mouse){
        if(mouse.villageHome()!=null&&!mouse.villageHome().equals(residents.put(mouse.getUUID(),mouse.villageHome())))setDirty();
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof FieldMouse mouse)get(level).reserve(mouse);
    }
    @SubscribeEvent public static void left(EntityLeaveLevelEvent event){
        if(event.getLevel() instanceof ServerLevel level&&event.getEntity() instanceof FieldMouse mouse){
            Entity.RemovalReason reason=mouse.getRemovalReason();
            if(reason==Entity.RemovalReason.KILLED||reason==Entity.RemovalReason.DISCARDED||reason==Entity.RemovalReason.CHANGED_DIMENSION){
                var data=get(level);if(data.residents.remove(mouse.getUUID())!=null)data.setDirty();
            }
        }
    }
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(event.getLevel() instanceof ServerLevel level){
            var data=get(level);if(!level.players().isEmpty()||!data.pending.isEmpty())data.morning(level);
        }
    }
    public int morning(ServerLevel level){
        long time=level.getDayTime(),day=Math.floorDiv(time,24000);
        // A short dawn window also catches time skipping through normal player sleep.
        if(Math.floorMod(time,24000)>=1000){if(!pending.isEmpty()){pending.clear();setDirty();}return 0;}
        if(day<lastMorning)return 0;
        if(day>lastMorning){
            pending.clear();lastMorning=day;setDirty();
            if(level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))plan(level);
        }
        if(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)){
            if(!pending.isEmpty()){pending.clear();setDirty();}return 0;
        }
        if(pending.isEmpty()||lastWorkTick==level.getGameTime())return 0;
        lastWorkTick=level.getGameTime();
        var job=pending.getFirst();
        int cats=0;
        for(UUID id:job.cats)if(level.getEntity(id) instanceof Cat cat&&cat.isAlive()&&level.isVillage(cat.blockPosition())
                &&job.homes.stream().anyMatch(pos->pos.distSqr(cat.blockPosition())<=96*96))cats++;
        int limit=quota(cats),existing=population(job.homes);
        if(existing>=limit){pending.removeFirst();setDirty();return 0;}
        // A village finds its crop only once, even if several mice are missing.
        if(job.crop==null)job.crop=findCrop(level,job.homes.getFirst());
        if(job.crop==null||!level.hasChunkAt(job.crop)||FieldMouse.cropAge(level.getBlockState(job.crop))==null){
            pending.removeFirst();setDirty();return 0;
        }
        boolean spawned=spawn(level,job.homes.getFirst(),job.crop);
        if(--job.attempts<=0||spawned&&existing+1>=limit)pending.removeFirst();
        setDirty();return spawned?1:0;
    }
    private void plan(ServerLevel level){
        var groups=new ArrayList<Village>();
        for(Entity entity:level.getAllEntities())if(entity instanceof Cat cat&&cat.isAlive()&&level.isVillage(cat.blockPosition())){
            var home=level.getPoiManager().findClosest(p->p.is(PoiTypes.HOME),cat.blockPosition(),48,PoiManager.Occupancy.IS_OCCUPIED).orElse(null);
            if(home==null)continue;
            Village match=null;
            for(var group:new ArrayList<>(groups))if(group.homes.stream().anyMatch(pos->pos.distSqr(home)<=96*96)){
                if(match==null)match=group;
                else{match.homes.addAll(group.homes);match.cats.addAll(group.cats);groups.remove(group);}
            }
            if(match==null){match=new Village();groups.add(match);}
            if(!match.homes.contains(home))match.homes.add(home);match.cats.add(cat.getUUID());
        }
        for(var village:groups){
            int missing=quota(village.cats.size())-population(village.homes);
            if(missing>0)pending.add(new PendingVillage(village.homes,village.cats,Math.min(missing,32)));
        }
    }
    private static final class Village {final List<BlockPos> homes=new ArrayList<>();final List<UUID> cats=new ArrayList<>();}
    private static final class PendingVillage {
        final List<BlockPos> homes;final List<UUID> cats;int attempts;BlockPos crop;
        PendingVillage(List<BlockPos> homes,List<UUID> cats,int attempts){this.homes=List.copyOf(homes);this.cats=List.copyOf(cats);this.attempts=attempts;}
    }
    private BlockPos findCrop(ServerLevel level,BlockPos home){
        // At most one loaded crop search and one spawn per level per tick.
        BlockPos crop=null;
        for(int i=0;i<256;i++){
            var at=home.offset(level.random.nextInt(65)-32,level.random.nextInt(9)-4,level.random.nextInt(65)-32);
            if(level.hasChunkAt(at)&&FieldMouse.cropAge(level.getBlockState(at))!=null){crop=at;break;}
        }
        // Thin farms should not depend on random sampling a single planted block.
        if(crop==null)outer:for(BlockPos at:BlockPos.betweenClosed(home.offset(-16,-3,-16),home.offset(16,3,16))){
            if(level.hasChunkAt(at)&&FieldMouse.cropAge(level.getBlockState(at))!=null){crop=at.immutable();break outer;}
        }
        return crop;
    }
    private boolean spawn(ServerLevel level,BlockPos home,BlockPos crop){
        for(int i=0;i<16;i++){
            BlockPos at=crop.offset(level.random.nextInt(5)-2,0,level.random.nextInt(5)-2);
            if(!level.hasChunkAt(at)||!level.getFluidState(at).isEmpty())continue;
            var surface=level.getBlockState(at.below()).getCollisionShape(level,at.below());
            if(surface.isEmpty())continue;
            FieldMouse mouse=FantasyFeudalismMobs.FIELD_MOUSE.get().create(level);
            // Farmland is 15/16 high; use its real surface instead of requiring a full cube.
            mouse.moveTo(at.getX()+.5,at.getY()-1+surface.max(net.minecraft.core.Direction.Axis.Y),at.getZ()+.5,level.random.nextFloat()*360,0);
            if(!level.noCollision(mouse)||!level.getWorldBorder().isWithinBounds(at))continue;
            mouse.setVillageHome(home);mouse.setCropHome(crop);
            mouse.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.NATURAL,null);
            if(level.addFreshEntity(mouse)){reserve(mouse);return true;}
        }
        return false;
    }
    public static MouseVillages load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new MouseVillages();
        if(tag.contains("LastMorning"))data.lastMorning=tag.getLong("LastMorning");
        for(Tag entry:tag.getList("Mice",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)entry;if(row.hasUUID("Id"))data.residents.put(row.getUUID("Id"),BlockPos.of(row.getLong("Home")));
        }
        for(Tag entry:tag.getList("Pending",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)entry;var homes=new ArrayList<BlockPos>();var cats=new ArrayList<UUID>();
            for(Tag pos:row.getList("Homes",Tag.TAG_LONG))homes.add(BlockPos.of(((LongTag)pos).getAsLong()));
            for(Tag id:row.getList("Cats",Tag.TAG_INT_ARRAY))cats.add(NbtUtils.loadUUID(id));
            if(!homes.isEmpty()&&cats.size()>=3&&row.getInt("Attempts")>0){
                var job=new PendingVillage(homes,cats,Math.min(row.getInt("Attempts"),32));
                if(row.contains("Crop"))job.crop=BlockPos.of(row.getLong("Crop"));data.pending.add(job);
            }
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.putLong("LastMorning",lastMorning);var list=new ListTag();
        residents.forEach((id,pos)->{var row=new CompoundTag();row.putUUID("Id",id);row.putLong("Home",pos.asLong());list.add(row);});
        tag.put("Mice",list);var jobs=new ListTag();
        for(var job:pending){
            var row=new CompoundTag();var homes=new ListTag();var cats=new ListTag();
            job.homes.forEach(pos->homes.add(LongTag.valueOf(pos.asLong())));job.cats.forEach(id->cats.add(NbtUtils.createUUID(id)));
            row.put("Homes",homes);row.put("Cats",cats);row.putInt("Attempts",job.attempts);
            if(job.crop!=null)row.putLong("Crop",job.crop.asLong());jobs.add(row);
        }
        tag.put("Pending",jobs);return tag;
    }
}
