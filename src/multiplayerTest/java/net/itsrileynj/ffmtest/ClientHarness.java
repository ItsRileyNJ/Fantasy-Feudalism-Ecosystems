package net.itsrileynj.ffmtest;

import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.SpiderModel;
import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="ffm_multiplayer_test", value=Dist.CLIENT)
public final class ClientHarness {
    private static final Map<Integer,Integer> WOLF_HURT=new HashMap<>();
    private static final Map<String,Integer> WOLF_SOUNDS=new java.util.concurrent.ConcurrentHashMap<>();
    @SubscribeEvent public static void sound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if(event.getName().contains("wolf"))WOLF_SOUNDS.merge(event.getName(),1,Integer::sum);
    }
    private static final Map<Integer,Map<String,Object>> WOLF_POSES=new HashMap<>();
    private static final Map<Integer,Map<String,Object>> HORSE_POSES=new HashMap<>();
    private static long frames, skeletonFrames, crossbowFrames, guardFrames;
    private static final List<String> RENDER_ERRORS=new ArrayList<>();
    private static final Map<Integer,Entity> OBSERVED_ENTITIES=new HashMap<>();
    private static final Map<Integer,Map<String,Object>> CROSSBOW_POSES=new HashMap<>();
    private static final Map<Integer,Map<String,Object>> SPIDER_POSES=new HashMap<>();
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) { frames++; }
    @SubscribeEvent public static void rendered(RenderLivingEvent.Post<?,?> event) {
        if(event.getEntity() instanceof Spider spider && event.getRenderer().getModel() instanceof SpiderModel<?> model) {
            var abdomen=model.root().getChild("body1");
            SPIDER_POSES.put(spider.getId(),Map.of("sitting",SpiderCompanion.isSitting(spider),
                    "abdomenY",abdomen.y,"restY",abdomen.getInitialPose().y,
                    "abdomenXRot",abdomen.xRot,"restXRot",abdomen.getInitialPose().xRot));
        }
        if(MercenaryHarness.isGuard(event.getEntity())) {
            guardFrames++;
            if(event.getRenderer().getModel() instanceof HierarchicalModel<?> model)
                model.root().getAllParts().forEach(part->{
                    if(!Float.isFinite(part.xRot)||!Float.isFinite(part.yRot)||!Float.isFinite(part.zRot))RENDER_ERRORS.add("Non-finite guard model rotation");
                });
        }
        if(event.getEntity() instanceof net.minecraft.world.entity.animal.horse.Horse horse
                && event.getRenderer().getModel() instanceof net.minecraft.client.model.HorseModel<?> horseModel) {
            try {
                var field=net.minecraft.client.model.HorseModel.class.getDeclaredField("headParts");field.setAccessible(true);
                var head=(net.minecraft.client.model.geom.ModelPart)field.get(horseModel);
                if(!Float.isFinite(head.xRot)||!Float.isFinite(head.yRot))RENDER_ERRORS.add("Non-finite horse head rotation");
                HORSE_POSES.put(horse.getId(),Map.of("grazing",horse.isEating(),"eatAnim",horse.getEatAnim(1),"headX",head.xRot,"headY",head.y));
            } catch(ReflectiveOperationException error) {RENDER_ERRORS.add(error.toString());}
        }
        if(event.getEntity() instanceof net.minecraft.world.entity.animal.Wolf wolf
                && event.getRenderer().getModel() instanceof net.minecraft.client.model.WolfModel<?> model) {
            var pose=new LinkedHashMap<String,Object>();
            pose.put("tamed",wolf.isTame());pose.put("sitting",wolf.isInSittingPose());
            pose.put("stalking",((net.itsrileynj.fantasyfeudalismmobs.WolfState)wolf).ffm$stalking());
            try {
                for(String name:List.of("head","body","upperBody","tail","rightFrontLeg","leftFrontLeg")) {
                    var field=net.minecraft.client.model.WolfModel.class.getDeclaredField(name);field.setAccessible(true);
                    var part=(net.minecraft.client.model.geom.ModelPart)field.get(model);
                    if(!Float.isFinite(part.xRot)||!Float.isFinite(part.y))RENDER_ERRORS.add("Non-finite wolf model pose");
                    pose.put(name,Map.of("y",part.y,"xRot",part.xRot,"scaleY",part.yScale));
                }
                WOLF_POSES.put(wolf.getId(),pose);
            } catch(ReflectiveOperationException error){RENDER_ERRORS.add(error.toString());}
        }
        if(!(event.getEntity() instanceof AbstractSkeleton skeleton))return;
        skeletonFrames++;
        if(skeleton.getMainHandItem().getItem() instanceof CrossbowItem)crossbowFrames++;
        if(event.getRenderer().getModel() instanceof HumanoidModel<?> model) {
            for(var part:List.of(model.head,model.rightArm,model.leftArm,model.rightLeg,model.leftLeg))
                if(!Float.isFinite(part.xRot)||!Float.isFinite(part.yRot)||!Float.isFinite(part.zRot))RENDER_ERRORS.add("Non-finite skeleton model rotation");
            if(skeleton.getMainHandItem().getItem() instanceof CrossbowItem)
                CROSSBOW_POSES.put(skeleton.getId(),Map.of("usingItem",skeleton.isUsingItem(),"aggressive",skeleton.isAggressive(),
                        "rightX",model.rightArm.xRot,"leftX",model.leftArm.xRot,
                        "rightY",model.rightArm.yRot,"leftY",model.leftArm.yRot));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(System.getProperty("ffm.test.control")==null)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=null)for(var entity:mc.level.entitiesForRendering())
            if(entity instanceof net.minecraft.world.entity.animal.Wolf wolf)WOLF_HURT.merge(wolf.getId(),wolf.hurtTime,Math::max);
        EffectParticleProbe.tick(mc);
        JsonObject q=ControlFiles.command();if(q==null)return;
        try {
            Object value=execute(Minecraft.getInstance(),q);
            if(value!=null)ControlFiles.respond(q,value);
        }catch(Throwable error){error.printStackTrace();ControlFiles.fail(q,error);}
    }
    private static Entity target(Minecraft mc,JsonObject q) {
        Entity entity=mc.level.getEntity(q.get("entity").getAsInt());
        if(entity==null)entity=OBSERVED_ENTITIES.get(q.get("entity").getAsInt());
        if(entity==null)throw new IllegalStateException("Client has not tracked entity "+q.get("entity"));return entity;
    }
    private static Object execute(Minecraft mc,JsonObject q)throws Exception {
        String action=q.get("action").getAsString();
        switch(action) {
            case "cleanView":mc.options.hideGui=q.get("value").getAsBoolean();mc.gui.getChat().clearMessages(true);mc.getTutorial().stop();return true;
            case "watchEffectParticles":EffectParticleProbe.watch(q.get("entity").getAsInt());return true;
            case "effectParticleProbe":return EffectParticleProbe.snapshot();
            case "snapshot":return snapshot(mc);
            case "connect": {
                if(mc.level!=null){mc.level.disconnect();mc.disconnect();}OBSERVED_ENTITIES.clear();
                String address=q.has("address")?q.get("address").getAsString():"127.0.0.1:25577";
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),new ServerData("FFM Test",address,ServerData.Type.OTHER),false,null);return true;
            }
            case "disconnect":if(mc.level!=null)mc.level.disconnect();mc.disconnect();mc.setScreen(new TitleScreen());return true;
            case "select":mc.player.getInventory().selected=q.get("slot").getAsInt();return true;
            case "use":mc.options.keyUse.setDown(q.get("value").getAsBoolean());return true;
            case "useBlock": {
                var pos=new net.minecraft.core.BlockPos(q.get("x").getAsInt(),q.get("y").getAsInt(),q.get("z").getAsInt());
                return mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false)).toString();
            }
            case "walk":mc.options.keyUp.setDown(q.get("value").getAsBoolean());return true;
            case "shift": {
                boolean value=q.get("value").getAsBoolean();mc.options.keyShift.setDown(value);mc.player.setShiftKeyDown(value);
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player,value?ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY:ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));return true;
            }
            case "button":mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId,q.get("button").getAsInt());return true;
            case "command":mc.getConnection().sendCommand(q.get("command").getAsString());return true;
            case "guiScale":mc.options.guiScale().set(q.get("scale").getAsInt());mc.resizeDisplay();return true;
            case "mouse": {
                double x=q.get("x").getAsDouble(),y=q.get("y").getAsDouble();
                int button=q.has("button")?q.get("button").getAsInt():0;
                mc.screen.mouseClicked(x,y,button);mc.screen.mouseReleased(x,y,button);return true;
            }
            case "interact": {
                Entity e=target(mc,q);InteractionHand hand=q.has("offhand")&&q.get("offhand").getAsBoolean()?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
                int count=q.has("count")?q.get("count").getAsInt():1;
                for(int i=0;i<count;i++)mc.gameMode.interact(mc.player,e,hand);
                return true;
            }
            case "attack":mc.gameMode.attack(mc.player,target(mc,q));return true;
            case "inventory":mc.setScreen(new InventoryScreen(mc.player));return true;
            case "close":if(mc.player!=null)mc.player.closeContainer();else mc.setScreen(null);return true;
            case "click": {
                mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,q.get("slot").getAsInt(),
                    q.has("button")?q.get("button").getAsInt():0,ClickType.valueOf(q.has("type")?q.get("type").getAsString():"PICKUP"),mc.player);return true;
            }
            case "look":mc.player.setYRot(q.get("yaw").getAsFloat());mc.player.setXRot(q.get("pitch").getAsFloat());return true;
            case "screenshot": {
                Path path=ControlFiles.root().resolve(q.get("file").getAsString());Files.createDirectories(path.getParent());
                try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}return path.toString();
            }
            case "reload": {
                mc.reloadResourcePacks().whenComplete((ok,error)->{
                    if(error!=null)ControlFiles.fail(q,error);else ControlFiles.respond(q,true);
                });return null;
            }
            default:throw new IllegalArgumentException("Unknown client action "+action);
        }
    }
    private static Object snapshot(Minecraft mc) throws ReflectiveOperationException {
        var result=new LinkedHashMap<String,Object>();
        result.put("screen",mc.screen==null?"":mc.screen.getClass().getSimpleName());result.put("frames",frames);
        result.put("skeletonFrames",skeletonFrames);result.put("crossbowFrames",crossbowFrames);result.put("renderErrors",List.copyOf(RENDER_ERRORS));
        result.put("guardFrames",guardFrames);
        result.put("crossbowPoses",new HashMap<>(CROSSBOW_POSES));
        result.put("spiderPoses",new HashMap<>(SPIDER_POSES));
        result.put("horsePoses",new HashMap<>(HORSE_POSES));
        result.put("wolfPoses",new HashMap<>(WOLF_POSES));
        result.put("wolfHurtMax",new HashMap<>(WOLF_HURT));result.put("wolfSounds",new HashMap<>(WOLF_SOUNDS));
        if(mc.screen instanceof CreativeModeInventoryScreen creative) {
            result.put("creativeTabs",creative.getCurrentPage().getVisibleTabs().stream().map(tab->Map.of(
                    "title",tab.getDisplayName().getString(),"top",creative.getCurrentPage().isTop(tab),
                    "column",creative.getCurrentPage().getColumn(tab),
                    "items",tab.getIconItem().is(FantasyFeudalismMobs.BEE_BOTTLE)
                            ? tab.getDisplayItems().stream().map(ControlFiles::stack).toList() : List.of())).toList());
            result.put("creativeSlots",creative.getMenu().slots.stream().map(slot->ControlFiles.stack(slot.getItem())).toList());
        }
        result.put("connected",mc.level!=null&&mc.player!=null);
        if(mc.player!=null&&mc.level!=null) {
            result.put("player",ControlFiles.player(mc.player));var entities=new ArrayList<Map<String,Object>>();
            for(Entity e:mc.level.entitiesForRendering())if(e!=mc.player){
                var info=ControlFiles.entity(e);
                if(e instanceof Spider spider) {
                    var texture=mc.getEntityRenderDispatcher().getRenderer(spider).getTextureLocation(spider);
                    info.put("texture",texture.toString());
                    info.put("texturePresent",mc.getResourceManager().getResource(texture).isPresent());
                }
                if(MercenaryHarness.isGuard(e))info.put("privateHighlight",Class.forName("dev.villagerscontract.ClientGuardHighlights").getMethod("highlighted",Entity.class).invoke(null,e));
                entities.add(info);OBSERVED_ENTITIES.put(e.getId(),e);
            }result.put("entities",entities);
            var meatModels=new LinkedHashMap<String,Object>();
            for(var item:List.of(FantasyFeudalismMobs.RAW_HORSE.get(),FantasyFeudalismMobs.COOKED_HORSE.get())) {
                var meatModel=mc.getItemRenderer().getModel(new ItemStack(item),mc.level,mc.player,0);
                meatModels.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString(),Map.of(
                        "missing",meatModel==mc.getModelManager().getMissingModel(),"sprite",meatModel.getParticleIcon().contents().name().toString()));
            }
            result.put("horseMeatModels",meatModels);
            ItemStack bottle=new ItemStack(FantasyFeudalismMobs.BEE_BOTTLE.get());
            var model=mc.getItemRenderer().getModel(bottle,mc.level,mc.player,0);
            result.put("beeBottleModelMissing",model==mc.getModelManager().getMissingModel());
            result.put("beeBottleSprite",model.getParticleIcon().contents().name().toString());
        }
        return result;
    }
}
