package net.itsrileynj.fantasyfeudalismmobs;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BatEnclosureBlock extends BaseEntityBlock {
    public static final MapCodec<BatEnclosureBlock> CODEC=simpleCodec(BatEnclosureBlock::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Shapes.block();
    public BatEnclosureBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BatEnclosureBlockEntity(pos,state);}
    @Override @Nullable public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,FantasyFeudalismMobs.BAT_ENCLOSURE_ENTITY.get(),BatEnclosureBlockEntity::serverTick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(stack.getItem() instanceof BatNetItem&&BatNetItem.captured(stack)!=null){
            if(player.isSpectator())return ItemInteractionResult.FAIL;
            if(level.isClientSide)return ItemInteractionResult.SUCCESS;
            if(level.getBlockEntity(pos) instanceof BatEnclosureBlockEntity home&&home.insert(stack))return ItemInteractionResult.CONSUME;
            return ItemInteractionResult.FAIL;
        }
        if(stack.is(net.minecraft.world.item.Items.APPLE)){
            if(player.isSpectator())return ItemInteractionResult.FAIL;
            if(level.isClientSide)return ItemInteractionResult.SUCCESS;
            return level.getBlockEntity(pos) instanceof BatEnclosureBlockEntity home&&home.feed(player,hand)?ItemInteractionResult.CONSUME:ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&level.getBlockEntity(pos) instanceof BatEnclosureBlockEntity home&&!level.isClientSide)home.breakHome();
        super.onRemove(state,level,pos,replacement,moving);
    }
}
