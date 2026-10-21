package net.itsrileynj.fantasyfeudalismmobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Saved pack meals cover unloaded members; transient hunts retain UUIDs only. */
public final class WolfPackData extends SavedData {
    private final Map<UUID,Long> meals = new HashMap<>();
    final Map<String,Hunt> hunts = new HashMap<>();
    private long nextCleanup;

    static final class Hunt {
        final UUID prey;
        boolean charging;
        long touched;
        Hunt(UUID prey, long now) { this.prey=prey;this.touched=now; }
    }

    public static WolfPackData get(ServerLevel level) {
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WolfPackData::new,WolfPackData::load),
                "fantasy_feudalism_wolf_packs");
        data.cleanup(level.getGameTime());return data;
    }

    public long deadline(UUID pack) { return meals.getOrDefault(pack,0L); }
    public void feed(UUID pack,long until) { meals.put(pack,until);hunts.keySet().removeIf(key->key.endsWith("/"+pack));setDirty(); }

    private void cleanup(long now) {
        if(now<nextCleanup)return;nextCleanup=now+1200;
        if(meals.entrySet().removeIf(e->e.getValue()<=now))setDirty();
        hunts.entrySet().removeIf(e->e.getValue().touched+200<now);
    }

    public static WolfPackData load(CompoundTag tag,HolderLookup.Provider provider) {
        var data=new WolfPackData();
        for(var entry:tag.getList("Meals",10)) {
            var meal=(CompoundTag)entry;
            if(meal.hasUUID("Pack"))data.meals.put(meal.getUUID("Pack"),meal.getLong("Until"));
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider) {
        var list=new ListTag();
        meals.forEach((pack,until)->{var meal=new CompoundTag();meal.putUUID("Pack",pack);meal.putLong("Until",until);list.add(meal);});
        tag.put("Meals",list);return tag;
    }
}
