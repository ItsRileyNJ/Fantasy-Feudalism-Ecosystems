package net.itsrileynj.fantasyfeudalismmobs.gametest.rootworm;

import java.util.List;
import java.util.function.Consumer;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.BarkCompatibility;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(FantasyFeudalismMobs.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BarkTests {
    private static UseOnContext context(GameTestHelper h, net.minecraft.world.entity.player.Player player) {
        var pos=h.absolutePos(new BlockPos(8,2,8));
        player.setPos(Vec3.atCenterOf(pos).add(0,0,2));
        return new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private static int barkCount(GameTestHelper h) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(h.absolutePos(new BlockPos(8,2,8))).inflate(3),e->e.getItem().is(FantasyFeudalismMobs.BARK.get()))
                .stream().mapToInt(e->e.getItem().getCount()).sum();
    }
    @GameTest(template="test_empty")
    public static void barkAllVanillaStrippingDropsExactlyOneAndKeepsAxis(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var axe=new ItemStack(Items.DIAMOND_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        var context=context(h,player);int count=0;
        for(var block:List.of(Blocks.OAK_LOG,Blocks.OAK_WOOD,Blocks.SPRUCE_LOG,Blocks.SPRUCE_WOOD,Blocks.BIRCH_LOG,Blocks.BIRCH_WOOD,
                Blocks.JUNGLE_LOG,Blocks.JUNGLE_WOOD,Blocks.ACACIA_LOG,Blocks.ACACIA_WOOD,Blocks.DARK_OAK_LOG,Blocks.DARK_OAK_WOOD,
                Blocks.CHERRY_LOG,Blocks.CHERRY_WOOD,Blocks.MANGROVE_LOG,Blocks.MANGROVE_WOOD,Blocks.CRIMSON_STEM,Blocks.CRIMSON_HYPHAE,
                Blocks.WARPED_STEM,Blocks.WARPED_HYPHAE,Blocks.BAMBOO_BLOCK)) {
            var before=block.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X);
            h.getLevel().setBlockAndUpdate(context.getClickedPos(),before);
            h.assertTrue(axe.useOn(context).consumesAction(),"Vanilla stripping succeeds");
            h.assertTrue(h.getLevel().getBlockState(context.getClickedPos()).equals(AxeItem.getAxeStrippingState(before)),"Native stripped state and axis survive");
            count++;
            h.assertTrue(barkCount(h)==(BarkCompatibility.usesFarmersDelight()?0:count),"Only standalone Ecosystems produces Bark when stripping");
            axe.useOn(context);h.assertTrue(barkCount(h)==(BarkCompatibility.usesFarmersDelight()?0:count),"Already stripped wood cannot produce more Bark");
        }
        h.assertTrue(axe.getDamageValue()==count,"Native durability cost is preserved");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkCancelledProtectedStrippingRestoresWoodAndDropsNothing(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);
        var context=context(h,player);h.getLevel().setBlockAndUpdate(context.getClickedPos(),Blocks.OAK_LOG.defaultBlockState());
        Consumer<BlockEvent.EntityPlaceEvent> cancel=e->{if(e.getLevel()==h.getLevel()&&e.getPos().equals(context.getClickedPos()))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            h.assertTrue(!axe.useOn(context).consumesAction(),"Protection veto rejects stripping");
            h.assertTrue(h.getLevel().getBlockState(context.getClickedPos()).is(Blocks.OAK_LOG)&&barkCount(h)==0,"Rolled-back stripping gives no Bark");
        } finally {NeoForge.EVENT_BUS.unregister(cancel);}
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkCopperScrapingAndWaxRemovalDoNotDropBark(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);var context=context(h,player);
        for(var block:List.of(Blocks.WEATHERED_COPPER,Blocks.WAXED_COPPER_BLOCK)) {
            h.getLevel().setBlockAndUpdate(context.getClickedPos(),block.defaultBlockState());
            h.assertTrue(axe.useOn(context).consumesAction(),"Native copper action still succeeds");h.assertTrue(barkCount(h)==0,"Copper gives no Bark");
        }h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkCraftsOneStickInAnyCraftingSlot(GameTestHelper h) {
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"stick_from_bark");
        var recipeEntry=h.getLevel().getRecipeManager().byKey(id);
        h.assertTrue(recipeEntry.isPresent()!=BarkCompatibility.usesFarmersDelight(),"Ecosystems stick recipe only exists without Farmer's Delight");
        for(int slot=0;slot<9;slot++) {
            var items=new java.util.ArrayList<ItemStack>();for(int i=0;i<9;i++)items.add(i==slot?new ItemStack(FantasyFeudalismMobs.BARK.get()):ItemStack.EMPTY);
            var input=CraftingInput.of(3,3,items);var match=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel());
            if(BarkCompatibility.usesFarmersDelight()) {h.assertTrue(match.isEmpty(),"Disabled Bark recipe is not craftable");continue;}
            var recipe=match.orElseThrow();
            var result=recipe.value().assemble(input,h.getLevel().registryAccess());h.assertTrue(result.is(Items.STICK)&&result.getCount()==1,"One Bark crafts one stick, regardless of slot");
        }h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkFeedingThroughBodyPartsHealsAndConsumesOnlyWhenNeeded(GameTestHelper h) {
        var worm=h.spawn(RootWormModule.ROOT_WORM.get(),8,2,8);worm.setNoAi(true);worm.setHealth(10);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var bark=new ItemStack(BarkCompatibility.food(),3);player.setItemInHand(InteractionHand.MAIN_HAND,bark);
        h.assertTrue(worm.getParts()[8].interact(player,InteractionHand.MAIN_HAND).consumesAction(),"Bark can be fed through any body segment");
        h.assertTrue(worm.getHealth()==14&&bark.getCount()==2,"Bark restores two hearts and consumes one item");
        worm.interact(player,InteractionHand.MAIN_HAND);h.assertTrue(worm.getHealth()==16&&bark.getCount()==1,"Healing caps at maximum health");
        worm.interact(player,InteractionHand.MAIN_HAND);h.assertTrue(bark.getCount()==1&&worm.getTarget()==null,"Full-health worm wastes no food and remains neutral");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkCreativeFeedingPreservesItemsAndOtherFoodsDoNotHeal(GameTestHelper h) {
        var worm=h.spawn(RootWormModule.ROOT_WORM.get(),8,2,8);worm.setNoAi(true);worm.setHealth(8);
        var player=h.makeMockPlayer(GameType.CREATIVE);player.getAbilities().instabuild=true;var bark=new ItemStack(BarkCompatibility.food());player.setItemInHand(InteractionHand.MAIN_HAND,bark);
        worm.interact(player,InteractionHand.MAIN_HAND);h.assertTrue(worm.getHealth()==12&&bark.getCount()==1,"Creative feeding heals without consuming Bark");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));worm.interact(player,InteractionHand.MAIN_HAND);
        h.assertTrue(worm.getHealth()==12,"Ordinary sticks are not worm food");h.succeed();
    }

    @GameTest(template="test_empty",batch="bark_creative")
    public static void barkCreativeAndRecipeViewerVisibilityFollowInstalledMod(GameTestHelper h) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(),true,h.getLevel().registryAccess());
        boolean visible=net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.stream().anyMatch(tab->tab.getDisplayItems().stream().anyMatch(stack->stack.is(FantasyFeudalismMobs.BARK.get())));
        h.assertTrue(visible!=BarkCompatibility.usesFarmersDelight(),"Ecosystems Bark only appears in creative when standalone");
        h.assertTrue(new ItemStack(FantasyFeudalismMobs.BARK.get()).is(net.neoforged.neoforge.common.Tags.Items.HIDDEN_FROM_RECIPE_VIEWERS)==BarkCompatibility.usesFarmersDelight(),"Common recipe-viewer hiding tag follows Farmer's Delight presence");
        var advancement=h.getLevel().getServer().getAdvancements().get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"recipes/misc/stick_from_bark"));
        h.assertTrue((advancement!=null)!=BarkCompatibility.usesFarmersDelight(),"Disabled recipe has no recipe-book unlock");h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkOnlyActiveFoodHealsAndForeignRecipesRemainIntact(GameTestHelper h) {
        if(BarkCompatibility.usesFarmersDelight()) {
            var expected=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("farmersdelight:tree_bark"));
            h.assertTrue(BarkCompatibility.food()==expected&&expected!=Items.AIR,"Tree Bark is the selected real item");
            for(var name:List.of("cutting/oak_log","paper_from_tree_bark","organic_compost_from_tree_bark"))h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("farmersdelight",name)).isPresent(),"Farmer's Delight bark recipes remain loaded");
            var worm=h.spawn(RootWormModule.ROOT_WORM.get(),8,2,8);worm.setNoAi(true);worm.setHealth(8);
            var player=h.makeMockPlayer(GameType.SURVIVAL);var old=new ItemStack(FantasyFeudalismMobs.BARK.get());player.setItemInHand(InteractionHand.MAIN_HAND,old);
            worm.interact(player,InteractionHand.MAIN_HAND);h.assertTrue(worm.getHealth()==8&&old.getCount()==1,"Inactive Ecosystems Bark is preserved but not used instead of Tree Bark");
        } else h.assertTrue(BarkCompatibility.food()==FantasyFeudalismMobs.BARK.get(),"No external mod is needed for standalone Bark");
        h.succeed();
    }
    @GameTest(template="test_empty")
    public static void barkFarmersDelightCuttingBoardStillProducesItsOwnBark(GameTestHelper h) {
        var id=net.minecraft.resources.ResourceLocation.parse("farmersdelight:cutting_board");
        if(!BarkCompatibility.usesFarmersDelight()) {h.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id),"No Farmer's Delight dependency is loaded");h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(8,2,8));h.getLevel().setBlockAndUpdate(pos.below(),Blocks.OAK_PLANKS.defaultBlockState());
        var state=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState();h.getLevel().setBlockAndUpdate(pos,state);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var log=new ItemStack(Items.OAK_LOG);player.setItemInHand(InteractionHand.MAIN_HAND,log);state.useItemOn(log,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        var axe=new ItemStack(Items.IRON_AXE);player.setItemInHand(InteractionHand.MAIN_HAND,axe);state.useItemOn(axe,h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(BarkCompatibility.food())).mapToInt(e->e.getItem().getCount()).sum()==1,"Native cutting produces one Tree Bark");
        h.assertTrue(drops.stream().anyMatch(e->e.getItem().is(Items.STRIPPED_OAK_LOG)),"Native cutting still returns the stripped log");
        h.assertTrue(barkCount(h)==0,"No Ecosystems Bark duplicates the cutting-board reward");h.succeed();
    }
}
