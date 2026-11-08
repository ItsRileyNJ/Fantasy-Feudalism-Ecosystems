package net.itsrileynj.fantasyfeudalismmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ForestBirdRenderer extends MobRenderer<ForestBird,ForestBirdModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"textures/entity/birds/forest_birds.png");
    public static ModelLayerLocation layer(BirdKind kind) { return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,kind.id),"main"); }
    public ForestBirdRenderer(EntityRendererProvider.Context context,BirdKind kind) { super(context,new ForestBirdModel(context.bakeLayer(layer(kind))),.16F*kind.scale); }
    @Override protected void scale(ForestBird entity,PoseStack pose,float partial) { float s=entity.kind().scale;pose.scale(s,s,s); }
    @Override public ResourceLocation getTextureLocation(ForestBird entity) { return TEXTURE; }
}
