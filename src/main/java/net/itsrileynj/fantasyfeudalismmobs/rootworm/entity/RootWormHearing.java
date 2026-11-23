package net.itsrileynj.fantasyfeudalismmobs.rootworm.entity;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormConfig;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/** Hearing uses vanilla game events, not sound files, custom blocks, tags, or biome checks. */
public final class RootWormHearing implements GameEventListener {
    private final RootWorm worm;
    private final PositionSource source;
    public RootWormHearing(RootWorm worm) {
        this.worm = worm;
        source = new EntityPositionSource(worm, worm.getEyeHeight());
    }
    @Override public PositionSource getListenerSource() { return source; }
    @Override public int getListenerRadius() { return RootWormConfig.HEARING_RADIUS.get(); }
    @Override public DeliveryMode getDeliveryMode() { return DeliveryMode.BY_DISTANCE; }
    @Override public boolean handleGameEvent(ServerLevel level, Holder<GameEvent> event, GameEvent.Context context, Vec3 position) {
        if (!(context.sourceEntity() instanceof LivingEntity actor) || actor instanceof RootWorm) return false;
        if (event.is(GameEvent.STEP.key())) {
            if (actor.isSteppingCarefully()) return false;
            return worm.hearSound(actor, position, actor.isSprinting() ? 1.0 : 0.5);
        }
        if (event.is(GameEvent.EAT.key()) || event.is(GameEvent.HIT_GROUND.key()) || event.is(GameEvent.ENTITY_DAMAGE.key())
                || event.is(GameEvent.BLOCK_DESTROY.key()) || event.is(GameEvent.BLOCK_PLACE.key()) || event.is(GameEvent.EQUIP.key()) || event.is(GameEvent.UNEQUIP.key())) {
            return worm.hearSound(actor, position, 1.0);
        }
        return false;
    }
    /** Minecraft has a landing game event; NeoForge supplies the matching takeoff event. */
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        broadcastNearby(event.getEntity());
    }
    /** Players do not emit vanilla equip events when changing held items. Ignore count/durability-only updates. */
    public static void onEquipmentChange(net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (event.getSlot().getType() == net.minecraft.world.entity.EquipmentSlot.Type.HAND
                && !net.minecraft.world.item.ItemStack.isSameItem(event.getFrom(), event.getTo())) {
            broadcastNearby(event.getEntity());
        }
    }
    private static void broadcastNearby(LivingEntity actor) {
        if (!(actor.level() instanceof ServerLevel level) || actor instanceof RootWorm) return;
        int radius = RootWormConfig.HEARING_RADIUS.get();
        for (var worm : level.getEntitiesOfClass(RootWorm.class, actor.getBoundingBox().inflate(radius))) {
            worm.hearSound(actor, actor.position(), 1.0);
        }
    }
}
