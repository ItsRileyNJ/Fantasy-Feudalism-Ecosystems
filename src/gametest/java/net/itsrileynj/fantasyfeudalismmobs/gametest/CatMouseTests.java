package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.UUID;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CatMouseTests {
    public static final class GriefProbe {
        private final UUID mouse;int checks;
        GriefProbe(UUID mouse){this.mouse=mouse;}
        @net.neoforged.bus.api.SubscribeEvent public void check(net.neoforged.neoforge.event.entity.EntityMobGriefingEvent event){
            if(event.getEntity().getUUID().equals(mouse))checks++;
        }
    }
    @GameTest(template="test_empty",batch="mouse_throttle",timeoutTicks=580)
    public static void failedBitesBackOffButResumeAfterGriefingIsEnabled(GameTestHelper h){
        floor(h);var level=h.getLevel();boolean old=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        var pos=h.absolutePos(new BlockPos(8,2,8));h.setBlock(8,1,8,Blocks.FARMLAND);
        h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),7,2,8);mouse.setCropHome(pos);
        var probe=new GriefProbe(mouse.getUUID());net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(probe);
        int[] denied={0};
        h.runAtTickTime(350,()->{denied[0]=probe.checks;level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());});
        h.runAtTickTime(520,()->{
            try{
                h.assertTrue(denied[0]==1,"A denied crop is not retried before twenty seconds: "+denied[0]);
                h.assertTrue(level.getBlockState(pos).getValue(CropBlock.AGE)==6,"Mouse resumes ordinary feeding when permission returns");
                h.assertTrue(probe.checks==2&&mouse.nextMeal()>level.getGameTime(),"The next twenty-second check succeeds and enters its full saved cooldown");h.succeed();
            }finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(probe);level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(old,level.getServer());}
        });
    }
    @GameTest(template="test_empty",batch="mouse_search_fairness",timeoutTicks=260)
    public static void sharedSearchBudgetStillLetsEveryMouseFindItsCrop(GameTestHelper h){
        floor(h);var positions=new java.util.ArrayList<BlockPos>();var mice=new java.util.ArrayList<FieldMouse>();
        for(int x:new int[]{2,6,10,14})for(int z:new int[]{2,6,10,14}){
            h.setBlock(x+1,1,z,Blocks.FARMLAND);h.setBlock(x+1,2,z,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
            positions.add(h.absolutePos(new BlockPos(x+1,2,z)));mice.add(h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),x,2,z));
        }
        h.runAtTickTime(200,()->{
            // Crop choice is shared; fairness means every mouse feeds, not one mouse assigned to each plant.
            for(FieldMouse mouse:mice)h.assertTrue(mouse.nextMeal()>h.getLevel().getGameTime(),
                    "Mouse waiting for food: position="+mouse.position()+" home="+mouse.cropHome()+" deadline="+mouse.nextMeal()+" path="+mouse.getNavigation().getPath());
            h.assertTrue(positions.stream().anyMatch(pos->h.getLevel().getBlockState(pos).getValue(CropBlock.AGE)<7),"Actual crop stages were eaten");
            h.succeed();
        });
    }
    @GameTest(template="test_empty",batch="mouse_morning_queue",timeoutTicks=80)
    public static void morningSpawnsAreSpreadAcrossTicksAndPendingWorkSurvivesSaveLoad(GameTestHelper h){
        floor(h);var level=h.getLevel();var original=MouseVillages.get(level);long oldTime=level.getDayTime();
        boolean oldSpawn=level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);var home=bed(h,8);
        level.getPoiManager().add(home,level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE).getHolderOrThrow(PoiTypes.HOME));
        level.getPoiManager().take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(home),home,1);
        for(int x=4;x<14;x++)for(int z=11;z<15;z++){h.setBlock(x,1,z,Blocks.FARMLAND);h.setBlock(x,2,z,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));}
        for(int x=5;x<11;x++){var cat=h.spawn(EntityType.CAT,x,2,5);cat.setNoAi(true);}
        var fresh=new MouseVillages();level.getDataStorage().set("ffm_field_mice",fresh);
        long day=Math.max(original.lastMorning()+10,10000);level.setDayTime(day*24000);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());
        int first=fresh.morning(level),sameTick=fresh.morning(level);
        var saved=fresh.save(new CompoundTag(),level.registryAccess());
        var restored=MouseVillages.load(saved,level.registryAccess());level.getDataStorage().set("ffm_field_mice",restored);
        h.runAtTickTime(20,()->{
            try{
                h.assertTrue(first==1&&sameTick==0,"Six cats cannot create both mice on one server tick");
                h.assertTrue(saved.getList("Pending",net.minecraft.nbt.Tag.TAG_COMPOUND).size()==1,"The remaining morning job is serialized");
                h.assertTrue(restored.population(java.util.List.of(home))==2,"Restored work creates the second mouse without duplicating the first");
                h.assertTrue(restored.lastMorning()==day&&restored.morning(level)==0,"Finishing a saved job cannot restart the morning census");h.succeed();
            }finally{
                for(var entry:restored.save(new CompoundTag(),level.registryAccess()).getList("Mice",net.minecraft.nbt.Tag.TAG_COMPOUND)){
                    var entity=level.getEntity(((CompoundTag)entry).getUUID("Id"));if(entity!=null)entity.discard();
                }
                level.getDataStorage().set("ffm_field_mice",original);level.getPoiManager().remove(home);level.setDayTime(oldTime);
                level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(oldSpawn,level.getServer());
            }
        });
    }
    private static java.util.List<net.minecraft.sounds.SoundEvent> soundPackets(ServerPlayer player){
        var channel=(io.netty.channel.embedded.EmbeddedChannel)player.connection.getConnection().channel();channel.runPendingTasks();
        var sounds=new java.util.ArrayList<net.minecraft.sounds.SoundEvent>();Object packet;
        while((packet=channel.readOutbound())!=null){
            if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundPacket sound)sounds.add(sound.getSound().value());
            else if(packet instanceof net.minecraft.network.protocol.game.ClientboundSoundEntityPacket sound)sounds.add(sound.getSound().value());
            io.netty.util.ReferenceCountUtil.release(packet);
        }
        return sounds;
    }
    @GameTest(template="test_empty",batch="mouse_sounds")
    public static void mouseUsesAnimalAmbientTimingAndBroadcastsAmbientHurtAndFatalSounds(GameTestHelper h){
        var p=player(h);var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setNoAi(true);
        try{
            soundPackets(p);mouse.ambientSoundTime=1000;mouse.baseTick();
            h.assertTrue(soundPackets(p).contains(FantasyFeudalismMobs.MOUSE_AMBIENT.get()),"Native random ambient scheduler broadcasts the mouse ambient event to nearby players");
            h.assertTrue(mouse.ambientSoundTime==-EntityType.COW.create(h.getLevel()).getAmbientSoundInterval(),"Ambient cooldown matches a vanilla animal");
            mouse.hurt(h.getLevel().damageSources().generic(),.5F);
            h.assertTrue(soundPackets(p).contains(FantasyFeudalismMobs.MOUSE_HURT.get()),"Nonfatal damage broadcasts a hurt sound");
            mouse.setSilent(true);mouse.ambientSoundTime=1000;mouse.baseTick();mouse.invulnerableTime=0;mouse.hurt(h.getLevel().damageSources().generic(),.5F);
            var silent=soundPackets(p);
            h.assertFalse(silent.contains(FantasyFeudalismMobs.MOUSE_AMBIENT.get())||silent.contains(FantasyFeudalismMobs.MOUSE_HURT.get()),"Native Silent suppresses ambient and damage sounds");
            mouse.setSilent(false);mouse.invulnerableTime=0;mouse.hurt(h.getLevel().damageSources().generic(),10);
            h.assertFalse(mouse.isAlive(),"Final hit is fatal");
            h.assertTrue(soundPackets(p).contains(FantasyFeudalismMobs.MOUSE_HURT.get()),"Fatal damage also uses the supplied hurt bank");h.succeed();
        }finally{remove(p);}
    }
    private static ServerPlayer player(GameTestHelper h){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"cat-test"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.moveTo(h.absoluteVec(new Vec3(8,2,8)));return p;
    }
    private static void remove(ServerPlayer p){p.getServer().getPlayerList().remove(p);}
    private static void floor(GameTestHelper h){for(int x=0;x<18;x++)for(int z=0;z<18;z++)h.setBlock(x,1,z,Blocks.STONE);}
    private static BlockPos bed(GameTestHelper h,int x){
        h.setBlock(x,2,8,Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.HEAD));
        h.setBlock(x,2,9,Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.PART,BedPart.FOOT));
        return h.absolutePos(new BlockPos(x,2,8));
    }
    private static void rested(ServerPlayer p){p.addEffect(new MobEffectInstance(FantasyFeudalismMobs.RESTED,RestedEffect.DURATION));}

    @GameTest(template="test_empty",batch="cat_food")
    public static void restedHalvesExhaustionAndMilkRemovalRestoresNormalRate(GameTestHelper h){
        var p=player(h);
        try{
            var food=p.getFoodData();food.tick(p);food.setExhaustion(0);rested(p);
            p.causeFoodExhaustion(4);h.assertTrue(Math.abs(food.getExhaustionLevel()-2)<.0001,"Rested halves exhaustion from ordinary activity");
            h.assertTrue(p.getEffect(FantasyFeudalismMobs.RESTED).getDuration()==7200,"Six-minute duration");
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MILK_BUCKET).finishUsingItem(h.getLevel(),p);
            h.assertFalse(p.hasEffect(FantasyFeudalismMobs.RESTED),"Native milk removes Rested");
            food.setExhaustion(0);p.causeFoodExhaustion(4);h.assertTrue(food.getExhaustionLevel()==4,"Normal hunger returns when the effect ends");h.succeed();
        }finally{remove(p);}
    }
    @GameTest(template="test_empty",batch="cat_regen")
    public static void restedBoostsBothNativeRegenerationPathsButNotOtherHealing(GameTestHelper h){
        var p=player(h);var level=h.getLevel();boolean rule=level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
        try{
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true,level.getServer());rested(p);
            var food=p.getFoodData();p.setHealth(5);food.setFoodLevel(20);food.setSaturation(6);
            for(int i=0;i<10;i++)food.tick(p);
            h.assertTrue(Math.abs(p.getHealth()-6.35F)<.0001,"Saturated natural healing increases by exactly 35%");
            food.setExhaustion(0);food.setFoodLevel(18);food.setSaturation(0);p.setHealth(5);
            for(int i=0;i<80;i++)food.tick(p);
            h.assertTrue(Math.abs(p.getHealth()-6.35F)<.0001,"Ordinary natural healing increases by exactly 35%");
            p.setHealth(5);p.heal(2);h.assertTrue(p.getHealth()==7,"Other healing is unchanged");
            level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,level.getServer());
            food.setFoodLevel(20);food.setSaturation(6);food.setExhaustion(0);p.setHealth(5);
            for(int i=0;i<100;i++)food.tick(p);
            h.assertTrue(p.getHealth()==5,"Rested cannot bypass naturalRegeneration=false");h.succeed();
        }finally{level.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(rule,level.getServer());remove(p);}
    }
    @GameTest(template="test_empty",batch="cat_sleep",timeoutTicks=200)
    public static void ownCatAndStrayActuallyJoinBedsAndGrantRestedOnWake(GameTestHelper h){
        floor(h);var level=h.getLevel();long dayTime=level.getDayTime();level.setDayTime(18000);
        int percent=level.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        level.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(101,level.getServer());
        var owner=player(h);var other=player(h);var bed1=bed(h,4);var bed2=bed(h,13);
        var owned=h.spawn(EntityType.CAT,2,2,9);owned.setTame(true,true);owned.setOwnerUUID(owner.getUUID());owned.setOrderedToSit(false);
        var stray=h.spawn(EntityType.CAT,15,2,9);
        owner.startSleeping(bed1);other.startSleeping(bed2);
        // Embedded test connections do not deliver normal client-driven doTick calls.
        h.onEachTick(()->{if(owner.isSleeping())owner.doTick();if(other.isSleeping())other.doTick();});
        h.assertTrue(CatRest.candidate(owned,owner)&&CatRest.candidate(stray,other),"Eligible nearby cats captured when sleep starts");
        h.runAtTickTime(160,()->{
            try{
                h.assertTrue(CatRest.sharingBed(owned,owner),"Owned cat really settles on its owner's bed: "+owned.position()+" lying="+owned.isLying());
                h.assertTrue(CatRest.sharingBed(stray,other),"Stray really settles on the other bed: "+stray.position()+" lying="+stray.isLying());
                h.assertTrue(owner.getSleepTimer()>=100&&other.getSleepTimer()>=100,"Players actually slept: "+owner.getSleepTimer()+", "+other.getSleepTimer());
                owner.stopSleepInBed(false,true);other.stopSleepInBed(false,true);
                h.assertTrue(owner.hasEffect(FantasyFeudalismMobs.RESTED)&&other.hasEffect(FantasyFeudalismMobs.RESTED),"Both valid sleepers receive Rested");
                h.assertTrue(AdvancementTests.complete(owner,"cat_nap")&&AdvancementTests.complete(other,"cat_nap"),"Actual cat-assisted sleep awards Cat Nap to both eligible sleepers");h.succeed();
            }finally{
                level.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(percent,level.getServer());remove(owner);remove(other);
                level.setDayTime(dayTime);
            }
        });
    }
    @GameTest(template="test_empty",batch="cat_sleep_rules")
    public static void sleepExcludesOtherOwnersDistantCatsAndBriefBedVisits(GameTestHelper h){
        floor(h);var p=player(h);var pos=bed(h,8);
        try{
            var stray=h.spawn(EntityType.CAT,7,2,8);
            var foreign=h.spawn(EntityType.CAT,8,2,7);foreign.setTame(true,true);foreign.setOwnerUUID(UUID.randomUUID());
            var far=h.spawn(EntityType.CAT,14,2,8);
            var sitting=h.spawn(EntityType.CAT,9,2,8);sitting.setTame(true,true);sitting.setOwnerUUID(p.getUUID());sitting.setOrderedToSit(true);
            p.startSleeping(pos);
            h.assertTrue(CatRest.candidate(stray,p),"Strays may join");
            h.assertFalse(CatRest.candidate(foreign,p),"Another player's pet may not grant the benefit");
            h.assertFalse(CatRest.candidate(far,p),"Cats beyond four blocks are excluded");
            h.assertFalse(CatRest.candidate(sitting,p),"A sit order is honored");
            stray.setPos(p.getX()+4,p.getY(),p.getZ());CatRest.begin(p);
            h.assertTrue(CatRest.candidate(stray,p),"Exactly four blocks is inside the sleep radius");
            stray.setPos(p.getX()+4.01,p.getY(),p.getZ());CatRest.begin(p);
            h.assertFalse(CatRest.candidate(stray,p),"Beyond four blocks is outside the sleep radius");
            p.stopSleepInBed(true,true);h.assertFalse(p.hasEffect(FantasyFeudalismMobs.RESTED),"Brief bed visits cannot grant Rested");h.succeed();
        }finally{remove(p);}
    }
    @GameTest(template="test_empty")
    public static void mouseHasTwoHeartsIsSmallerThanSilverfishAndDiesToOneCatHit(GameTestHelper h){
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setNoAi(true);
        var cat=h.spawn(EntityType.CAT,7,2,8);cat.setNoAi(true);
        cat.setSprinting(true);
        h.assertTrue(mouse.getMaxHealth()==4,"Mouse has two hearts");
        h.assertTrue(mouse.getBbWidth()<EntityType.SILVERFISH.getWidth()&&mouse.getBbHeight()<EntityType.SILVERFISH.getHeight(),"Mouse body is smaller than a silverfish");
        h.assertTrue(Math.abs(mouse.getAttributeValue(Attributes.MOVEMENT_SPEED)/(cat.getAttributeValue(Attributes.MOVEMENT_SPEED)*FieldMouse.CAT_CHASE_SPEED)-.9)<.00001,"Escape movement speed is 90% of cat chase speed");
        cat.doHurtTarget(mouse);h.assertFalse(mouse.isAlive(),"One actual melee hit from a cat kills a full-health mouse");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=200)
    public static void actualCatHuntKillsNearbyMouseAndSittingCatsDoNotHunt(GameTestHelper h){
        floor(h);var cat=h.spawn(EntityType.CAT,3,2,8);var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),11,2,8);mouse.setNoAi(true);
        cat.setTame(true,true);cat.setOwnerUUID(UUID.randomUUID());cat.setOrderedToSit(true);
        h.assertFalse(new CatMouseHuntGoal(cat).canUse(),"Sitting pet honors its order");
        cat.setOrderedToSit(false);cat.setInSittingPose(false);cat.setTame(false,true);
        h.succeedWhen(()->h.assertFalse(mouse.isAlive(),"Cat hunt: cat="+cat.position()+" mouse="+mouse.position()+" health="+mouse.getHealth()+" target="+cat.getTarget()+" path="+cat.getNavigation().getPath()+" canAttack="+cat.canAttack(mouse)+" sitting="+cat.isOrderedToSit()));
    }
    @GameTest(template="test_empty")
    public static void catHuntRangeIsSixteenBlocks(GameTestHelper h){
        var cat=h.spawn(EntityType.CAT,1,2,8);cat.setNoAi(true);
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),17,2,8);mouse.setNoAi(true);
        h.assertTrue(new CatMouseHuntGoal(cat).canUse(),"A mouse exactly sixteen blocks away can be hunted");
        mouse.setPos(mouse.getX()+.1,mouse.getY(),mouse.getZ());
        h.assertFalse(new CatMouseHuntGoal(cat).canUse(),"A mouse beyond sixteen blocks is not acquired");h.succeed();
    }
    @GameTest(template="test_empty",batch="mouse_crops")
    public static void mouseEatsExactlyOneStageHonorsGriefingAndPersistsCooldown(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(8,2,8));h.setBlock(8,1,8,Blocks.FARMLAND);
        h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setNoAi(true);
        boolean grief=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        try{
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
            h.assertFalse(mouse.eatCrop(pos),"mobGriefing=false blocks crop damage");
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
            h.assertTrue(mouse.eatCrop(pos)&&level.getBlockState(pos).getValue(CropBlock.AGE)==6,"Exactly one stage is eaten");
            h.assertTrue(mouse.nextMeal()==level.getGameTime()+1800,"Cooldown is exactly ninety seconds of world ticks");
            h.assertFalse(mouse.eatCrop(pos),"A second bite before the deadline is rejected");
            var restored=FantasyFeudalismMobs.FIELD_MOUSE.get().create(level);restored.load(mouse.saveWithoutId(new CompoundTag()));
            h.assertTrue(restored.nextMeal()==mouse.nextMeal()&&!restored.eatCrop(pos),"Save/load preserves the individual cooldown");
            var second=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),9,2,8);second.setNoAi(true);
            h.assertTrue(second.eatCrop(pos)&&level.getBlockState(pos).getValue(CropBlock.AGE)==5,"Another mouse has an independent meal timer");h.succeed();
        }finally{level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());}
    }
    @GameTest(template="test_empty",timeoutTicks=1850,batch="mouse_full_cooldown")
    public static void mouseCanEatAgainOnlyAfterNinetySeconds(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,2,8));h.setBlock(8,1,8,Blocks.FARMLAND);h.setBlock(8,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.setNoAi(true);
        h.assertTrue(mouse.eatCrop(pos),"First bite succeeds");
        h.runAtTickTime(1798,()->h.assertFalse(mouse.eatCrop(pos),"A bite before ninety seconds is rejected"));
        h.runAtTickTime(1802,()->{h.assertTrue(mouse.eatCrop(pos),"Bite after the deadline succeeds");h.assertTrue(h.getLevel().getBlockState(pos).getValue(CropBlock.AGE)==5,"Only two stages eaten");h.succeed();});
    }
    @GameTest(template="test_empty",batch="mouse_pitcher")
    public static void mouseReducesBothPitcherHalvesWithoutBreakingOrDroppingThePlant(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(8,2,8));h.setBlock(8,1,8,Blocks.FARMLAND);
        h.setBlock(8,2,8,Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,4));
        h.setBlock(8,3,8,Blocks.PITCHER_CROP.defaultBlockState().setValue(PitcherCropBlock.AGE,4).setValue(PitcherCropBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var first=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),7,2,8);first.setNoAi(true);
        h.assertTrue(first.eatCrop(pos),"Mouse can eat a crop with its own collision shape");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PitcherCropBlock.AGE)==3&&h.getLevel().getBlockState(pos.above()).getValue(PitcherCropBlock.AGE)==3,"Both pitcher halves lose exactly one age stage");
        var second=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),9,2,8);second.setNoAi(true);
        h.assertTrue(second.eatCrop(pos),"Another eligible mouse can reduce the plant to a single block");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.PITCHER_CROP)&&h.getLevel().getBlockState(pos).getValue(PitcherCropBlock.AGE)==2&&h.getLevel().getBlockState(pos.above()).isAir(),"The young lower crop survives and only its upper half disappears");
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(3)).isEmpty(),"Eating stages cannot create plant drops");h.succeed();
    }
    @GameTest(template="test_empty",timeoutTicks=300,batch="mouse_foraging")
    public static void mouseReturnsToRememberedCropAfterBeingMovedAway(GameTestHelper h){
        floor(h);h.setBlock(12,1,8,Blocks.FARMLAND);h.setBlock(12,2,8,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),3,2,8);mouse.setCropHome(h.absolutePos(new BlockPos(12,2,8)));
        h.succeedWhen(()->h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(12,2,8))).getValue(CropBlock.AGE)==6,"Forage: mouse="+mouse.position()+" path="+mouse.getNavigation().getPath()+" home="+mouse.cropHome()+" next="+mouse.nextMeal()+" time="+h.getLevel().getGameTime()));
    }
    @GameTest(template="test_empty",timeoutTicks=120,batch="mouse_flee")
    public static void mouseRunsFromNearbyVillager(GameTestHelper h){
        floor(h);var mouse=h.spawn(FantasyFeudalismMobs.FIELD_MOUSE.get(),8,2,8);mouse.getRandom().setSeed(4);
        var villager=h.spawn(EntityType.VILLAGER,10,2,8);villager.setNoAi(true);
        h.succeedWhen(()->h.assertTrue(mouse.distanceToSqr(villager)>25,"Villager escape: mouse="+mouse.position()+" villager="+villager.position()+" distance="+mouse.distanceTo(villager)+" path="+mouse.getNavigation().getPath()));
    }
    @GameTest(template="test_empty",batch="mouse_morning",timeoutTicks=120)
    public static void villageMiceSpawnOnlyAtMorningRatioAndDoNotDuplicateAfterUnload(GameTestHelper h){
        floor(h);var level=h.getLevel();long previous=level.getDayTime();boolean spawning=level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        var home=bed(h,8);var pois=level.getPoiManager();
        pois.add(home,level.registryAccess().registryOrThrow(Registries.POINT_OF_INTEREST_TYPE).getHolderOrThrow(PoiTypes.HOME));
        pois.take(type->type.is(PoiTypes.HOME),(type,at)->at.equals(home),home,1);
        for(int x=5;x<=11;x++)for(int z=11;z<=14;z++){h.setBlock(x,1,z,Blocks.FARMLAND);h.setBlock(x,2,z,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));}
        var a=h.spawn(EntityType.CAT,6,2,6);a.setNoAi(true);var b=h.spawn(EntityType.CAT,7,2,6);b.setNoAi(true);
        try{
            h.assertTrue(level.isVillage(home),"An occupied bed establishes the test village");
            var data=MouseVillages.get(level);long day=Math.max(data.lastMorning()+1,100);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,level.getServer());level.setDayTime(day*24000);
            h.assertTrue(data.morning(level)==0,"Fewer than three cats spawn no mice");
            var c=h.spawn(EntityType.CAT,9,2,6);c.setNoAi(true);
            h.assertTrue(data.morning(level)==0,"Adding cats cannot trigger another attempt on the same morning");
            level.setDayTime((day+1)*24000+6000);h.assertTrue(data.morning(level)==0,"No daytime spawning");
            level.setDayTime((day+2)*24000);h.assertTrue(data.morning(level)==1,"Three cats produce one mouse on the next eligible morning");
            var mice=level.getEntitiesOfClass(FieldMouse.class,new net.minecraft.world.phys.AABB(home).inflate(40));
            h.assertTrue(mice.size()==1,"Exactly one mouse belongs to this village");
            mice.getFirst().setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            level.setDayTime((day+3)*24000);h.assertTrue(data.morning(level)==0,"An unloaded mouse still occupies its saved population slot");
            h.assertTrue(data.save(new CompoundTag(),level.registryAccess()).getLong("LastMorning")==day+3,"Daily attempt stamp is saved");h.succeed();
        }finally{pois.remove(home);level.setDayTime(previous);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(spawning,level.getServer());}
    }
}
