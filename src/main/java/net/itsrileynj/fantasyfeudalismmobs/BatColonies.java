package net.itsrileynj.fantasyfeudalismmobs;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Cross-dimension resident claims survive unloaded homes and prevent duplicate enclosure occupants. */
public final class BatColonies extends SavedData {
    private static final SavedData.Factory<BatColonies> FACTORY=new SavedData.Factory<>(BatColonies::new,BatColonies::load);
    public record Home(String dimension, BlockPos pos) {
        public static Home at(ServerLevel level, BlockPos pos) {
            return new Home(level.dimension().location().toString(),pos.immutable());
        }
    }
    private final Map<UUID,Home> homes=new HashMap<>();
    public static BatColonies get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                FACTORY,"ffm_bat_colonies");
    }
    public boolean claim(UUID bat,Home home) {
        if(homes.containsKey(bat))return false;
        homes.put(bat,home);setDirty();return true;
    }
    public boolean owns(UUID bat,Home home){return home.equals(homes.get(bat));}
    public Home home(UUID bat){return homes.get(bat);}
    public void release(UUID bat){if(homes.remove(bat)!=null)setDirty();}
    private static BatColonies load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new BatColonies();
        for(var entry:tag.getList("Residents",Tag.TAG_COMPOUND)){
            var row=(CompoundTag)entry;
            if(row.hasUUID("Bat")&&ResourceLocation.tryParse(row.getString("Dimension"))!=null)
                data.homes.put(row.getUUID("Bat"),new Home(row.getString("Dimension"),BlockPos.of(row.getLong("Pos"))));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();homes.forEach((bat,home)->{
            var row=new CompoundTag();row.putUUID("Bat",bat);row.putString("Dimension",home.dimension());row.putLong("Pos",home.pos().asLong());list.add(row);
        });tag.put("Residents",list);return tag;
    }
}
