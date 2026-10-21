package net.itsrileynj.fantasyfeudalismmobs.mixin;

import java.util.*;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** GuardMenu hides the player inventory: vanilla menu resync cannot undo its local prediction. */
@Pseudo
@Mixin(targets="dev.villagerscontract.GuardMenu", remap=false)
public abstract class MercenaryMenuMixin {
    @Unique private List<ItemStack> ffm$beforePrivateInventory;

    @Inject(method="clicked",at=@At("HEAD"),remap=false)
    private void ffm$rememberPrivateInventory(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        ffm$beforePrivateInventory=null;
        if (player instanceof ServerPlayer && (type==ClickType.QUICK_MOVE || type==ClickType.SWAP)) {
            ffm$beforePrivateInventory=new ArrayList<>();
            for(int index=0;index<player.getInventory().getContainerSize();index++)
                ffm$beforePrivateInventory.add(player.getInventory().getItem(index).copy());
        }
    }

    @Inject(method="clicked",at=@At("RETURN"),remap=false)
    private void ffm$correctRejectedPrediction(int slot,int button,ClickType type,Player player,CallbackInfo ci) {
        var before=ffm$beforePrivateInventory;ffm$beforePrivateInventory=null;
        if (!(player instanceof ServerPlayer server) || before==null || server.containerMenu!=(Object)this) return;
        for(int index=0;index<before.size();index++)
            if(!ItemStack.matches(before.get(index),server.getInventory().getItem(index))) return;
        // Successful changes already use GuardMenu's delta packets. A losing/no-op request
        // needs a correction even when its authoritative inventory has never changed.
        for(int index=0;index<before.size();index++)
            server.connection.send(new ClientboundContainerSetSlotPacket(-2,0,index,server.getInventory().getItem(index)));
    }
}
