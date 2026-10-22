package net.itsrileynj.fantasyfeudalismmobs.gametest;

import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BatEcosystemTests {
    private static void floor(GameTestHelper h){for(int x=0;x<24;x++)for(int z=0;z<20;z++)h.setBlock(x,1,z,Blocks.GRASS_BLOCK);}
    private static net.minecraft.world.entity.player.Player player(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.moveTo(h.absoluteVec(new Vec3(3,2,3)));return player;
    }
    private static BatEnclosureBlockEntity home(GameTestHelper h,int x){
        floor(h);h.setBlock(x,2,8,FantasyFeudalismMobs.BAT_ENCLOSURE.get());
        return (BatEnclosureBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(x,2,8)));
    }
    private static Bat bat(GameTestHelper h,int x){var bat=h.spawn(EntityType.BAT,x,3,5);bat.setResting(false);return bat;}
    private static ItemStack capture(GameTestHelper h,Bat bat){
        var player=player(h);var net=new ItemStack(FantasyFeudalismMobs.BAT_NET.get());player.setItemInHand(InteractionHand.MAIN_HAND,net);
        h.assertTrue(BatNetItem.capture(player,InteractionHand.MAIN_HAND,bat).consumesAction(),"Capture succeeds");return net;
    }
    private static int fertilizer(GameTestHelper h){return h.getLevel().getEntitiesOfClass(ItemEntity.class,h.getBounds().inflate(4),
            e->e.getItem().is(FantasyFeudalismMobs.FERTILIZER.get())).stream().mapToInt(e->e.getItem().getCount()).sum();}

    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void staleBatCaptureAndDuplicateTransfersCannotMultiplyBats(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);var bat=bat(h,5);bat.setCustomName(net.minecraft.network.chat.Component.literal("Pip"));bat.setHealth(2);
        UUID id=bat.getUUID();var net=capture(h,bat);var second=player(h);second.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FantasyFeudalismMobs.BAT_NET.get()));
        for(int i=0;i<100;i++)BatNetItem.capture(second,InteractionHand.MAIN_HAND,bat);
        h.assertTrue(BatNetItem.captured(second.getMainHandItem())==null,"Removed bats cannot be captured by stale competing clicks");
        var copy=net.copy();var tag=BatNetItem.captured(net);
        h.assertTrue(tag.getUUID("UUID").equals(id)&&tag.getFloat("Health")==2&&tag.contains("CustomName"),"The net preserves identity, health and name");
        h.assertTrue(home.insert(net)&&BatNetItem.captured(net)==null,"Moving into the enclosure empties the same reusable net");
        h.assertFalse(home.insert(net),"An empty net cannot insert another bat");
        h.assertFalse(home(h,15).insert(copy),"An administrative duplicate of captured NBT cannot claim the same bat at a second home");
        h.assertTrue(home.residentCount()==1&&home.insideCount()==1,"Only the original bat is inside");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void enclosureReservesExactlyThreeOriginalResidents(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);var ids=new HashSet<UUID>();
        for(int n=0;n<3;n++){var bat=bat(h,3+n);ids.add(bat.getUUID());h.assertTrue(home.insert(capture(h,bat)),"The first three bats fit");}
        var fourth=capture(h,bat(h,7));h.assertFalse(home.insert(fourth),"The fourth bat remains in its net");
        h.assertTrue(BatNetItem.captured(fourth)!=null&&new HashSet<>(home.residentIds()).equals(ids),"Capacity failure does not lose or replace a resident");
        h.assertTrue(home.insideCount()==3,"Stored/rendered interior data contains exactly three actual bats");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void applesYieldOneFertilizerPerBatAndDoNotBypassCooldown(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);for(int n=0;n<3;n++)home.insert(capture(h,bat(h,3+n)));
        var player=player(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,8));
        for(int n=0;n<3;n++)h.assertTrue(home.feed(player,InteractionHand.MAIN_HAND),"Each ready bat accepts its own apple");
        h.assertTrue(fertilizer(h)==3&&player.getMainHandItem().getCount()==5,"Three apples produce exactly three dropped fertilizers");
        for(int n=0;n<100;n++)h.assertFalse(home.feed(player,InteractionHand.MAIN_HAND),"A full-health bat on cooldown does not consume another apple");
        var saved=home.saveWithoutMetadata(h.getLevel().registryAccess());home.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertFalse(home.feed(player,InteractionHand.MAIN_HAND),"Block-entity save/load preserves all fertilizer cooldowns");
        h.assertTrue(fertilizer(h)==3&&player.getMainHandItem().getCount()==5,"Reload and stale feed requests cannot duplicate output");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void feedingHealsOnCooldownWithoutProducingAnotherFertilizer(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);var bat=bat(h,5);bat.setHealth(1);home.insert(capture(h,bat));
        var player=player(h);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,3));
        home.feed(player,InteractionHand.MAIN_HAND);home.feed(player,InteractionHand.MAIN_HAND);
        var stored=home.saveWithoutMetadata(h.getLevel().registryAccess()).getList("Bats",10).getCompound(0).getCompound("Entity");
        h.assertTrue(stored.getFloat("Health")==6,"Apples heal the actual stored bat");
        h.assertTrue(fertilizer(h)==1&&player.getMainHandItem().getCount()==1,"Healing during cooldown consumes an apple but adds no fertilizer");
        h.assertTrue(stored.getCompound("NeoForgeData").getLong(BatFarming.NEXT)==h.getLevel().getGameTime()+6000,"The first output establishes exactly five minutes, with no extension from healing");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void breakingAnOccupiedEnclosureReleasesOnlyItsActualBats(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);var ids=new HashSet<UUID>();
        for(int n=0;n<3;n++){var bat=bat(h,3+n);ids.add(bat.getUUID());home.insert(capture(h,bat));}
        h.setBlock(8,2,8,Blocks.AIR);
        var live=h.getLevel().getEntitiesOfClass(Bat.class,h.getBounds());
        h.assertTrue(live.size()==3&&new HashSet<>(live.stream().map(Bat::getUUID).toList()).equals(ids),"Breaking the block releases the three original identities exactly once");
        h.assertTrue(live.stream().noneMatch(BatResidents::housed),"Released bats are no longer attached to a destroyed home");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_flight",timeoutTicks=900)
    public static void actualResidentsLeaveAtNightStayNearbyAndReturnAtDawn(GameTestHelper h){
        var home=home(h,12);var ids=new HashSet<UUID>();for(int n=0;n<3;n++){var bat=bat(h,3+n);ids.add(bat.getUUID());home.insert(capture(h,bat));}
        h.getLevel().setDayTime(18000);boolean[] released={false},dawn={false};double[] farthest={0};
        h.onEachTick(()->{
            for(UUID id:ids)if(h.getLevel().getEntity(id) instanceof Bat bat)farthest[0]=Math.max(farthest[0],bat.position().distanceTo(Vec3.atCenterOf(home.getBlockPos())));
            if(home.insideCount()==0&&ids.stream().allMatch(id->h.getLevel().getEntity(id) instanceof Bat))released[0]=true;
        });
        h.runAfterDelay(220,()->{
            h.assertTrue(released[0],"Three stored bats become three real flying bats at night");
            h.assertTrue(home.residentCount()==3&&home.insideCount()==0,"Outside bats keep reservations but are not represented as hanging inside");
            h.assertTrue(farthest[0]<=15.5,"Night flight remains within the enclosure's fifteen-block area");
            dawn[0]=true;h.getLevel().setDayTime(24000);
        });
        h.succeedWhen(()->{
            h.assertTrue(dawn[0]&&home.insideCount()==3,"Ordinary world AI returns all three bats at dawn");
            h.assertTrue(ids.stream().noneMatch(id->h.getLevel().getEntity(id)!=null),"Returning bats are stored, with no duplicate live entities");
            h.assertTrue(new HashSet<>(home.residentIds()).equals(ids),"Returning does not replace the original residents");
        });
    }
    @GameTest(template="test_empty",batch="ffm_bat_blocked",timeoutTicks=220)
    public static void blockedExitsKeepBatsSafeUntilAnExitOpens(GameTestHelper h){
        var home=home(h,8);var bat=bat(h,5);UUID id=bat.getUUID();home.insert(capture(h,bat));
        for(int x=7;x<=9;x++)for(int y=2;y<=4;y++)for(int z=7;z<=9;z++)if(!(x==8&&y==2&&z==8))h.setBlock(x,y,z,Blocks.STONE);
        h.getLevel().setDayTime(18000);
        h.runAfterDelay(35,()->{
            h.assertTrue(home.insideCount()==1&&h.getLevel().getEntity(id)==null,"Blocked departures never lose the stored bat");
            for(int y=2;y<=4;y++)h.setBlock(8,y,7,Blocks.AIR);
        });
        h.succeedWhen(()->h.assertTrue(home.insideCount()==0&&h.getLevel().getEntity(id) instanceof Bat,"Opening a real exit releases the original bat"));
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void fertilizerMaturesVanillaCropsWithoutSpendingOnMaturePlants(GameTestHelper h){
        h.getLevel().setDayTime(6000);floor(h);
        Block[] crops={Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS,Blocks.TORCHFLOWER_CROP};
        for(int n=0;n<crops.length;n++){
            h.setBlock(3+n*3,1,5,Blocks.FARMLAND);h.setBlock(3+n*3,2,5,crops[n]);var pos=h.absolutePos(new BlockPos(3+n*3,2,5));
            h.assertTrue(FertilizerItem.grow(h.getLevel(),pos),"Fertilizer instantly grows "+crops[n]);var mature=h.getLevel().getBlockState(pos);
            h.assertTrue(mature.is(Blocks.TORCHFLOWER)||((CropBlock)mature.getBlock()).isMaxAge(mature),"The crop reaches its final state");
            h.assertFalse(FertilizerItem.grow(h.getLevel(),pos),"A mature crop cannot consume another growth action");
        }
        h.setBlock(4,1,10,Blocks.SOUL_SAND);h.setBlock(4,2,10,Blocks.NETHER_WART);h.setBlock(8,2,10,Blocks.SWEET_BERRY_BUSH);
        for(int x:new int[]{4,8})h.assertTrue(FertilizerItem.grow(h.getLevel(),h.absolutePos(new BlockPos(x,2,10))),"Wart and berry crops mature too");
        h.assertTrue(h.getBlockState(new BlockPos(4,2,10)).getValue(NetherWartBlock.AGE)==3&&h.getBlockState(new BlockPos(8,2,10)).getValue(SweetBerryBushBlock.AGE)==3,"Both crops reach maximum age");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void fertilizerRespectsPitcherClearanceAndBothHalves(GameTestHelper h){
        h.getLevel().setDayTime(6000);floor(h);h.setBlock(8,1,8,Blocks.FARMLAND);h.setBlock(8,2,8,Blocks.PITCHER_CROP);h.setBlock(8,3,8,Blocks.STONE);
        var pos=h.absolutePos(new BlockPos(8,2,8));h.assertFalse(FertilizerItem.grow(h.getLevel(),pos),"Fertilizer cannot grow a pitcher through a solid ceiling");
        h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(PitcherCropBlock.AGE)==0,"Blocked growth is atomic, not a partially spent action");
        h.setBlock(8,3,8,Blocks.AIR);h.assertTrue(FertilizerItem.grow(h.getLevel(),pos),"A pitcher grows when it has room");
        h.assertTrue(h.getBlockState(new BlockPos(8,2,8)).getValue(PitcherCropBlock.AGE)==4
                &&h.getBlockState(new BlockPos(8,3,8)).getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER,"Both mature pitcher halves exist");h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day",timeoutTicks=100)
    public static void recapturingAndMovingABatKeepsItsFertilizerDeadline(GameTestHelper h){
        h.getLevel().setDayTime(6000);var home=home(h,8);var bat=bat(h,5);
        long deadline=h.getLevel().getGameTime()+40;bat.getPersistentData().putLong(BatFarming.NEXT,deadline);
        var net=capture(h,bat);home.insert(net);h.setBlock(8,2,8,Blocks.AIR);
        var released=(Bat)h.getLevel().getEntity(bat.getUUID());
        var moved=home(h,15);moved.insert(capture(h,released));var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,2));
        h.assertFalse(moved.feed(p,InteractionHand.MAIN_HAND),"Moving homes does not reset the per-bat cooldown");
        h.succeedWhen(()->{
            h.assertTrue(h.getLevel().getGameTime()>=deadline,"The original deadline has elapsed");
            h.assertTrue(moved.feed(p,InteractionHand.MAIN_HAND),"The original resident becomes ready again");
            h.assertTrue(fertilizer(h)==1&&p.getMainHandItem().getCount()==1,"Exactly one new fertilizer and one apple spent after cooldown");
        });
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void fertilizerMaturesFruitCocoaCaneAndCactus(GameTestHelper h){
        h.getLevel().setDayTime(6000);floor(h);
        h.setBlock(3,1,5,Blocks.FARMLAND);h.setBlock(3,2,5,Blocks.MELON_STEM);
        h.assertTrue(FertilizerItem.grow(h.getLevel(),h.absolutePos(new BlockPos(3,2,5))),"A melon stem produces fruit immediately when there is space");
        h.assertTrue(h.getBlockState(new BlockPos(3,2,5)).is(Blocks.ATTACHED_MELON_STEM),"The stem attaches to its actual fruit");
        h.setBlock(8,2,5,Blocks.JUNGLE_LOG);h.setBlock(8,2,6,Blocks.COCOA);
        h.assertTrue(FertilizerItem.grow(h.getLevel(),h.absolutePos(new BlockPos(8,2,6)))&&h.getBlockState(new BlockPos(8,2,6)).getValue(CocoaBlock.AGE)==2,"Cocoa reaches maximum age");
        h.setBlock(11,1,4,Blocks.WATER);h.setBlock(11,2,5,Blocks.SUGAR_CANE);
        h.setBlock(15,1,5,Blocks.SAND);h.setBlock(15,2,5,Blocks.CACTUS);
        for(int x:new int[]{11,15}){
            var at=new BlockPos(x,2,5);h.assertTrue(FertilizerItem.grow(h.getLevel(),h.absolutePos(at)),"Column crop grows instantly");
            h.assertTrue(h.getBlockState(at.above(2)).is(x==11?Blocks.SUGAR_CANE:Blocks.CACTUS),"The column reaches three blocks");
            h.assertFalse(FertilizerItem.grow(h.getLevel(),h.absolutePos(at.above())),"Mature columns do not spend fertilizer");
        }h.succeed();
    }
    @GameTest(template="test_empty",batch="ffm_bat_day")
    public static void craftingRecipesUseTheRequestedNineSlots(GameTestHelper h){
        var net=CraftingInput.of(3,3,List.of(new ItemStack(Items.STRING),new ItemStack(Items.STRING),new ItemStack(Items.STRING),
                new ItemStack(Items.STRING),new ItemStack(Items.IRON_NUGGET),new ItemStack(Items.STRING),new ItemStack(Items.STRING),new ItemStack(Items.STICK),new ItemStack(Items.STRING)));
        var cage=CraftingInput.of(3,3,List.of(new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_FENCE),new ItemStack(Items.APPLE),new ItemStack(Items.OAK_FENCE),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS),new ItemStack(Items.OAK_PLANKS)));
        var manager=h.getLevel().getRecipeManager();
        h.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,net,h.getLevel()).orElseThrow().value().getResultItem(h.getLevel().registryAccess()).is(FantasyFeudalismMobs.BAT_NET),"Bat Net uses seven string, center iron nugget and bottom-center stick");
        h.assertTrue(manager.getRecipeFor(RecipeType.CRAFTING,cage,h.getLevel()).orElseThrow().value().getResultItem(h.getLevel().registryAccess()).is(FantasyFeudalismMobs.BAT_ENCLOSURE_ITEM),"Enclosure uses six planks, left/right fences and center apple");h.succeed();
    }
}
