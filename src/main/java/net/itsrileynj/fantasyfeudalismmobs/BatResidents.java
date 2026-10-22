package net.itsrileynj.fantasyfeudalismmobs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Only housed bats use this flight controller. No chunk is loaded to find an enclosure. */
public final class BatResidents {
    public static final String HOME="ffm_bat_home";
    private Path path;
    private FlyingPathNavigation navigation;
    private Vec3 destination;
    private long nextPath;
    public static boolean housed(Bat bat){return bat.getPersistentData().contains(HOME);}
    public static void assign(Bat bat,BlockPos pos){
        var tag=new CompoundTag();tag.putString("Dimension",bat.level().dimension().location().toString());tag.putLong("Pos",pos.asLong());
        bat.getPersistentData().put(HOME,tag);
    }
    public boolean tick(Bat bat){
        if(!(bat.level() instanceof ServerLevel level)||!housed(bat))return false;
        var tag=bat.getPersistentData().getCompound(HOME);BlockPos home=BlockPos.of(tag.getLong("Pos"));
        var address=new BatColonies.Home(tag.getString("Dimension"),home);
        if(!level.dimension().location().toString().equals(address.dimension())||!BatColonies.get(level).owns(bat.getUUID(),address)){
            BatColonies.get(level).release(bat.getUUID());bat.getPersistentData().remove(HOME);return false;
        }
        Vec3 center=Vec3.atCenterOf(home);
        var state=level.hasChunkAt(home)?level.getBlockState(home):net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var front=state.hasProperty(BatEnclosureBlock.FACING)?state.getValue(BatEnclosureBlock.FACING):net.minecraft.core.Direction.NORTH;
        Vec3 entrance=center.add(front.getStepX()*1.1,.15,front.getStepZ()*1.1);
        boolean returning=!NightCycle.isNight(level);
        if(level.hasChunkAt(home)){
            if(!(level.getBlockEntity(home) instanceof BatEnclosureBlockEntity enclosure)){
                BatColonies.get(level).release(bat.getUUID());bat.getPersistentData().remove(HOME);return false;
            }
            if(returning&&bat.distanceToSqr(center)<4){
                var hit=level.clip(new ClipContext(bat.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,bat));
                if((hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(home))&&enclosure.returnBat(bat))return true;
            }
        }
        bat.setResting(false);long now=level.getGameTime();
        boolean outside=bat.distanceToSqr(center)>14*14;
        if(returning||outside){
            if(destination==null||destination.distanceToSqr(entrance)>.01){path=null;nextPath=now;}
            destination=entrance;
        }else if(destination==null||bat.position().distanceToSqr(destination)<1||now>=nextPath){
            destination=null;
            for(int attempt=0;attempt<12;attempt++){
                Vec3 candidate=center.add((bat.getRandom().nextDouble()-.5)*24,1+bat.getRandom().nextDouble()*5,(bat.getRandom().nextDouble()-.5)*24);
                BlockPos at=BlockPos.containing(candidate);
                if(candidate.distanceToSqr(center)<13.5*13.5&&level.hasChunkAt(at)&&level.getFluidState(at).isEmpty()
                        &&level.noCollision(bat,bat.getDimensions(bat.getPose()).makeBoundingBox(candidate))){destination=candidate;break;}
            }
            if(destination==null)destination=center.add(0,2,-1.5);
        }
        // A failed or completed path waits for its normal retry deadline. Recreating the
        // navigation/path every tick here makes dense colonies stall on obstructed routes.
        if(navigation==null)navigation=new FlyingPathNavigation(bat,level);
        if(now>=nextPath){
            nextPath=now+40;
            path=navigation.createPath(BlockPos.containing(destination),1,32);
        }
        Vec3 toward=destination;
        if(path!=null&&!path.isDone()){
            toward=path.getNextEntityPos(bat);
            if(bat.position().distanceToSqr(toward)<.7*.7){path.advance();if(!path.isDone())toward=path.getNextEntityPos(bat);}
        }
        if(toward.distanceToSqr(center)>15*15)toward=entrance;
        Vec3 difference=toward.subtract(bat.position());
        Vec3 velocity=difference.lengthSqr()<.1?Vec3.ZERO:difference.normalize().scale(.24);
        // Account for vanilla gravity; Bat.tick applies the normal vertical damping afterward.
        bat.setDeltaMovement(velocity.add(0,bat.getGravity(),0));bat.hasImpulse=true;
        if(velocity.horizontalDistanceSqr()>.001){float yaw=(float)(Math.toDegrees(Math.atan2(velocity.z,velocity.x))-90);bat.setYRot(yaw);bat.setYHeadRot(yaw);}
        return true;
    }
}
