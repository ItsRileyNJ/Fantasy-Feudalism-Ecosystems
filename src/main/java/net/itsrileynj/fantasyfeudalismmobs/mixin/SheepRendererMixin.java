package net.itsrileynj.fantasyfeudalismmobs.mixin;

import net.itsrileynj.fantasyfeudalismmobs.SheepState;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SheepRenderer.class)
public abstract class SheepRendererMixin extends MobRenderer<Sheep,SheepModel<Sheep>> {
    protected SheepRendererMixin(EntityRendererProvider.Context context, SheepModel<Sheep> model, float shadow) {
        super(context,model,shadow);
    }

    @Override protected boolean isShaking(Sheep sheep) {
        // Use the native frozen-entity shiver without setting frozen ticks or causing damage.
        return super.isShaking(sheep) || ((SheepState)sheep).ffm$isCold();
    }
}
