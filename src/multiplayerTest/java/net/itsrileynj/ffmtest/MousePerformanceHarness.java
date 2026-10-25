package net.itsrileynj.ffmtest;

import com.google.gson.JsonObject;
import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

/** Identical local, server-only workloads for the old and optimized release JARs. Never packaged. */
public final class MousePerformanceHarness {
    private static long griefChecks;
    private static BlockPos villageHome;
    @SubscribeEvent public static void grief(EntityMobGriefingEvent event){if(event.getEntity() instanceof FieldMouse)griefChecks++;}
    public static Map<String,Object> metrics(MinecraftServer server){
        var data=new LinkedHashMap<String,Object>(ServerHarness.timings());data.put("tick",server.getTickCount());
        data.put("gameTime",server.overworld().getGameTime());data.put("griefChecks",griefChecks);
        int mice=0;for(Entity e:server.overworld().getAllEntities())if(e instanceof FieldMouse&&e.isAlive())mice++;
        data.put("livingMice",mice);return data;
    }
    public static Object fixture(MinecraftServer server,JsonObject request){
        ServerLevel level=server.overworld();String scenario=request.get("scenario").getAsString();
        if(scenario.equals("reset")){griefChecks=0;return true;}
        if(scenario.equals("morning")){
            long day=Math.max(MouseVillages.get(level).lastMorning()+1,1000);level.setDayTime(day*24000);
            long started=System.nanoTime();int spawned=MouseVillages.get(level).morning(level);
            return Map.of("elapsedMs",(System.nanoTime()-started)/1e6,"spawned",spawned,"day",day);
        }
        var old=new ArrayList<Entity>();for(Entity e:level.getAllEntities())if(!(e instanceof net.minecraft.server.level.ServerPlayer))old.add(e);old.forEach(Entity::discard);
        if(villageHome!=null){level.getPoiManager().remove(villageHome);villageHome=null;}
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(!scenario.equals("blocked"),server);
        level.setDayTime(6000);level.setWeatherParameters(100000,0,false,false);
        for(int cx=-1;cx<=6;cx++)for(int cz=-1;cz<=4;cz++){level.setChunkForced(cx,cz,true);level.getChunk(cx,cz);}
        for(int x=-4;x<100;x++)for(int z=-4;z<66;z++){
            level.setBlock(new BlockPos(x,63,z),Blocks.GRASS_BLOCK.defaultBlockState(),3);
            for(int y=64;y<=69;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        }
        int count=request.has("count")?request.get("count").getAsInt():200;
        if(scenario.equals("village")){
            villageHome=new BlockPos(40,64,20);
            level.setBlock(villageHome,Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD),3);
            level.setBlock(villageHome.south(),Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.FOOT),3);
            level.getPoiManager().add(villageHome,level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE).getHolderOrThrow(PoiTypes.HOME));
            level.getPoiManager().take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(villageHome),villageHome,1);
            for(int i=0;i<96;i++){
                var cat=EntityType.CAT.create(level);cat.setNoAi(true);cat.setPersistenceRequired();cat.moveTo(36+i%12*.6,64,12+i/12*.6);level.addFreshEntity(cat);
            }
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,server);griefChecks=0;
            return Map.of("cats",96,"village",level.isVillage(villageHome));
        }
        for(int i=0;i<count;i++){
            int x=2+i%20*4,z=2+i/20*4;BlockPos crop=new BlockPos(x+1,64,z);
            if(!scenario.equals("barren")){
                level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),3);
                level.setBlock(crop,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7),3);
            }
            var mouse=FantasyFeudalismMobs.FIELD_MOUSE.get().create(level);mouse.moveTo(x+.5,64,z+.5);
            mouse.getRandom().setSeed(1000+i);if(!scenario.equals("barren"))mouse.setCropHome(crop);level.addFreshEntity(mouse);
            if(scenario.equals("threats")&&i%10==0){var villager=EntityType.VILLAGER.create(level);villager.moveTo(x+2.5,64,z+.5);villager.setNoAi(true);level.addFreshEntity(villager);}
        }
        griefChecks=0;return Map.of("scenario",scenario,"mice",count);
    }
}
