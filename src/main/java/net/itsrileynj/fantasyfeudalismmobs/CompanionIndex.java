package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;

/** Server-thread-only, level-scoped index. Values contain UUIDs, never strong world/entity references. */
public final class CompanionIndex {
    private static final Map<ServerLevel, Entries> LEVELS = new WeakHashMap<>();

    private static final class Entries {
        final Map<UUID, Set<UUID>> byOwner = new HashMap<>();
        final Map<UUID, UUID> ownerBySpider = new HashMap<>();
        void remove(UUID spider) {
            UUID owner = ownerBySpider.remove(spider);
            if (owner == null) return;
            Set<UUID> ids = byOwner.get(owner);
            if (ids != null && ids.remove(spider) && ids.isEmpty()) byOwner.remove(owner);
        }
    }

    public static void track(Spider spider) {
        if (!(spider.level() instanceof ServerLevel level)) return;
        var owner = ((SpiderState)spider).ffm$owner();
        Entries entries = LEVELS.get(level);
        if (entries != null) entries.remove(spider.getUUID());
        if (owner.isEmpty() || spider.isRemoved()) return;
        if (entries == null) { entries = new Entries(); LEVELS.put(level, entries); }
        entries.ownerBySpider.put(spider.getUUID(), owner.get());
        entries.byOwner.computeIfAbsent(owner.get(), id -> new HashSet<>()).add(spider.getUUID());
    }

    public static void forget(Spider spider, ServerLevel level) {
        Entries entries = LEVELS.get(level);
        if (entries != null) {
            entries.remove(spider.getUUID());
            if (entries.ownerBySpider.isEmpty()) LEVELS.remove(level);
        }
    }

    public static boolean protects(Player owner) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        Entries entries = LEVELS.get(level);
        Set<UUID> ids = entries == null ? null : entries.byOwner.get(owner.getUUID());
        if (ids == null) return false;
        for (UUID id : ids) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Spider spider && spider.isAlive() && !spider.isRemoved()
                    && SpiderCompanion.ownedBy(spider, owner) && !SpiderCompanion.isSitting(spider)
                    && !spider.isPassenger() && !spider.isLeashed()) return true;
        }
        return false;
    }
    private CompanionIndex() {}
}
