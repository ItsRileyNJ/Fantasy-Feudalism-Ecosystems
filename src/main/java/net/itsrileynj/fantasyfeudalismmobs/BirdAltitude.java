package net.itsrileynj.fantasyfeudalismmobs;

import java.util.*;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Loaded-column terrain heights, cached only for the current world tick. */
public final class BirdAltitude {
    public static final int MAX_ABOVE_GROUND=20;
    private static final Map<Level,Frame> CACHE=new WeakHashMap<>();
    private static final class Frame { long tick=Long.MIN_VALUE;final Long2IntOpenHashMap heights=new Long2IntOpenHashMap(); }
    private BirdAltitude() {}
    public static int surfaceY(Level level,int x,int z) {
        var frame=CACHE.computeIfAbsent(level,k->new Frame());long tick=level.getGameTime();
        if(frame.tick!=tick) { frame.tick=tick;frame.heights.clear(); }
        long key=((long)x<<32)^(z&0xffffffffL);
        if(frame.heights.containsKey(key))return frame.heights.get(key);
        if(!level.hasChunkAt(new BlockPos(x,level.getMinBuildHeight(),z)))return Integer.MIN_VALUE/2;
        int top=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
        // A tree trunk also must not raise the apparent terrain to canopy height.
        var pos=new BlockPos.MutableBlockPos();
        while(top>level.getMinBuildHeight()) {
            var state=level.getBlockState(pos.set(x,top-1,z));
            if(!state.isAir()&&!state.is(BlockTags.LEAVES)&&!state.is(BlockTags.LOGS))break;
            top--;
        }
        frame.heights.put(key,top);return top;
    }
    public static double maxFeet(ForestBird bird,double x,double z) {
        return surfaceY(bird.level(),Mth.floor(x),Mth.floor(z))+MAX_ABOVE_GROUND-bird.getBbHeight();
    }
    public static boolean allowed(ForestBird bird,double x,double y,double z) { return y<=maxFeet(bird,x,z)+1e-5; }
    public static boolean segmentAllowed(ForestBird bird,Vec3 from,Vec3 to) {
        int steps=Math.max(1,Mth.ceil(from.distanceTo(to)*2));
        for(int n=0;n<=steps;n++) {
            double t=(double)n/steps;Vec3 at=from.lerp(to,t);
            if(!allowed(bird,at.x,at.y,at.z))return false;
        }
        return true;
    }
}
