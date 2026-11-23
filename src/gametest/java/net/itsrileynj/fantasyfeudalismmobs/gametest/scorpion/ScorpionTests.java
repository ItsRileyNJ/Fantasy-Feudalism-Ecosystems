package net.itsrileynj.fantasyfeudalismmobs.gametest.scorpion;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.scorpion.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScorpionTests {
    private static Scorpion spawn(GameTestHelper h) {h.getLevel().setDayTime(18000);var s=h.spawn(ScorpionModule.SCORPION.get(),16,2,16);s.setOnGround(true);return s;}
    private static net.minecraft.world.entity.animal.Pig victim(GameTestHelper h,Scorpion s) {
        var p=h.spawn(EntityType.PIG,16,2,15);p.setNoAi(true);p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);p.setHealth(40);return p;
    }
    @GameTest(template="test_empty")
    public static void scorpionSpawnEggCreatesSpiderSizedIndependentMonster(GameTestHelper h) {
        h.setBlock(8,1,8,Blocks.SAND);var p=h.makeMockPlayer(GameType.CREATIVE);p.getAbilities().instabuild=true;
        var egg=new ItemStack(ScorpionModule.EGG.get());p.setItemInHand(InteractionHand.MAIN_HAND,egg);var pos=h.absolutePos(new BlockPos(8,1,8));
        h.assertTrue(egg.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false))).consumesAction(),"Egg uses native placement");
        var scorpions=h.getLevel().getEntitiesOfClass(Scorpion.class,new AABB(pos).inflate(3));h.assertTrue(scorpions.size()==1,"One egg makes one scorpion");
        var s=scorpions.getFirst();h.assertTrue(Math.abs(s.getBbWidth()-1.4)<.001&&Math.abs(s.getBbHeight()-.9)<.001&&s.getMaxHealth()==16,"Scorpion has vanilla spider dimensions and eight hearts");
        h.assertTrue(!(((Entity)s) instanceof net.minecraft.world.entity.monster.Spider),"Scorpion does not inherit pet-spider mechanics");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void lavaScorpionEggHasQuarterSizeAndRetainsVariantOnReload(GameTestHelper h) {
        h.setBlock(8,1,8,Blocks.SAND);var player=h.makeMockPlayer(GameType.CREATIVE);player.getAbilities().instabuild=true;
        var egg=new ItemStack(ScorpionModule.LAVA_EGG.get());player.setItemInHand(InteractionHand.MAIN_HAND,egg);var pos=h.absolutePos(new BlockPos(8,1,8));
        egg.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false)));
        var matches=h.getLevel().getEntitiesOfClass(Scorpion.class,new AABB(pos).inflate(3));
        h.assertTrue(matches.size()==1,"One lava egg spawns one mob");var small=matches.getFirst();
        h.assertTrue(small.isLava()&&small.visualScale()==.25F&&Math.abs(small.getBbWidth()-.35)<.001&&Math.abs(small.getBbHeight()-.225)<.001,"Variant has quarter-scale rendering and collision");
        h.assertTrue(small.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD),"Small variant retains native arthropod rules");
        var saved=new net.minecraft.nbt.CompoundTag();small.save(saved);var restored=EntityType.loadEntityRecursive(saved,h.getLevel(),e->e);
        h.assertTrue(restored instanceof Scorpion copy&&copy.isLava()&&copy.visualScale()==.25F,"Independent variant identity and scale survive reload");
        h.assertTrue(ScorpionModule.SCORPION.get().create(h.getLevel()).visualScale()==1,"Original scorpion remains full-sized");h.succeed();
    }
    @GameTest(template="test_empty",batch="lava_scorpion_habitat")
    public static void lavaScorpionRequiresCoveredCaveAndNearbyLava(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(8,2,8));var level=h.getLevel();var type=ScorpionModule.LAVA_SCORPION.get();
        h.setBlock(8,1,8,Blocks.STONE);h.setBlock(8,7,8,Blocks.STONE);
        h.runAfterDelay(3,()->{
            h.assertTrue(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,level.random),"Covered cave without lava rejects spawning");
            h.setBlock(11,2,8,Blocks.LAVA);
        });
        h.runAfterDelay(6,()->{
            h.assertTrue(SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,level.random),"Nearby lava permits a covered cave spawn despite lava light: sky="+level.canSeeSky(pos)+" height="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())+" pos="+pos+" difficulty="+level.getDifficulty());
            h.setBlock(8,7,8,Blocks.AIR);
        });
        h.runAfterDelay(9,()->{h.assertTrue(!SpawnPlacements.checkSpawnRules(type,level,MobSpawnType.NATURAL,pos,level.random),"Surface lava does not qualify as a cave");h.succeed();});
    }
    @GameTest(template="test_root_worm",batch="scorpion_daylight")
    public static void normalScorpionIsPassiveByDayAndCancelsItsStingAtDawn(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var p=victim(h,s);var level=h.getLevel();
        level.setDayTime(6000);s.setTarget(p);s.beginSting(p);
        h.assertTrue(s.getTarget()==null&&!s.isStinging()&&!s.doHurtTarget(p)&&p.getHealth()==40,"Normal scorpion refuses daytime targets and damage");
        level.setDayTime(13000);s.setTarget(p);s.beginSting(p);
        h.assertTrue(s.getTarget()==p&&s.isStinging(),"Normal scorpion can hunt at night");
        level.setDayTime(23000);s.aiStep();
        h.assertTrue(s.getTarget()==null&&!s.isStinging()&&p.getHealth()==40&&!p.hasEffect(MobEffects.POISON),"Dawn cancels a pending sting before damage");
        var lava=ScorpionModule.LAVA_SCORPION.get().create(level);lava.setPos(s.position());level.addFreshEntity(lava);lava.setNoAi(true);
        level.setDayTime(6000);h.assertTrue(lava.doHurtTarget(p)&&p.hasEffect(MobEffects.POISON),"Lava scorpion remains hostile during the day");h.succeed();
    }
    @GameTest(template="test_empty",batch="lava_scorpion_depth")
    public static void lavaScorpionSpawnBoundaryRequiresNegativeY(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(8,2,8));
        for(int y:new int[]{-1,0,20}) {
            var pos=new BlockPos(origin.getX()+(y+1)*16,y,origin.getZ());
            level.setBlockAndUpdate(pos.below(),Blocks.DEEPSLATE.defaultBlockState());
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.above(6),Blocks.DEEPSLATE.defaultBlockState());
            level.setBlockAndUpdate(pos.offset(3,0,0),Blocks.LAVA.defaultBlockState());
        }
        h.runAfterDelay(4,()->{
            for(int y:new int[]{-1,0,20}) {
                var pos=new BlockPos(origin.getX()+(y+1)*16,y,origin.getZ());
                h.assertTrue(SpawnPlacements.checkSpawnRules(ScorpionModule.LAVA_SCORPION.get(),level,MobSpawnType.NATURAL,pos,level.random)==(y<0),"Lava cave spawn boundary at Y="+y+" sky="+level.canSeeSky(pos)+" height="+level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())+" lava="+level.getFluidState(pos.offset(3,0,0))+" ground="+level.getBlockState(pos.below()));
            }h.succeed();
        });
    }
    @GameTest(template="test_empty",batch="lava_scorpion_fire",timeoutTicks=80)
    public static void lavaScorpionResistsNativeFireAndLavaWhileNormalScorpionDoesNot(GameTestHelper h) {
        h.setBlock(8,1,8,Blocks.DEEPSLATE);h.setBlock(8,2,8,Blocks.LAVA);
        var lava=h.spawn(ScorpionModule.LAVA_SCORPION.get(),8,2,8);lava.setNoAi(true);var level=h.getLevel();
        h.assertTrue(lava.fireImmune()&&lava.getMaxSpawnClusterSize()==5,"Lava variant has native fire immunity and permits full packs of five");
        for(var damage:List.of(level.damageSources().inFire(),level.damageSources().onFire(),level.damageSources().lava()))
            h.assertTrue(!lava.hurt(damage,4)&&lava.getHealth()==16,"Native fire/lava damage is rejected");
        var normal=h.spawn(ScorpionModule.SCORPION.get(),12,2,8);normal.setNoAi(true);
        h.assertTrue(!normal.fireImmune()&&normal.hurt(level.damageSources().lava(),4),"Normal variant retains lava vulnerability");
        h.runAfterDelay(40,()->{h.assertTrue(lava.isAlive()&&lava.getHealth()==16&&!lava.isOnFire(),"Physical lava exposure causes no health loss or burning");h.succeed();});
    }
    @GameTest(template="test_root_worm",batch="scorpion_predation",timeoutTicks=200)
    public static void normalScorpionHuntsAndEatsWildAndTamedSpiders(GameTestHelper h) {
        var s=spawn(h);var wild=h.spawn(EntityType.SPIDER,16,2,15);wild.setNoAi(true);
        net.minecraft.world.entity.monster.Spider[] pet={null};boolean[] firstMeal={false};
        h.onEachTick(()->{
            firstMeal[0]|=!wild.isAlive()&&s.isEating();
            if(firstMeal[0]&&!s.isEating()&&pet[0]==null) {
                pet[0]=EntityType.SPIDER.create(h.getLevel());pet[0].moveTo(s.position().add(0,0,-.7));pet[0].setNoAi(true);
                pet[0].getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(30);pet[0].setHealth(30);
                net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion.data(pet[0]).putUUID("owner",UUID.randomUUID());
                net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion.sync(pet[0]);h.getLevel().addFreshEntity(pet[0]);
            }
        });
        h.succeedWhen(()->h.assertTrue(firstMeal[0]&&pet[0]!=null&&!pet[0].isAlive(),"Native hunting kills and eats both wild and full-health pet spiders"));
    }
    @GameTest(template="test_root_worm")
    public static void normalScorpionCannotEatAnInvulnerableSpider(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var spider=h.spawn(EntityType.SPIDER,16,2,15);spider.setNoAi(true);spider.setInvulnerable(true);s.beginSting(spider);
        h.runAfterDelay(20,()->{h.assertTrue(spider.isAlive()&&spider.getHealth()==spider.getMaxHealth()&&!s.isEating(),"Protected spider neither dies nor triggers feeding");h.succeed();});
    }
    @GameTest(template="test_root_worm",batch="scorpion_player_attacks",timeoutTicks=120)
    public static void normalScorpionPincerIsPlainDamageAndStingGivesTenSecondsOfNausea(GameTestHelper h) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"sting-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(GameType.SURVIVAL);
        var s=spawn(h);s.setNoAi(true);p.setNoGravity(true);p.moveTo(s.position().add(0,0,-.5));p.setDeltaMovement(Vec3.ZERO);boolean[] plain={false};
        // Real players have sixty ticks of native join protection; exercise damage after it expires.
        h.runAfterDelay(65,()->{p.moveTo(s.position().add(0,0,-.5));p.setDeltaMovement(Vec3.ZERO);s.beginPincer(p);});
        h.runAfterDelay(71,()->{plain[0]=p.getHealth()<20&&!p.hasEffect(MobEffects.CONFUSION)&&!p.hasEffect(MobEffects.POISON);});
        h.runAfterDelay(77,()->{p.invulnerableTime=0;p.setHealth(20);p.moveTo(s.position().add(0,0,-.5));p.setDeltaMovement(Vec3.ZERO);s.beginSting(p);});
        h.runAfterDelay(88,()->{try {
            var nausea=p.getEffect(MobEffects.CONFUSION);h.assertTrue(plain[0]&&p.getHealth()<20&&nausea!=null&&nausea.getDuration()>=190&&!p.hasEffect(MobEffects.POISON),"Pincers do plain damage; stings apply ten seconds of Nausea without poison: plain="+plain[0]+" health="+p.getHealth()+" nausea="+nausea+" player="+p.position()+" scorpion="+s.position());h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}});
    }
    @GameTest(template="test_empty",batch="lava_scorpion_contact")
    public static void lavaScorpionContactMatchesMagmaAndNeverTargetsPlayers(GameTestHelper h) {
        var lava=h.spawn(ScorpionModule.LAVA_SCORPION.get(),8,2,8);lava.setNoAi(true);var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(lava.position());
        lava.setTarget(p);lava.beginSting(p);h.assertTrue(lava.getTarget()==null&&!lava.isAttacking()&&!lava.doHurtTarget(p),"Lava scorpions refuse all player attacks");
        lava.playerTouch(p);h.assertTrue(p.getHealth()==19&&!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.CONFUSION),"Touch deals one point of hot-floor damage without venom");
        p.invulnerableTime=0;p.setHealth(20);p.setShiftKeyDown(true);lava.playerTouch(p);h.assertTrue(p.getHealth()==20,"Careful stepping protects like a magma block");
        p.setShiftKeyDown(false);p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,0));lava.playerTouch(p);h.assertTrue(p.getHealth()==20,"Native fire resistance protects from contact heat");
        for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)){var protectedPlayer=h.makeMockPlayer(mode);protectedPlayer.setPos(lava.position());lava.playerTouch(protectedPlayer);h.assertTrue(protectedPlayer.getHealth()==20,"Creative and spectator contact is safe");}
        h.succeed();
    }
    @GameTest(template="test_empty",batch="scorpion_arrows")
    public static void largeScorpionRejectsArrowsButLavaScorpionAndOtherWeaponsRemainNormal(GameTestHelper h) {
        var normal=h.spawn(ScorpionModule.SCORPION.get(),8,2,8);normal.setNoAi(true);var level=h.getLevel();
        for(var type:List.of(EntityType.ARROW,EntityType.SPECTRAL_ARROW)) {
            var arrow=(net.minecraft.world.entity.projectile.AbstractArrow)type.create(level);
            h.assertTrue(!normal.hurt(level.damageSources().arrow(arrow,null),4)&&normal.getHealth()==16,"Large scorpion rejects normal and spectral arrow damage");
        }
        var lava=h.spawn(ScorpionModule.LAVA_SCORPION.get(),12,2,8);lava.setNoAi(true);
        h.assertTrue(lava.hurt(level.damageSources().arrow(EntityType.ARROW.create(level),null),4),"Lava scorpion remains vulnerable to arrows");
        h.assertTrue(normal.hurt(level.damageSources().trident(EntityType.TRIDENT.create(level),null),4),"Tridents retain damage against the large scorpion");
        h.succeed();
    }
    @GameTest(template="test_root_worm",batch="lava_scorpion_natural_pipeline",timeoutTicks=100)
    public static void lavaScorpionNaturallySpawnsInALitDeepslateCave(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(20,2,20));
        var origin=new BlockPos((center.getX()&~15)+8,center.getY(),(center.getZ()&~15)+8);
        for(int x=0;x<40;x++)for(int z=0;z<40;z++) {h.setBlock(x,1,z,Blocks.DEEPSLATE);h.setBlock(x,8,z,Blocks.DEEPSLATE);}
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(origin.offset(x,0,z),Blocks.LAVA.defaultBlockState());
        for(int x=4;x<40;x+=8)for(int z=4;z<40;z+=8)h.setBlock(x,5,z,Blocks.LIGHT);
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"spawn-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(GameType.CREATIVE);
        player.moveTo(Vec3.atBottomCenterOf(origin.offset(40,0,0)));
        var biomeFill=net.minecraft.server.commands.FillBiomeCommand.fill(level,h.absolutePos(new BlockPos(0,1,0)),h.absolutePos(new BlockPos(39,7,39)),level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(Biomes.DRIPSTONE_CAVES));
        h.assertTrue(biomeFill.right().isEmpty(),"The bounded cave fixture receives its biome: "+biomeFill);
        h.runAfterDelay(5,()->{try {
            h.assertTrue(level.getBlockState(origin).getLightEmission(level,origin)>0,"The test contains emissive lava, not a light-free fixture");
            level.random.setSeed(6262L);int[] spawned={0};
            for(int attempt=0;attempt<256&&spawned[0]==0;attempt++) {
                net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER,level,level.getChunk(origin),origin,
                        (type,pos,chunk)->type==ScorpionModule.LAVA_SCORPION.get(),(mob,chunk)->spawned[0]++);
            }
            h.assertTrue(spawned[0]>0,"The native weighted spawn-category pipeline produces Lava Scorpions near a non-spectator player");h.succeed();
        }finally{level.getServer().getPlayerList().remove(player);}});
    }
    @GameTest(template="test_root_worm",batch="normal_scorpion_surface_pipeline",timeoutTicks=200)
    public static void normalScorpionNaturallySpawnsOnBadlandsSurfaceMaterials(GameTestHelper h) {
        var level=h.getLevel();level.setDayTime(18000);var base=h.absolutePos(BlockPos.ZERO);
        var origin=new BlockPos(((base.getX()+20)&~15)+8,64,((base.getZ()+20)&~15)+8);
        for(int x=0;x<40;x++)for(int z=0;z<40;z++)level.setBlockAndUpdate(new BlockPos(base.getX()+x,62,base.getZ()+z),Blocks.STONE.defaultBlockState());
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"mesa-spawn-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);level.getServer().getPlayerList().placeNewPlayer(connection,player,cookie);player.setGameMode(GameType.CREATIVE);
        player.moveTo(Vec3.atBottomCenterOf(origin.offset(40,0,0)));
        h.runAfterDelay(5,()->{try {
            for(var biome:List.of(Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS)) {
                var fill=net.minecraft.server.commands.FillBiomeCommand.fill(level,new BlockPos(base.getX(),63,base.getZ()),new BlockPos(base.getX()+39,70,base.getZ()+39),level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(biome));
                h.assertTrue(fill.right().isEmpty(),"Surface biome is installed: "+biome);
                for(long time:biome==Biomes.WOODED_BADLANDS?new long[]{6000,18000}:new long[]{18000}) {
                    level.setDayTime(time);
                    for(var ground:biome==Biomes.WOODED_BADLANDS?List.of(Blocks.RED_SAND,Blocks.TERRACOTTA,Blocks.GRASS_BLOCK,Blocks.COARSE_DIRT):List.of(Blocks.RED_SAND,Blocks.TERRACOTTA)) {
                        for(int x=0;x<40;x++)for(int z=0;z<40;z++)level.setBlockAndUpdate(new BlockPos(base.getX()+x,63,base.getZ()+z),ground.defaultBlockState());
                        level.random.setSeed(6464L);int[] spawned={0};int attempts=0;
                        while(attempts<1024&&spawned[0]==0) {
                            attempts++;
                            net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER,level,level.getChunk(origin),origin,
                                    (type,pos,chunk)->type==ScorpionModule.SCORPION.get(),(mob,chunk)->{spawned[0]++;mob.discard();});
                        }
                        h.assertTrue(spawned[0]>0,"Native normal Scorpion spawning on "+ground+" in "+biome+" at time="+time);
                        System.out.println("SCORPION_SURFACE_SPAWN biome="+biome.location()+" ground="+ground+" time="+time+" attempts="+attempts+" spawned="+spawned[0]);
                    }
                }
            }h.succeed();
        }finally{level.getServer().getPlayerList().remove(player);}});
    }
    @GameTest(template="test_empty",batch="scorpion_wooded_daylight")
    public static void scorpionWoodedDaylightIgnoresSunlightButPreservesTorchProtectionAndPassiveDays(GameTestHelper h) {
        var level=h.getLevel();level.setDayTime(6000);h.setBlock(8,1,8,Blocks.GRASS_BLOCK);var pos=h.absolutePos(new BlockPos(8,2,8));int[] blockLight={0};
        var daylight=(net.minecraft.world.level.ServerLevelAccessor)java.lang.reflect.Proxy.newProxyInstance(net.minecraft.world.level.ServerLevelAccessor.class.getClassLoader(),new Class<?>[]{net.minecraft.world.level.ServerLevelAccessor.class},(proxy,method,args)->{
            if(method.getName().equals("getBrightness"))return args[0]==net.minecraft.world.level.LightLayer.SKY?15:blockLight[0];
            if(method.getName().equals("getMaxLocalRawBrightness"))return 15;
            try{return method.invoke(level,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
        });
        for(var biome:List.of(Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS)) {
            h.setBiome(biome);boolean allowed=SpawnPlacements.checkSpawnRules(ScorpionModule.SCORPION.get(),daylight,MobSpawnType.NATURAL,pos,net.minecraft.util.RandomSource.create(65));
            h.assertTrue(allowed==(biome==Biomes.WOODED_BADLANDS),"Full daylight spawning is enabled only in wooded badlands");
        }
        blockLight[0]=14;
        h.assertTrue(!SpawnPlacements.checkSpawnRules(ScorpionModule.SCORPION.get(),daylight,MobSpawnType.NATURAL,pos,level.random),"Artificial block light still spawnproofs wooded badlands");
        var scorpion=h.spawn(ScorpionModule.SCORPION.get(),8,2,8);scorpion.setNoAi(true);var victim=h.spawn(EntityType.PIG,8,2,7);victim.setNoAi(true);
        scorpion.setTarget(victim);scorpion.beginSting(victim);
        h.assertTrue(scorpion.getTarget()==null&&!scorpion.isAttacking()&&!scorpion.doHurtTarget(victim),"Wooded daylight spawns remain passive during the day");h.succeed();
    }
    @GameTest(template="test_empty",batch="scorpion_biomes")
    public static void scorpionNaturalEntriesAreRareSinglesInEveryBadlandsVariant(GameTestHelper h) {
        var allowed=List.of(Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS);
        for(var biome:List.of(Biomes.DESERT,Biomes.BADLANDS,Biomes.ERODED_BADLANDS,Biomes.WOODED_BADLANDS,Biomes.PLAINS,Biomes.FOREST,Biomes.SAVANNA)) {
            h.setBiome(biome);var entries=h.getLevel().getBiome(h.absolutePos(new BlockPos(8,2,8))).value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            boolean found=entries.stream().anyMatch(e->e.type==ScorpionModule.SCORPION.get()&&e.minCount==1&&e.maxCount==1&&e.getWeight().asInt()==(biome==Biomes.WOODED_BADLANDS?30:12));
            h.assertTrue(found==allowed.contains(biome),"Rare singleton entries occur only in badlands variants");
            h.assertTrue(entries.stream().filter(e->e.type==ScorpionModule.SCORPION.get()).count()==(allowed.contains(biome)?1:0),"One scorpion entry per eligible biome, without duplicates");
            h.assertTrue(entries.stream().anyMatch(e->e.type==ScorpionModule.LAVA_SCORPION.get()&&e.minCount==3&&e.maxCount==5&&e.getWeight().asInt()==12),"Lava scorpion native spawn lists use rare packs of three to five");
        }h.succeed();
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void scorpionActualMeleeGoalTelegraphsThenDealsPoisonousSting(GameTestHelper h) {
        var s=spawn(h);s.targetSelector.removeAllGoals(g->true);var p=victim(h,s);s.setTarget(p);boolean[] seen={false,false};int[] started={-1};
        h.onEachTick(()->{
            if(s.isStinging()&&!seen[0]){seen[0]=true;started[0]=s.tickCount;h.assertTrue(p.getHealth()==40,"The wind-up starts before any damage");}
            if(p.getHealth()<40&&!seen[1]){seen[1]=true;h.assertTrue(seen[0]&&s.tickCount-started[0]>=Scorpion.STING_WINDUP,"Sting cannot hit before its wind-up");h.assertTrue(p.hasEffect(MobEffects.POISON),"Successful sting poisons the victim");}
        });
        h.runAfterDelay(35,()->{h.assertTrue(seen[0]&&seen[1],"Real melee AI delivers an animated poisonous sting");h.succeed();});
    }
    @GameTest(template="test_root_worm",timeoutTicks=60)
    public static void scorpionMovingAwayDuringWindupAvoidsTheSting(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var p=victim(h,s);s.beginSting(p);
        h.runAfterDelay(3,()->p.setPos(s.position().add(6,0,0)));
        h.runAfterDelay(20,()->{h.assertTrue(p.getHealth()==40&&!p.hasEffect(MobEffects.POISON),"The strike rechecks range instead of hitting a retreating victim remotely");h.assertTrue(!s.isStinging(),"Sting animation finishes");h.succeed();});
    }
    @GameTest(template="test_root_worm")
    public static void scorpionRejectedDamageDoesNotApplyPoison(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);var p=victim(h,s);p.setInvulnerable(true);
        h.assertTrue(!s.doHurtTarget(p)&&p.getHealth()==40&&!p.hasEffect(MobEffects.POISON),"An invulnerable target gets neither damage nor poison");h.succeed();
    }
    @GameTest(template="test_root_worm")
    public static void scorpionCreativeAndSpectatorPlayersAreProtected(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);
        for(var mode:List.of(GameType.CREATIVE,GameType.SPECTATOR)) {
            var p=h.makeMockPlayer(mode);p.setPos(s.position().add(0,0,1));s.beginSting(p);
            h.assertTrue(!s.isStinging()&&!s.doHurtTarget(p)&&!p.hasEffect(MobEffects.POISON),"Creative and spectator players are never stung");
        }h.succeed();
    }
    @GameTest(template="test_root_worm",timeoutTicks=100)
    public static void scorpionTargetsNearbySurvivalPlayersWithoutBeingProvoked(GameTestHelper h) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"scorpion-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(GameType.SURVIVAL);
        var s=spawn(h);p.moveTo(s.position().add(3,0,0));
        h.runAfterDelay(30,()->{try {h.assertTrue(s.getTarget()==p,"Scorpion naturally targets survival players");h.succeed();}finally{h.getLevel().getServer().getPlayerList().remove(p);}});
    }
    @GameTest(template="test_root_worm")
    public static void scorpionUsesNativeArthropodTagAndResistsItsOwnVenom(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);
        h.assertTrue(s.getType().is(net.minecraft.tags.EntityTypeTags.ARTHROPOD),"Native arthropod enchantment rules recognize scorpions");
        h.assertTrue(!s.addEffect(new MobEffectInstance(MobEffects.POISON,100,0)),"Scorpions resist poison");
        h.assertTrue(s.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100,0)),"Ordinary negative effects still work");h.succeed();
    }
    @GameTest(template="test_root_worm")
    public static void scorpionReloadPreservesHealthWithoutResumingAnOldAttack(GameTestHelper h) {
        var s=spawn(h);s.setNoAi(true);s.setHealth(9);s.beginSting(victim(h,s));var tag=new net.minecraft.nbt.CompoundTag();s.saveWithoutId(tag);
        var copy=ScorpionModule.SCORPION.get().create(h.getLevel());copy.load(tag);
        h.assertTrue(copy.getHealth()==9&&!copy.isStinging()&&copy.getStingAmount(0)==0,"Saved scorpion health survives reload and no stale target is struck");h.succeed();
    }
}
