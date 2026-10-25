package net.itsrileynj.ffmtest;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.itsrileynj.fantasyfeudalismmobs.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.entity.ambient.Bat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Opt-in test-only wrapper measuring animated native feet in enclosure-local coordinates. */
public final class BatAttachmentProbe implements BlockEntityRenderer<BatEnclosureBlockEntity> {
    private static final Map<String,Map<String,Object>> SAMPLES=new TreeMap<>();
    private static Matrix4f enclosureInverse;
    private static BatEnclosureBlockEntity active;
    private final BlockEntityRenderer<BatEnclosureBlockEntity> delegate;
    private BatAttachmentProbe(BlockEntityRenderer<BatEnclosureBlockEntity> delegate){this.delegate=delegate;}
    @SuppressWarnings("unchecked") public static Object install(Minecraft mc)throws Exception{
        var dispatcher=mc.getBlockEntityRenderDispatcher();
        var field=BlockEntityRenderDispatcher.class.getDeclaredField("renderers");field.setAccessible(true);
        var renderers=new HashMap<>((Map<net.minecraft.world.level.block.entity.BlockEntityType<?>,BlockEntityRenderer<?>>)field.get(dispatcher));
        var type=FantasyFeudalismMobs.BAT_ENCLOSURE_ENTITY.get();var original=renderers.get(type);
        if(!(original instanceof BatAttachmentProbe))renderers.put(type,new BatAttachmentProbe((BlockEntityRenderer<BatEnclosureBlockEntity>)original));
        field.set(dispatcher,renderers);SAMPLES.clear();return true;
    }
    public static Object snapshot(){return new TreeMap<>(SAMPLES);}
    @Override public void render(BatEnclosureBlockEntity home,float partial,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
        active=home;enclosureInverse=new Matrix4f(poses.last().pose()).invert();
        try{delegate.render(home,partial,poses,buffers,light,overlay);}finally{active=null;enclosureInverse=null;}
    }
    public static void record(Bat bat,BatModel model,PoseStack parent){
        if(active==null||!active.displayBats().contains(bat))return;
        var local=new PoseStack();model.root().translateAndRotate(local);
        var body=model.root().getChild("body");body.translateAndRotate(local);body.getChild("feet").translateAndRotate(local);
        // Reconstruct the native LivingEntityRenderer transform around the actual animated foot vertices.
        var matrix=new Matrix4f(enclosureInverse).mul(parent.last().pose())
                .scale(bat.getScale()).rotateY((float)Math.PI).scale(-1,-1,1).translate(0,-1.501F,0)
                .mul(local.last().pose());
        Vector3f tip=matrix.transformPosition(new Vector3f(0,2F/16,0));
        String id=bat.getUUID().toString();var previous=SAMPLES.get(id);int frames=previous==null?1:(int)previous.get("frames")+1;
        double gap=Math.abs(tip.y()-14.0/16),maximum=previous==null?gap:Math.max(gap,(double)previous.get("maxRoofError"));
        SAMPLES.put(id,Map.of("frames",frames,"feet",List.of(tip.x(),tip.y(),tip.z()),"roofY",14.0/16,"maxRoofError",maximum,"home",active.getBlockPos().toShortString()));
    }
}
