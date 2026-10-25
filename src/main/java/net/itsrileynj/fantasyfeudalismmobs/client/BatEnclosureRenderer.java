package net.itsrileynj.fantasyfeudalismmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Uses vanilla hanging bat models; display bats are never added as world entities. */
public final class BatEnclosureRenderer implements BlockEntityRenderer<BatEnclosureBlockEntity> {
    // Keep hanging bats under the flat roof, clear of the supplied model's rear angled baffle.
    private static final double[][] SINGLE={{.5,.42,5}};
    private static final double[][] PAIR={{.33,.32,-8},{.67,.49,10}};
    private static final double[][] TRIO={{.26,.31,-5},{.50,.50,12},{.74,.39,5}};
    public BatEnclosureRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(BatEnclosureBlockEntity home,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        if(home.getLevel()!=null)light=net.minecraft.client.renderer.LevelRenderer.getLightColor(home.getLevel(),
                home.getBlockPos().relative(home.getBlockState().getValue(net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlock.FACING)));
        var bats=home.displayBats();
        for(int i=0;i<bats.size();i++){
            poses.pushPose();poses.translate(.5,0,.5);
            var facing=home.getBlockState().getValue(net.itsrileynj.fantasyfeudalismmobs.BatEnclosureBlock.FACING);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-facing.toYRot()));
            var slot=(bats.size()==1?SINGLE:bats.size()==2?PAIR:TRIO)[i];
            // Native resting feet reach model Y=9.5; LivingEntityRenderer offsets its origin by 1.501.
            // Anchor the feet to the roof's underside at 14/16, including a saved entity scale.
            double anchor=14.0/16-.55*bats.get(i).getScale()*(1.501-9.5/16);
            poses.translate(slot[0]-.5,anchor,slot[1]-.5);
            poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)slot[2]));
            poses.scale(.55F,.55F,.55F);
            // Render just the model: world-position shadows and debug hitboxes do not belong inside a display.
            // Display bats never tick. A world partial tick would replay interpolation every tick
            // and wind their animation clock backwards, making the hanging models shake.
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(bats.get(i)).render(bats.get(i),0,0,poses,buffers,light);
            poses.popPose();
        }
    }
}
