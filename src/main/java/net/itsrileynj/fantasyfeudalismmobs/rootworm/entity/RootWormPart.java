package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/** A targetable segment, registered through the parent's multipart lifecycle; it has no separate health or physics. */
public final class RootWormPart extends PartEntity<RootWorm> {
    private final int segment;

    RootWormPart(RootWorm parent, int segment) {
        super(parent);
        this.segment = segment;
        noPhysics = true;
        refreshDimensions();
    }

    /** Retain ordinary mob melee handling (critical hits, knockback and enchantments) after part targeting. */
    public static void redirectPlayerAttack(AttackEntityEvent event) {
        if (event.getTarget() instanceof RootWormPart part) {
            event.setCanceled(true);
            if (part.getParent().isAlive()) event.getEntity().attack(part.getParent());
        }
    }

    @Override public void setRemainingFireTicks(int ticks) {
        if (getParent() == null) super.setRemainingFireTicks(ticks);
        else getParent().setRemainingFireTicks(ticks);
    }
    @Override public int getRemainingFireTicks() { return getParent() == null ? super.getRemainingFireTicks() : getParent().getRemainingFireTicks(); }

    @Override public EntityDimensions getDimensions(Pose pose) { var search = getParent().getSearchPose(segment, 0);
        return EntityDimensions.scalable(RootWormSearchPose.width(segment, search), RootWormSearchPose.height(segment, search)).scale(getParent().getScale()); }
    @Override public boolean isPickable() { return getParent().isAlive(); }
    @Override public boolean isPushable() { return false; }
    @Override public float getPickRadius() { return 0.025F; }
    @Override public boolean is(Entity entity) { return this == entity || getParent() == entity; }
    @Override public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().isAlive() && getParent().hurt(source, amount);
    }
    @Override public InteractionResult interact(Player player, InteractionHand hand) { return getParent().interact(player, hand); }
    @Override public ItemStack getPickResult() { return getParent().getPickResult(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
