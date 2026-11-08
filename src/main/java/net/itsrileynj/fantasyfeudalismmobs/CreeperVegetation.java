package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public final class CreeperVegetation {
    private static final Map<Block, Block> MOSSY = Map.ofEntries(
            Map.entry(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE),
            Map.entry(Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS),
            Map.entry(Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB),
            Map.entry(Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL),
            Map.entry(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS),
            Map.entry(Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS),
            Map.entry(Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB),
            Map.entry(Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL),
            Map.entry(Blocks.INFESTED_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS));

    public static BlockState mossy(BlockState original) {
        Block replacement = MOSSY.get(original.getBlock());
        if (replacement == null) return original;
        BlockState result = replacement.defaultBlockState();
        for (Property<?> property : original.getProperties()) result = copyProperty(original, result, property);
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    /** Only exposed terrain can support new, single-block vegetation. Never use plants as supports. */
    public static boolean suitableGround(Level level,BlockPos pos) {
        BlockState state=level.getBlockState(pos);
        if(!state.getFluidState().isEmpty()||state.is(Blocks.FARMLAND)||state.is(BlockTags.CROPS)||state.hasBlockEntity())return false;
        boolean terrain=state.is(BlockTags.DIRT)||state.is(BlockTags.BASE_STONE_OVERWORLD)
                ||state.is(Blocks.MOSS_BLOCK)||state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(Blocks.GRAVEL)
                ||state.is(Blocks.COBBLESTONE)||state.is(Blocks.MOSSY_COBBLESTONE)
                ||state.is(Blocks.STONE_BRICKS)||state.is(Blocks.MOSSY_STONE_BRICKS);
        return terrain&&state.isFaceSturdy(level,pos,Direction.UP);
    }
    private static void placeSurfacePlant(Level level,BlockPos pos) {
        if(!level.getBlockState(pos).is(Blocks.AIR)||!level.getFluidState(pos).isEmpty()
                ||!level.hasChunkAt(pos.below())||!suitableGround(level,pos.below())||level.random.nextFloat()>=.75F)return;
        BlockState carpet=Blocks.MOSS_CARPET.defaultBlockState(),fern=Blocks.FERN.defaultBlockState(),grass=Blocks.SHORT_GRASS.defaultBlockState();
        boolean c=carpet.canSurvive(level,pos),f=fern.canSurvive(level,pos),g=grass.canSurvive(level,pos);
        int total=(c?50:0)+(f?35:0)+(g?15:0);
        if(total==0)return;
        int roll=level.random.nextInt(total);
        BlockState plant=c&&roll<50?carpet:f&&roll<(c?50:0)+35?fern:grass;
        level.setBlock(pos,plant,Block.UPDATE_ALL);
    }

    public static void affectExisting(Level level, Vec3 center, float radius) {
        boolean mossConversion=FeudalConfig.CREEPER_BLOCK_CONVERSION.get();
        BlockPos origin = BlockPos.containing(center);
        int reach = (int)Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(center) > radius * radius || !level.hasChunkAt(pos)) continue;
            BlockState before = level.getBlockState(pos);
            // A separate pass may decorate exposed normal air; block conversion and crop growth never fill it.
            if (before.isAir()) continue;
            if (CropGrowth.advanceExisting(level, pos, 2)) continue;
            if (!mossConversion) continue;
            BlockState after = mossy(before);
            if (after != before) level.setBlock(pos, after, Block.UPDATE_ALL);
        }
        // Separate pass avoids decorating a new plant or relying on iteration order after moss conversion.
        for(BlockPos pos:BlockPos.betweenClosed(origin.offset(-reach,-reach,-reach),origin.offset(reach,reach,reach))) {
            if(Vec3.atCenterOf(pos).distanceToSqr(center)<=radius*radius&&level.hasChunkAt(pos))placeSurfacePlant(level,pos);
        }
    }
    private CreeperVegetation() {}
}
