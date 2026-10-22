package net.itsrileynj.fantasyfeudalismmobs.client;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid=FantasyFeudalismMobs.MOD_ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EcosystemsClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(FantasyFeudalismMobs.BAT_ENCLOSURE_ENTITY.get(),BatEnclosureRenderer::new);
    }
    @SubscribeEvent public static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->0xFFE3C447,FantasyFeudalismMobs.FERTILIZER.get());
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(FantasyFeudalismMobs.BAT_ENCLOSURE.get(),net.minecraft.client.renderer.RenderType.cutout()));
        event.enqueueWork(()->ItemProperties.register(FantasyFeudalismMobs.BAT_NET.get(),
                ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"occupied"),
                (stack,level,entity,seed)->BatNetItem.captured(stack)==null?0:1));
    }
}
