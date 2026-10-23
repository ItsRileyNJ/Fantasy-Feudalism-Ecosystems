package net.itsrileynj.fantasyfeudalismmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Uses vanilla hanging bat models; display bats are never added as world entities. */
public final class BatEnclosureRenderer implements BlockEntityRenderer<BatEnclosureBlockEntity> {
    public BatEnclosureRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(BatEnclosureBlockEntity home,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(home.getLevel()!=null)light=net.minecraft.client.renderer.LevelRenderer.getLightColor(home.getLevel(),
                home.getBlockPos().relative(home.getBlockState().getValue(net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlock.FACING)));
        var bats=home.displayBats();
        for(int i=0;i<bats.size();i++){
            poses.pushPose();poses.translate(.5,0,.5);
            var facing=home.getBlockState().getValue(net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlock.FACING);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-facing.toYRot()));
            double x=bats.size()==1?.5:bats.size()==2?.32+i*.36:.24+i*.26;
            poses.translate(x-.5,.27,-.05);poses.scale(.55F,.55F,.55F);
            // Render just the model: world-position shadows and debug hitboxes do not belong inside a display.
            // Display bats never tick. A world partial tick would replay interpolation every tick
            // and wind their animation clock backwards, making the hanging models shake.
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bats.get(i)).render(bats.get(i),0,0,poses,buffers,light);
            poses.popPose();
        }
    }
}
