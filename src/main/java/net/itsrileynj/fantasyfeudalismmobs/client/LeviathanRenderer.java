package net.itsrileynj.fantasyfeudalismmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.Leviathan;
import net.itsrileynj.fantasyfeudalismmobs.Leviathans;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

final class LeviathanRenderer extends MobRenderer<Leviathan, LeviathanModel> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(Leviathans.ID, "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID, "textures/entity/leviathan.png");

    LeviathanRenderer(EntityRendererProvider.Context context) {
        super(context, new LeviathanModel(context.bakeLayer(LAYER)), 1.15F);
    }

    @Override
    public ResourceLocation getTextureLocation(Leviathan fish) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(Leviathan fish, PoseStack pose, float age, float bodyYaw,
                                  float partialTick, float scale) {
        super.setupRotations(fish, pose, age, fish.swimHeading(partialTick, bodyYaw), partialTick, scale);
    }

    @Override
    protected void scale(Leviathan fish, PoseStack pose, float partialTick) {
        pose.scale(3F, 3F, 3F);
    }
}
