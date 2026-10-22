package net.itsrileynj.fantasyfeudalismmobs;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

/** The same non-stackable net carries the original bat NBT; capture never creates a second entity. */
public final class BatNetItem extends Item {
    public static final String CAPTURED="ffm_captured_bat";
    public BatNetItem(Properties properties){super(properties.stacksTo(1));}
    public static CompoundTag captured(ItemStack stack){
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        return data.contains(CAPTURED,CompoundTag.TAG_COMPOUND)?data.getCompound(CAPTURED).copy():null;
    }
    public static void clear(ItemStack stack){
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.remove(CAPTURED));
    }
    public static InteractionResult capture(Player player,InteractionHand hand,Bat bat){
        var stack=player.getItemInHand(hand);
        if(!(stack.getItem() instanceof BatNetItem)||player.isSpectator()||!bat.isAlive()||bat.isRemoved())return InteractionResult.PASS;
        if(captured(stack)!=null)return InteractionResult.FAIL;
        if(player.level().isClientSide)return InteractionResult.SUCCESS;
        var level=(ServerLevel)player.level();
        // A riding bat cannot be silently separated from another entity.
        if(bat.isPassenger()||bat.isVehicle()||bat.isLeashed())return InteractionResult.FAIL;
        var saved=bat.saveWithoutId(new CompoundTag());
        saved.getCompound("NeoForgeData").remove(BatResidents.HOME);
        saved.getCompound("ForgeData").remove(BatResidents.HOME);
        BatColonies.get(level).release(bat.getUUID());
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->tag.put(CAPTURED,saved));
        bat.discard();return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){
        tooltip.add(Component.translatable(captured(stack)==null?"tooltip.fantasy_feudalism_mobs.bat_net.empty":"tooltip.fantasy_feudalism_mobs.bat_net.occupied"));
    }
}
