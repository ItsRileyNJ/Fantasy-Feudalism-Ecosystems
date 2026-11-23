package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RootWormIntegrationTests {
    @GameTest(template="test_empty")
    public static void rootWormSpawnEggCreatesTheRegisteredMultipartMob(GameTestHelper h) {
        h.setBlock(8,1,8,Blocks.GRASS_BLOCK);var player=h.makeMockPlayer(GameType.CREATIVE);player.getAbilities().instabuild=true;
        var egg=new ItemStack(RootWormModule.ROOT_WORM_EGG.get());player.setItemInHand(InteractionHand.MAIN_HAND,egg);
        var pos=h.absolutePos(new BlockPos(8,1,8));var hit=new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false);
        h.assertTrue(egg.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Root Worm egg uses native item interaction");
        var worms=h.getLevel().getEntitiesOfClass(net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm.class,new AABB(pos).inflate(3),worm->true);
        h.assertTrue(worms.size()==1,"The egg produces exactly one parent entity");var worm=worms.getFirst();
        h.assertTrue(worm.getParts().length==9&&worm.getMaxHealth()==16,"The spawned mob retains nine parts and eight hearts");h.succeed();
    }
    @GameTest(template="test_empty",batch="rootworm_biomes")
    public static void rootWormNaturalEntriesCoverAllSavannasOnly(GameTestHelper h) {
        for(var biome:java.util.List.of(Biomes.SAVANNA,Biomes.SAVANNA_PLATEAU,Biomes.WINDSWEPT_SAVANNA,Biomes.FOREST,Biomes.PLAINS,Biomes.DESERT)) {
            h.setBiome(biome);var entries=h.getLevel().getBiome(h.absolutePos(new BlockPos(8,2,8))).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
            boolean found=entries.stream().anyMatch(e->e.type==RootWormModule.ROOT_WORM.get()&&e.minCount==2&&e.maxCount==4&&e.getWeight().asInt()==6);
            boolean savanna=biome==Biomes.SAVANNA||biome==Biomes.SAVANNA_PLATEAU||biome==Biomes.WINDSWEPT_SAVANNA;
            h.assertTrue(found==savanna,"Natural entries are present in each savanna variant and absent from unrelated biomes");
        }
        h.succeed();
    }
}
