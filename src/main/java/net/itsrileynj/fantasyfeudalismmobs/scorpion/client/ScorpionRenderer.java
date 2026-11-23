package net.itsrileynj.fantasyfeudalismmobs.scorpion.client;

import net.itsrileynj.fantasyfeudalismmobs.scorpion.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ScorpionRenderer extends MobRenderer<Scorpion,ScorpionModel> {
    public ScorpionRenderer(EntityRendererProvider.Context context) {this(context,false);}
    public ScorpionRenderer(EntityRendererProvider.Context context,boolean lava) {super(context,new ScorpionModel(context.bakeLayer(lava?ScorpionModel.LAVA_LAYER:ScorpionModel.LAYER)),lava?.1625F:.65F);}
    @Override protected void scale(Scorpion scorpion,com.mojang.blaze3d.vertex.PoseStack pose,float partialTick) {float scale=scorpion.visualScale();pose.scale(scale,scale,scale);}
    @Override public ResourceLocation getTextureLocation(Scorpion scorpion) {return ScorpionModule.id(scorpion.isLava()?"textures/entity/lava_scorpion.png":"textures/entity/scorpion.png");}
}
