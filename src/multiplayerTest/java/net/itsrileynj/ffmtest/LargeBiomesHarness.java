package net.itsrileynj.ffmtest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.ScorpionModule;

/** Disposable-world validation; never included in the release mod. */
final class LargeBiomesHarness {
    static Object audit(MinecraftServer server) {
        var level=server.overworld();
        var generator=level.getChunkSource().getGenerator();
        if(!(generator instanceof NoiseBasedChunkGenerator noise)||!noise.stable(NoiseGeneratorSettings.LARGE_BIOMES))
            throw new IllegalStateException("Expected an actual Large Biomes noise generator");
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var observations=new LinkedHashMap<String,Object>();
        var expected=new LinkedHashMap<String,List<String>>();
        for(var biome:List.of("forest","flower_forest","birch_forest","old_growth_birch_forest","dark_forest","cherry_grove"))expected.put(biome,List.of("robin","goldfinch","blue_jay"));
        for(var biome:List.of("taiga","snowy_taiga","old_growth_pine_taiga","old_growth_spruce_taiga"))expected.put(biome,List.of("crow"));
        for(var biome:List.of("savanna","savanna_plateau","windswept_savanna"))expected.put(biome,List.of("root_worm"));
        for(var biome:List.of("badlands","eroded_badlands","wooded_badlands"))expected.put(biome,List.of("scorpion"));
        for(var biome:List.of("dripstone_caves","lush_caves","deep_dark"))expected.put(biome,List.of());
        for(var entry:expected.entrySet()) {
            var biome=registry.get(ResourceLocation.withDefaultNamespace(entry.getKey()));
            var spawns=new ArrayList<Map<String,Object>>();
            var types=new ArrayList<>(entry.getValue());types.add("lava_scorpion");
            for(var name:types) {
                var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("fantasy_feudalism_mobs",name));
                var matches=biome.getMobSettings().getMobs(type.getCategory()).unwrap().stream().filter(s->s.type==type).toList();
                if(matches.size()!=1)throw new IllegalStateException(entry.getKey()+" / "+name+" spawn entries: "+matches.size());
                var spawn=matches.getFirst();
                spawns.add(Map.of("type",name,"weight",spawn.getWeight().asInt(),"min",spawn.minCount,"max",spawn.maxCount));
            }
            boolean geodes=biome.getGenerationSettings().features().stream().flatMap(s->s.stream()).anyMatch(f->f.unwrapKey().map(k->k.location().getPath().equals("amethyst_geode")).orElse(false));
            observations.put(entry.getKey(),Map.of("spawns",spawns,"geodeFeature",geodes));
        }
        if(!((Map<?,?>)observations.get("forest")).get("geodeFeature").equals(true))throw new IllegalStateException("Vanilla geode generation is missing");
        return Map.of("noiseSettings",noise.generatorSettings().unwrapKey().orElseThrow().location().toString(),"biomes",observations);
    }
    static Object spawn(MinecraftServer server) {
        var level=server.overworld();var origin=new BlockPos(136,20,136);
        for(int x=-20;x<20;x++)for(int z=-20;z<20;z++)for(int y=-1;y<=7;y++)
            level.setBlockAndUpdate(origin.offset(x,y,z),(y==-1?Blocks.AMETHYST_BLOCK:y==7?Blocks.DEEPSLATE:Blocks.AIR).defaultBlockState());
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"large-biomes-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(server,level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        server.getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(GameType.CREATIVE);
        player.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(origin.offset(40,0,0)));
        try {
            level.random.setSeed(6262L);int[] count={0};int attempts=0;
            for(;attempts<512&&count[0]==0;attempts++)NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER,level,level.getChunk(origin),origin,
                (type,pos,chunk)->type==ScorpionModule.CRYSTAPID.get(),(mob,chunk)->{count[0]++;mob.discard();});
            if(count[0]==0)throw new IllegalStateException("No native Crystapid spawns on cave amethyst in Large Biomes");
            return Map.of("spawned",count[0],"attempts",attempts,"biome",level.getBiome(origin).unwrapKey().orElseThrow().location().toString(),"position",List.of(136,20,136),"globalCapsIsolated",true);
        } finally {server.getPlayerList().remove(player);}
    }
}
