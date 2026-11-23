package net.itsrileynj.fantasyfeudalismmobs.rootworm.client;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Register only on Dist.CLIENT. No blocks, foliage colors or worldgen rendering are included. */
public final class RootWormClient {
    public static void register(IEventBus bus) {
        bus.addListener(RootWormClient::renderers);
        bus.addListener(RootWormClient::layers);
    }
    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RootWormModule.ROOT_WORM.get(), RootWormRenderer::new);
    }
    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RootWormModel.LAYER, RootWormModel::createLayer);
    }
    private RootWormClient() {}
}
