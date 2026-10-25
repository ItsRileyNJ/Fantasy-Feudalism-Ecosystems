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
    private long lastMorning=Long.MIN_VALUE;
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
        if(event.getLevel() instanceof ServerLevel level&&!level.players().isEmpty())get(level).morning(level);
    }
    public int morning(ServerLevel level){
        long time=level.getDayTime(),day=Math.floorDiv(time,24000);
        // A short dawn window also catches time skipping through normal player sleep.
        if(Math.floorMod(time,24000)>=1000||day<=lastMorning)return 0;
        lastMorning=day;setDirty();
        if(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return 0;
        var groups=new ArrayList<Village>();
        for(Entity entity:level.getAllEntities())if(entity instanceof Cat cat&&cat.isAlive()&&level.isVillage(cat.blockPosition())){
            var home=level.getPoiManager().findClosest(p->p.is(PoiTypes.HOME),cat.blockPosition(),48,PoiManager.Occupancy.IS_OCCUPIED).orElse(null);
            if(home==null)continue;
            Village match=null;
            for(var group:new ArrayList<>(groups))if(group.homes.stream().anyMatch(pos->pos.distSqr(home)<=96*96)){
                if(match==null)match=group;
                else{match.homes.addAll(group.homes);match.cats+=group.cats;groups.remove(group);}
            }
            if(match==null){match=new Village();groups.add(match);}
            if(!match.homes.contains(home))match.homes.add(home);match.cats++;
        }
        int spawned=0;
        for(var village:groups){
            int missing=quota(village.cats)-population(village.homes);
            // Each attempt is bounded, even in command-created villages with thousands of cats.
            for(int i=0;i<Math.min(missing,32);i++)if(spawn(level,village.homes.getFirst()))spawned++;
        }
        return spawned;
    }
    private static final class Village {final List<BlockPos> homes=new ArrayList<>();int cats;}
    private boolean spawn(ServerLevel level,BlockPos home){
        // Search loaded crop plots once per attempt; never generate chunks to create wildlife.
        BlockPos crop=null;
        for(int i=0;i<256;i++){
            var at=home.offset(level.random.nextInt(65)-32,level.random.nextInt(9)-4,level.random.nextInt(65)-32);
            if(level.hasChunkAt(at)&&FieldMouse.cropAge(level.getBlockState(at))!=null){crop=at;break;}
        }
        // Thin farms should not depend on random sampling a single planted block.
        if(crop==null)outer:for(BlockPos at:BlockPos.betweenClosed(home.offset(-16,-3,-16),home.offset(16,3,16))){
            if(level.hasChunkAt(at)&&FieldMouse.cropAge(level.getBlockState(at))!=null){crop=at.immutable();break outer;}
        }
        if(crop==null)return false;
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
    private static MouseVillages load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new MouseVillages();
        if(tag.contains("LastMorning"))data.lastMorning=tag.getLong("LastMorning");
        for(Tag entry:tag.getList("Mice",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)entry;if(row.hasUUID("Id"))data.residents.put(row.getUUID("Id"),BlockPos.of(row.getLong("Home")));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.putLong("LastMorning",lastMorning);var list=new ListTag();
        residents.forEach((id,pos)->{var row=new CompoundTag();row.putUUID("Id",id);row.putLong("Home",pos.asLong());list.add(row);});
        tag.put("Mice",list);return tag;
    }
}
