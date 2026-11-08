package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ForestBirdTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<39;x++)for(int z=0;z<39;z++) {
            h.setBlock(x,1,z,Blocks.GRASS_BLOCK);
            for(int y=12;y<16;y++)if(h.getBlockState(new BlockPos(x,y,z)).is(Blocks.BARRIER))h.setBlock(x,y,z,Blocks.AIR);
        }
    }
    /** Long flights need more room than the original two-second flight fixture. */
    private static void flightFloor(GameTestHelper h) {
        for(int x=-24;x<64;x++)for(int z=-24;z<64;z++) {
            h.setBlock(x,1,z,Blocks.GRASS_BLOCK);
            for(int y=2;y<=23;y++)if(h.getBlockState(new BlockPos(x,y,z)).is(Blocks.BARRIER))h.setBlock(x,y,z,Blocks.AIR);
        }
    }
    private static ForestBird bird(GameTestHelper h,EntityType<ForestBird> type,int x,int z,boolean ai) {
        var bird=h.spawn(type,x,2,z);bird.setNoAi(!ai);bird.setOnGround(true);return bird;
    }
    private static boolean entry(GameTestHelper h,EntityType<?> type,int min,int max) {
        return h.getLevel().getBiome(h.absolutePos(new BlockPos(8,2,8))).value().getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(e->e.type==type&&e.minCount==min&&e.maxCount==max);
    }
    @GameTest(template="test_empty")
    public static void allFourBirdsHaveNativeEggsAndAttributes(GameTestHelper h) {
        floor(h);var p=h.makeMockPlayer(GameType.CREATIVE);p.getAbilities().instabuild=true;
        for(int i=0;i<4;i++) {
            var egg=new ItemStack(Birds.EGGS.get(i).get());p.setItemInHand(InteractionHand.MAIN_HAND,egg);
            var support=h.absolutePos(new BlockPos(4+i*7,1,8));
            var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false);
            h.assertTrue(egg.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Each bird has a usable native spawn egg");
            var type=Birds.TYPES.get(i).get();
            var list=h.getLevel().getEntitiesOfClass(ForestBird.class,new AABB(support).inflate(2),b->b.getType()==type);
            h.assertTrue(list.size()==1,"Each egg produces exactly one bird");
            var bird=list.getFirst();h.assertTrue(bird.kind()==BirdKind.values()[i]&&bird.getMaxHealth()==(i==3?8:6),"The native species and attributes match");
            h.assertTrue(bird.getMaxSpawnClusterSize()==(bird.kind()==BirdKind.ROBIN?2:bird.kind().flocking?6:1),"Native pack limits match flocking and solitary species");
            h.assertTrue(bird.getNavigation() instanceof GroundPathNavigation,"Ground movement starts with walking navigation");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_biomes")
    public static void temperateBiomesHaveSmallBirdFlocksAndSingleBlueJays(GameTestHelper h) {
        for(var biome:List.of(Biomes.FOREST,Biomes.FLOWER_FOREST,Biomes.BIRCH_FOREST,Biomes.OLD_GROWTH_BIRCH_FOREST,Biomes.DARK_FOREST,Biomes.CHERRY_GROVE)) {
            h.setBiome(biome);
            h.assertTrue(entry(h,Birds.ROBIN.get(),2,2)&&entry(h,Birds.GOLDFINCH.get(),3,6),"Native biome lists contain robin pairs and goldfinch flocks of three to six");
            h.assertTrue(entry(h,Birds.BLUE_JAY.get(),1,1)&&!entry(h,Birds.CROW.get(),1,1),"Blue jays are solitary; crows are not in temperate forests");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_biomes")
    public static void crowsSpawnInAllSpruceForestsAndJunglesAreExcluded(GameTestHelper h) {
        for(var biome:List.of(Biomes.TAIGA,Biomes.SNOWY_TAIGA,Biomes.OLD_GROWTH_PINE_TAIGA,Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            h.setBiome(biome);h.assertTrue(entry(h,Birds.CROW.get(),1,1),"Crows have native spruce-biome entries");
            h.assertFalse(entry(h,Birds.ROBIN.get(),2,2),"Robin flocks do not spawn in spruce forests");
        }
        for(var biome:List.of(Biomes.JUNGLE,Biomes.PLAINS,Biomes.DESERT)) {
            h.setBiome(biome);for(var type:Birds.TYPES)h.assertFalse(entry(h,type.get(),type==Birds.ROBIN?2:type==Birds.GOLDFINCH?3:1,type==Birds.ROBIN?2:type==Birds.GOLDFINCH?6:1),"No bird entries outside their requested biome sets");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_biomes")
    public static void placementRulesCheckBiomeAndSupportButDoNotRestrictEggs(GameTestHelper h) {
        floor(h);h.setDayTime(6000);h.setBiome(Biomes.FOREST);var pos=h.absolutePos(new BlockPos(8,2,8));
        h.assertTrue(Birds.spawnRules(Birds.ROBIN.get(),h.getLevel(),MobSpawnType.NATURAL,pos,h.getLevel().random),"A lit forest floor supports native robin spawning");
        h.assertFalse(Birds.spawnRules(Birds.CROW.get(),h.getLevel(),MobSpawnType.NATURAL,pos,h.getLevel().random),"Crow natural spawning rejects a temperate forest");
        h.setBlock(8,1,8,Blocks.OAK_LEAVES);h.assertTrue(Birds.spawnRules(Birds.ROBIN.get(),h.getLevel(),MobSpawnType.NATURAL,pos,h.getLevel().random),"Birds may spawn on tree foliage");
        h.setBlock(8,1,8,Blocks.AIR);h.assertFalse(Birds.spawnRules(Birds.ROBIN.get(),h.getLevel(),MobSpawnType.NATURAL,pos,h.getLevel().random),"A bird cannot naturally spawn over empty space");
        h.setBiome(Biomes.DESERT);h.assertTrue(Birds.spawnRules(Birds.ROBIN.get(),h.getLevel(),MobSpawnType.SPAWN_EGG,pos,h.getLevel().random),"Player spawn eggs remain usable in every biome");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void robinAndGoldfinchSpawnGroupsShareSavedFlocksWithoutExceedingSix(GameTestHelper h) {
        floor(h);
        for(var type:List.of(Birds.ROBIN.get(),Birds.GOLDFINCH.get())) {
            SpawnGroupData data=null;UUID previous=null;int cap=type==Birds.ROBIN.get()?2:6;
            for(int i=0;i<8;i++) {
                var b=bird(h,type,4+i,8,false);data=b.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.NATURAL,data);
                if(i%cap==0) {
                    h.assertTrue(!b.flock().equals(previous),"The next group starts a different identity at the species cap");
                    previous=b.flock();
                } else h.assertTrue(b.flock().equals(previous),"Members share the capped natural group identity");
            }
        }
        for(var type:List.of(Birds.CROW.get(),Birds.BLUE_JAY.get())) {
            var b=bird(h,type,8,12,false);b.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.NATURAL,null);
            h.assertTrue(b.flock()==null,"Solitary species do not join a flock");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_threats")
    public static void onlySurvivalAndAdventurePlayersInsideFourBlocksScareBirds(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.ROBIN.get(),8,8,false);ServerPlayer p=AdvancementTests.player(h);
        try {
            p.moveTo(b.position().add(3.9,0,0));p.setGameMode(GameType.SURVIVAL);h.assertTrue(b.nearbyThreat()==p,"Survival players inside four blocks scare birds");
            p.setGameMode(GameType.ADVENTURE);h.assertTrue(b.nearbyThreat()==p,"Adventure players also scare birds");
            p.setGameMode(GameType.CREATIVE);h.assertTrue(b.nearbyThreat()==null,"Creative players do not scare birds");
            p.setGameMode(GameType.SPECTATOR);h.assertTrue(b.nearbyThreat()==null,"Spectators do not scare birds");
            p.setGameMode(GameType.SURVIVAL);p.moveTo(b.position().add(4.1,0,0));h.assertTrue(b.nearbyThreat()==null,"Players outside four blocks do not trigger flight");h.succeed();
        } finally { AdvancementTests.remove(p); }
    }
    @GameTest(template="test_empty")
    public static void saveReloadPreservesFlockAndRecoversFromAnInterruptedFlight(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.GOLDFINCH.get(),8,8,false);
        b.finalizeSpawn(h.getLevel(),h.getLevel().getCurrentDifficultyAt(b.blockPosition()),MobSpawnType.NATURAL,null);
        UUID flock=b.flock();long next=b.nextFlight();b.setFlying(true);
        h.assertTrue(b.isNoGravity()&&b.getNavigation() instanceof FlyingPathNavigation,"Flight uses native flying navigation");
        var tag=b.saveWithoutId(new CompoundTag());b.discard();var loaded=Birds.GOLDFINCH.get().create(h.getLevel());loaded.load(tag);h.getLevel().addFreshEntity(loaded);
        h.assertTrue(flock.equals(loaded.flock())&&loaded.nextFlight()==next,"Flock identity and ambient-flight deadline survive reload");
        h.assertTrue(!loaded.flightActive()&&!loaded.isNoGravity()&&loaded.getNavigation() instanceof GroundPathNavigation,"Reload cannot leave a bird permanently hovering");h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_live_flee",timeoutTicks=1000)
    public static void actualBirdAiTakesOffFromAPlayerThenLands(GameTestHelper h) {
        flightFloor(h);var b=bird(h,Birds.BLUE_JAY.get(),10,10,true);var start=b.position();double[] peak={start.y};
        ServerPlayer p=AdvancementTests.player(h);p.moveTo(start.add(-2,0,0));
        h.onEachTick(()->peak[0]=Math.max(peak[0],b.getY()));
        h.runAfterDelay(35,()->{
            try { h.assertTrue(peak[0]>start.y+.8&&b.distanceToSqr(p)>16,"Real flight must gain height and leave the player: "+b.position()+" peak="+peak[0]); }
            finally { AdvancementTests.remove(p); }
        });
        h.succeedWhen(()->{h.assertTrue(h.getTick()>45&&b.onGround()&&!b.flightActive(),"A fleeing bird must eventually land on real support: "+b.position());});
    }
    @GameTest(template="test_empty",batch="bird_live_walk",timeoutTicks=180)
    public static void groundBirdsWalkAndQuicklyChangeTheirHeadDirection(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.ROBIN.get(),12,12,true);var start=b.position();float first=b.getYHeadRot();boolean[] looked={false};
        h.onEachTick(()->looked[0]|=Math.abs(b.getYHeadRot()-first)>20);
        h.runAfterDelay(10,()->{for(var g:b.goalSelector.getAvailableGoals())if(g.getGoal() instanceof WaterAvoidingRandomStrollGoal stroll)stroll.trigger();});
        h.succeedWhen(()->h.assertTrue(b.position().distanceToSqr(start)>.04&&looked[0]&&b.onGround()&&!b.flightActive(),"A ground bird must walk and make quick native head turns: distance="+b.position().distanceToSqr(start)+" looked="+looked[0]+" ground="+b.onGround()+" flight="+b.flightActive()));
    }
    @GameTest(template="test_empty",batch="bird_water",timeoutTicks=240)
    public static void birdsCanLeaveWaterAndCannotTakeFallDamage(GameTestHelper h) {
        floor(h);h.setBlock(8,2,8,Blocks.WATER);var b=h.spawn(Birds.CROW.get(),8,2,8);float health=b.getHealth();
        h.assertFalse(b.causeFallDamage(30,1,h.getLevel().damageSources().fall()),"Flying wildlife is immune to fall damage");
        h.assertTrue(b.getHealth()==health,"Fall immunity does not consume health");
        h.succeedWhen(()->h.assertTrue(!b.isInWater()&&b.getY()>h.absolutePos(new BlockPos(8,2,8)).getY()+.8,"A bird in water must be able to take flight"));
    }
    @GameTest(template="test_empty",batch="bird_ceiling")
    public static void flightCeilingUsesTerrainInsteadOfLeavesAndTrunks(GameTestHelper h) {
        floor(h);h.setBlock(8,8,8,Blocks.OAK_LEAVES);
        for(int y=2;y<10;y++)h.setBlock(10,y,8,Blocks.SPRUCE_LOG);
        h.setBlock(10,10,8,Blocks.SPRUCE_LEAVES);
        var b=bird(h,Birds.CROW.get(),6,8,false);var ground=h.absolutePos(new BlockPos(8,2,8));
        h.assertTrue(BirdAltitude.surfaceY(h.getLevel(),ground.getX(),ground.getZ())==ground.getY(),"Leaves cannot raise the terrain height");
        var trunk=h.absolutePos(new BlockPos(10,2,8));
        h.assertTrue(BirdAltitude.surfaceY(h.getLevel(),trunk.getX(),trunk.getZ())==trunk.getY(),"Tree trunks cannot raise the terrain height either");
        h.assertTrue(!BirdAltitude.allowed(b,ground.getX()+.5,ground.getY()+BirdAltitude.MAX_ABOVE_GROUND,ground.getZ()+.5),"The complete bird stays below the twenty-block limit");
        b.setFlying(true);var path=b.getNavigation().createPath(new BlockPos(ground.getX(),ground.getY()+BirdAltitude.MAX_ABOVE_GROUND+3,ground.getZ()),0);
        h.assertTrue(path==null||!path.canReach(),"Native flight pathfinding rejects targets above the ceiling");h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_ceiling",timeoutTicks=100)
    public static void upwardSteeringCannotPushBirdsAboveTwentyBlocks(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.BLUE_JAY.get(),18,18,false);b.setFlying(true);
        double max=BirdAltitude.maxFeet(b,b.getX(),b.getZ());b.moveTo(b.getX(),max-.1,b.getZ());
        for(int i=0;i<30;i++) {
            b.setDeltaMovement(0,.4,0);b.travel(Vec3.ZERO);
            h.assertTrue(b.getY()+b.getBbHeight()<=BirdAltitude.surfaceY(h.getLevel(),b.getBlockX(),b.getBlockZ())+BirdAltitude.MAX_ABOVE_GROUND+.00001,"Upward momentum must respect the complete-body ceiling");
        }
        h.succeed();
    }
    @GameTest(template="test_empty",batch="bird_short_walk")
    public static void groundWalkTargetsStayWithinTwoBlocksAndFlightsAreFrequent(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.ROBIN.get(),18,18,false);var start=b.position();
        for(int i=0;i<20;i++) { b.scheduleFlight();long wait=b.nextFlight()-h.getLevel().getGameTime();h.assertTrue(wait>=80&&wait<=160,"Flights are scheduled four to eight seconds after landing"); }
        var walk=new BirdGoals.ShortWalk(b);
        h.succeedWhen(()->{
            h.assertTrue(walk.canUse(),"A landed bird must select a real short walk");
            walk.start();var target=b.getNavigation().getTargetPos();
            h.assertTrue(target!=null&&Vec3.atBottomCenterOf(target).subtract(start).multiply(1,0,1).lengthSqr()<=4.01,"Ground walking is limited to a couple of blocks");walk.stop();
        });
    }

    @GameTest(template="test_empty",batch="robin_pairs")
    public static void robinPairsAreExclusiveAndPersistAcrossReload(GameTestHelper h) {
        floor(h);var a=bird(h,Birds.ROBIN.get(),8,8,true);var b=bird(h,Birds.ROBIN.get(),10,8,true);var c=bird(h,Birds.ROBIN.get(),12,8,true);
        a.findPartner();c.findPartner();
        h.assertTrue(a.partner()==b&&b.partner()==a&&c.partner()==null,"Each robin has one reciprocal partner; a third cannot join");
        var first=a.saveWithoutId(new CompoundTag());var second=b.saveWithoutId(new CompoundTag());a.discard();b.discard();
        var loadedA=Birds.ROBIN.get().create(h.getLevel());var loadedB=Birds.ROBIN.get().create(h.getLevel());
        loadedA.load(first);loadedB.load(second);h.getLevel().addFreshEntity(loadedA);h.getLevel().addFreshEntity(loadedB);
        h.assertTrue(loadedA.partner()==loadedB&&loadedB.partner()==loadedA,"Both saved partner UUIDs recover after reload");
        loadedB.kill();loadedA.findPartner();
        h.assertTrue(loadedA.partner()==c&&c.partner()==loadedA,"A surviving robin can pair with another unpaired robin");h.succeed();
    }
    @GameTest(template="test_empty",batch="robin_pairs")
    public static void solitaryRobinsWaitForCompanyButStillEscapeThreats(GameTestHelper h) {
        floor(h);var b=bird(h,Birds.ROBIN.get(),8,8,true);b.flyToward(b.position().add(5,0,0));
        var goal=new BirdGoals.Flight(b);
        h.assertFalse(goal.canUse(),"A lone grounded robin does not start ambient solo flights");
        ServerPlayer p=AdvancementTests.player(h);
        p.moveTo(b.position().add(2,0,0));
        h.runAfterDelay(6,()->{
            try { h.assertTrue(goal.canUse(),"A lone robin can still escape a nearby Survival player");h.succeed(); }
            finally { AdvancementTests.remove(p); }
        });
    }
    @GameTest(template="test_empty",batch="robin_live_pair",timeoutTicks=1000)
    public static void pairedRobinsTakeOffTogetherAndRemainClose(GameTestHelper h) {
        flightFloor(h);var a=bird(h,Birds.ROBIN.get(),16,16,true);var b=bird(h,Birds.ROBIN.get(),18,16,true);
        a.findPartner();a.flyToward(a.position().add(8,0,0));
        boolean[] together={false};double[] farthest={0};
        h.onEachTick(()->{
            if(a.flightActive()&&b.flightActive()&&a.getY()>h.absolutePos(new BlockPos(16,2,16)).getY()+.4&&b.getY()>h.absolutePos(new BlockPos(18,2,16)).getY()+.4) {
                h.assertTrue(a.cruiseUntil()==b.cruiseUntil(),"Robin partners share a cruise deadline so landing does not restart their flight");
                together[0]=true;farthest[0]=Math.max(farthest[0],a.distanceToSqr(b));
            }
        });
        h.succeedWhen(()->{
            h.assertTrue(together[0],"The pair must take off and gain height together");
            h.assertTrue(farthest[0]<=64,"Paired flight remains within eight blocks: "+farthest[0]);
            h.assertTrue(h.getTick()>45&&a.onGround()&&b.onGround()&&!a.flightActive()&&!b.flightActive(),"Both partners land after coordinated flight: a="+a.position()+" ground="+a.onGround()+" flight="+a.flightActive()+" landing="+a.landingFlight()+" b="+b.position()+" ground="+b.onGround()+" flight="+b.flightActive()+" landing="+b.landingFlight());
        });
    }

    @GameTest(template="test_empty",batch="bird_cruise",timeoutTicks=1000)
    public static void birdsCruiseAboveGroundForLongerThenSettleWithoutLowHovering(GameTestHelper h) {
        flightFloor(h);var b=bird(h,Birds.BLUE_JAY.get(),18,18,true);b.flyToward(b.position().add(8,0,0));
        double[] peak={0};int[] low={0},longestLow={0};boolean[] sustained={false};
        h.onEachTick(()->{
            double altitude=b.getY()-BirdAltitude.surfaceY(h.getLevel(),b.getBlockX(),b.getBlockZ());peak[0]=Math.max(peak[0],altitude);
            if(h.getTick()>60&&b.flightActive()&&altitude<1.3)low[0]++;else low[0]=0;
            longestLow[0]=Math.max(longestLow[0],low[0]);
        });
        h.runAfterDelay(200,()->{
            h.assertTrue(b.flightActive()&&!b.landingFlight()&&peak[0]>=5,"A bird cruises for at least ten seconds well above the ground: peak="+peak[0]);sustained[0]=true;
        });
        h.succeedWhen(()->{
            h.assertTrue(sustained[0]&&h.getTick()>400&&b.onGround()&&!b.flightActive(),"Longer flight must eventually finish on support: "+b.position());
            h.assertTrue(longestLow[0]<=40,"Final descent must not hover just above support for long periods: "+longestLow[0]);
        });
    }
    @GameTest(template="test_empty",batch="bird_perches",timeoutTicks=800)
    public static void landingChoosesTreeCanopiesBeforeNearbyBareGround(GameTestHelper h) {
        floor(h);
        for(int x=14;x<=22;x++)for(int z=14;z<=22;z++)h.setBlock(x,7,z,Blocks.OAK_LEAVES);
        var b=bird(h,Birds.CROW.get(),18,18,false);b.moveTo(b.getX(),h.absolutePos(new BlockPos(18,11,18)).getY(),b.getZ());b.setOnGround(false);
        var goal=new BirdGoals.Flight(b);goal.start();h.onEachTick(goal::tick);
        h.succeedWhen(()->{
            var target=b.getNavigation().getTargetPos();
            h.assertTrue(b.landingFlight()&&target!=null,"Flight must select a landing approach after cruising");
            h.assertTrue(h.relativePos(target).getY()==8,"Higher tree leaves are preferred over reachable bare ground: "+h.relativePos(target));
            goal.stop();
        });
    }

}
