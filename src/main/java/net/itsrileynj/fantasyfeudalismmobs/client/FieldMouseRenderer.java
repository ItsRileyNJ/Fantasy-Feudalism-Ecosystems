package net.itsrileynj.fantasyfeudalismmobs.client;

import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class FieldMouseRenderer extends MobRenderer<FieldMouse,FieldMouseModel> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"field_mouse"),"main");
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"textures/entity/field_mouse.png");
    private static final ResourceLocation AMBER_TEXTURE=ResourceLocation.fromNamespaceAndPath(FantasyFeudalismMobs.MOD_ID,"textures/entity/field_mouse_amber.png");
    public FieldMouseRenderer(EntityRendererProvider.Context context){super(context,new FieldMouseModel(context.bakeLayer(LAYER)),.12F);}
    @Override public ResourceLocation getTextureLocation(FieldMouse mouse){return mouse.getVariant()==FieldMouse.AMBER?AMBER_TEXTURE:TEXTURE;}
}
