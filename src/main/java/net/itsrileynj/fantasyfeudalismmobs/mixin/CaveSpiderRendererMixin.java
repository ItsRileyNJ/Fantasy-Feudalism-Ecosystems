package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.FantasyFeudalismMobs;
import net.itsrileynj.fantasyfeudalismmobs.SpiderCompanion;
import net.minecraft.client.renderer.entity.CaveSpiderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.CaveSpider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveSpiderRenderer.class)
public abstract class CaveSpiderRendererMixin {
    @Unique private static final ResourceLocation FFM_TAMED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FantasyFeudalismMobs.MOD_ID, "textures/entity/spider/tamed_cave_spider.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/monster/CaveSpider;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"), cancellable = true)
    private void ffm$tamedTexture(CaveSpider spider, CallbackInfoReturnable<ResourceLocation> cir) {
        if (SpiderCompanion.isTamed(spider)) cir.setReturnValue(FFM_TAMED_TEXTURE);
    }
}
