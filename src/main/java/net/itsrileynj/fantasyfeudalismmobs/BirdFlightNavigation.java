package net.itsrileynj.fantasyfeudalismmobs;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.Vec3;

public final class BirdFlightNavigation extends FlyingPathNavigation {
    public BirdFlightNavigation(ForestBird bird,Level level) { super(bird,level); }
    @Override protected PathFinder createPathFinder(int maxNodes) {
        nodeEvaluator=new FlyNodeEvaluator() {
            @Override @Nullable protected Node findAcceptedNode(int x,int y,int z) {
                return BirdAltitude.allowed((ForestBird)mob,x,y,z)?super.findAcceptedNode(x,y,z):null;
            }
        };
        nodeEvaluator.setCanPassDoors(true);return new PathFinder(nodeEvaluator,maxNodes);
    }
    @Override protected boolean canMoveDirectly(Vec3 from,Vec3 to) {
        return BirdAltitude.segmentAllowed((ForestBird)mob,from,to)&&super.canMoveDirectly(from,to);
    }
}
