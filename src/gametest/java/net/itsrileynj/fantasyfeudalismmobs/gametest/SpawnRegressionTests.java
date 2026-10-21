package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.List;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.*;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnRegressionTests {
    @GameTest(template = "test_empty")
    public static void selectedModifiedSpawnEntriesSurviveRevalidation(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(8, 2, 8));
        var pool = WeightedRandomList.create(List.of(
                new MobSpawnSettings.SpawnerData(EntityType.SPIDER, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.CAVE_SPIDER, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.CREEPER, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.ZOMBIE, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.HUSK, 100, 4, 4),
                new MobSpawnSettings.SpawnerData(EntityType.GHAST, 100, 4, 4)));
        var selected = EventHooks.getPotentialSpawns(h.getLevel(), MobCategory.MONSTER, pos, pool);
        var validated = EventHooks.getPotentialSpawns(h.getLevel(), MobCategory.MONSTER, pos, pool);
        for (var entry : selected.unwrap()) h.assertTrue(validated.unwrap().contains(entry),
                "Minecraft's identity-based spawn validation must accept " + entry.type);
        h.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100, batch = "ffm_natural_spawning")
    public static void naturalSpawnerActuallyCreatesMineshaftSilverfish(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(8, 2, 8));
        for (int x = 0; x < 18; x++) for (int z = 0; z < 18; z++) {
            h.setBlock(x, 1, z, Blocks.STONE); h.setBlock(x, 5, z, Blocks.STONE);
            for (int y = 2; y < 5; y++) h.setBlock(x, y, z, x == 0 || x == 17 || z == 0 || z == 17 ? Blocks.STONE : Blocks.AIR);
        }
        var structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.withDefaultNamespace("mineshaft"));
        var room = new MineshaftPieces.MineShaftRoom(0, RandomSource.create(11), pos.getX() - 4, pos.getZ() - 4, MineshaftStructure.Type.NORMAL);
        room.move(0, pos.getY() - room.getBoundingBox().minY(), 0);
        var origin = new ChunkPos(pos); var chunk = level.getChunkAt(pos);
        var start = new StructureStart(structure, origin, 0, new PiecesContainer(List.of(room)));
        level.structureManager().setStartForStructure(SectionPos.of(pos), structure, start, chunk);
        var box = room.getBoundingBox();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var at = new BlockPos(x * 16, pos.getY(), z * 16);
            level.structureManager().addReferenceForStructure(SectionPos.of(at), structure, origin.toLong(), level.getChunkAt(at));
        }
        var observer = h.makeMockServerPlayerInLevel(); observer.moveTo(pos.getX() + 40, pos.getY(), pos.getZ());
        level.setBlock(observer.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
        h.runAfterDelay(10, () -> {
            int[] fish = {0};
            try {
                h.assertTrue(FeudalEvents.inMineshaft(level, pos), "Natural-spawn fixture is a registered mineshaft room");
                for (int attempt = 0; attempt < 120 && fish[0] < 4; attempt++) {
                    level.random.setSeed(20000 + attempt);
                    NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, chunk, pos,
                            (type, at, candidateChunk) -> true, (mob, spawnedChunk) -> {
                                if (mob instanceof Silverfish) fish[0]++;
                                mob.discard();
                            });
                }
                h.assertTrue(fish[0] > 0, "The actual natural spawner must create silverfish, got " + fish[0]);
            } finally { level.getServer().getPlayerList().remove(observer); }
            h.succeed();
        });
    }
}
