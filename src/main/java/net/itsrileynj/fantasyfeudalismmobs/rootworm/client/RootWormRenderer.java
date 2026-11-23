package net.itsrileynj.fantasyfeudalismmobs.rootworm.client;

import net.itsrileynj.fantasyfeudalismmobs.rootworm.RootWormModule;
import net.itsrileynj.fantasyfeudalismmobs.rootworm.entity.RootWorm;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class RootWormRenderer extends MobRenderer<RootWorm, RootWormModel> {
    private static final ResourceLocation TEXTURE = RootWormModule.id("textures/entity/root_worm.png");
    public RootWormRenderer(EntityRendererProvider.Context context) {
        super(context, new RootWormModel(context.bakeLayer(RootWormModel.LAYER)), 0.35F);
    }
    @Override public ResourceLocation getTextureLocation(RootWorm entity) { return TEXTURE; }
}
