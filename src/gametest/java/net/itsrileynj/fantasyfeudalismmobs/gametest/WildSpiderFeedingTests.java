package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WildSpiderFeedingTests {
    @GameTest(template = "test_empty")
    public static void wildSpiderBeeKillFullyHeals(GameTestHelper h) {
        for (boolean cave : new boolean[]{false, true}) {
            Spider spider = cave ? h.spawn(EntityType.CAVE_SPIDER, 4, 2, 4) : h.spawn(EntityType.SPIDER, 4, 2, 4);
            spider.setNoAi(true); spider.setHealth(3);
            var bee = h.spawn(EntityType.BEE, 5, 2, 4); bee.setNoAi(true); bee.setHealth(1);
            h.assertTrue(spider.doHurtTarget(bee) && !bee.isAlive(), "The wild spider must actually kill the bee");
            h.assertTrue(spider.getHealth() == spider.getMaxHealth(), "Eating a bee fully heals a wounded wild spider");
            spider.discard(); bee.discard();
        }
        h.succeed();
    }

    @GameTest(template = "test_empty")
    public static void nonfatalBeeHitsAndTamedKillsDoNotHeal(GameTestHelper h) {
        Spider wild = h.spawn(EntityType.SPIDER, 4, 2, 4); wild.setNoAi(true); wild.setHealth(3);
        var bee = h.spawn(EntityType.BEE, 5, 2, 4); bee.setNoAi(true);
        h.assertTrue(wild.doHurtTarget(bee) && bee.isAlive(), "Nonfatal hit fixture");
        h.assertTrue(wild.getHealth() == 3, "A nonfatal bee hit is not eating the bee");
        Spider tamed = h.spawn(EntityType.SPIDER, 8, 2, 4); tamed.setNoAi(true);
        SpiderCompanion.tame(tamed, UUID.randomUUID()); tamed.setHealth(3);
        var second = h.spawn(EntityType.BEE, 9, 2, 4); second.setNoAi(true); second.setHealth(1);
        h.assertTrue(tamed.doHurtTarget(second) && !second.isAlive(), "Tamed bee kill fixture");
        h.assertTrue(tamed.getHealth() == 3, "Tamed spiders retain their existing feeding rules");
        h.succeed();
    }
}
