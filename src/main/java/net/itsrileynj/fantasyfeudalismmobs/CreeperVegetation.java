package net.itsrileynj.fantasyfeudalismmobs;

import java.util.Map;
import net.minecraft.core.BlockPos;
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

    public static void convertExisting(Level level, Vec3 center, float radius) {
        if (!FeudalConfig.CREEPER_BLOCK_CONVERSION.get()) return;
        BlockPos origin = BlockPos.containing(center);
        int reach = (int)Math.ceil(radius);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-reach, -reach, -reach), origin.offset(reach, reach, reach))) {
            if (Vec3.atCenterOf(pos).distanceToSqr(center) > radius * radius || !level.hasChunkAt(pos)) continue;
            BlockState before = level.getBlockState(pos);
            // Never fill air, including cave air and void air. Only mossify an existing block.
            if (before.isAir()) continue;
            BlockState after = mossy(before);
            if (after != before) level.setBlock(pos, after, Block.UPDATE_ALL);
        }
    }
    private CreeperVegetation() {}
}
