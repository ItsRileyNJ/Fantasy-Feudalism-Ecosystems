package net.itsrileynj.fantasyfeudalismmobs.scorpion.client;

import net.itsrileynj.fantasyfeudalismmobs.scorpion.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class ScorpionRenderer extends MobRenderer<Scorpion,ScorpionModel> {
    public ScorpionRenderer(EntityRendererProvider.Context context) {super(context,new ScorpionModel(context.bakeLayer(ScorpionModel.LAYER)),.65F);}
    @Override public ResourceLocation getTextureLocation(Scorpion scorpion) {return ScorpionModule.id("textures/entity/scorpion.png");}
}
