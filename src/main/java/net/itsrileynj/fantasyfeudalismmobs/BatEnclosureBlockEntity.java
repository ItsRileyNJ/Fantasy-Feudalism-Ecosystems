package net.itsrileynj.fantasyfeudalismmobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Three resident reservations include bats currently flying outside; absent entities are never respawned. */
public final class BatEnclosureBlockEntity extends BlockEntity {
    public static final int CAPACITY=3;
    public static final class Resident {
        final UUID id;
        CompoundTag bat;
        boolean inside;
        Resident(UUID id,CompoundTag bat,boolean inside){this.id=id;this.bat=bat.copy();this.inside=inside;}
    }
    private final List<Resident> residents=new ArrayList<>();
    private List<Bat> displayBats;
    public BatEnclosureBlockEntity(BlockPos pos,BlockState state){super(FantasyFeudalismMobs.BAT_ENCLOSURE_ENTITY.get(),pos,state);}
    public int residentCount(){return residents.size();}
    public int insideCount(){return (int)residents.stream().filter(r->r.inside).count();}
    public List<UUID> residentIds(){return residents.stream().map(r->r.id).toList();}
    private BatColonies.Home address(){return BatColonies.Home.at((ServerLevel)level,worldPosition);}
    private void changed(){
        setChanged();displayBats=null;
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public boolean insert(ItemStack net){
        if(!(level instanceof ServerLevel server))return false;
        reconcile(server);
        CompoundTag saved=BatNetItem.captured(net);
        if(saved==null||!saved.hasUUID("UUID")||residents.size()>=CAPACITY)return false;
        UUID id=saved.getUUID("UUID");
        for(var dimension:server.getServer().getAllLevels())if(dimension.getEntity(id)!=null)return false;
        Bat test=EntityType.BAT.create(server);
        try{test.load(saved.copy());}catch(RuntimeException invalid){return false;}
        if(!test.isAlive()||!BatColonies.get(server).claim(id,address()))return false;
        residents.add(new Resident(id,saved,true));BatNetItem.clear(net);changed();return true;
    }
    public boolean returnBat(Bat bat){
        if(!(level instanceof ServerLevel server)||!BatColonies.get(server).owns(bat.getUUID(),address()))return false;
        var resident=residents.stream().filter(r->r.id.equals(bat.getUUID())&&!r.inside).findFirst().orElse(null);
        if(resident==null||!bat.isAlive())return false;
        resident.bat=bat.saveWithoutId(new CompoundTag());resident.inside=true;
        bat.discard();changed();return true;
    }
    public boolean feed(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(!(level instanceof ServerLevel server)||!player.getItemInHand(hand).is(net.minecraft.world.item.Items.APPLE))return false;
        reconcile(server);
        Resident chosen=null;Bat selected=null;
        for(var resident:residents)if(resident.inside){
            Bat bat=EntityType.BAT.create(server);bat.load(resident.bat.copy());
            if(BatFarming.canFeed(bat)&&(selected==null||(!BatFarming.ready(selected)&&BatFarming.ready(bat)))){chosen=resident;selected=bat;}
        }
        if(chosen==null)return false;
        var front=getBlockState().getValue(BatEnclosureBlock.FACING);
        BatFarming.feed(selected,player,hand,Vec3.atLowerCornerOf(worldPosition).add(.5+front.getStepX()*.85,.55,.5+front.getStepZ()*.85));
        chosen.bat=selected.saveWithoutId(new CompoundTag());changed();return true;
    }
    private void reconcile(ServerLevel server){
        if(residents.removeIf(r->!BatColonies.get(server).owns(r.id,address())))changed();
    }
    private Vec3 releasePosition(Bat bat){
        var front=getBlockState().getValue(BatEnclosureBlock.FACING);
        Vec3 preferred=new Vec3(.5+front.getStepX()*1.1,.3,.5+front.getStepZ()*1.1);
        for(var offset:List.of(preferred,new Vec3(.5,.3,-.6),new Vec3(.5,.3,1.6),new Vec3(-.6,.3,.5),new Vec3(1.6,.3,.5),new Vec3(.5,1.4,.5))){
            Vec3 at=Vec3.atLowerCornerOf(worldPosition).add(offset);
            if(level.hasChunkAt(BlockPos.containing(at))&&level.noCollision(bat,bat.getDimensions(bat.getPose()).makeBoundingBox(at))
                    &&level.getFluidState(BlockPos.containing(at)).isEmpty())return at;
        }
        return null;
    }
    private boolean release(Resident resident,boolean leavingHome){
        if(!(level instanceof ServerLevel server)||!resident.inside)return false;
        if(server.getEntity(resident.id)!=null)return false;
        Bat bat=EntityType.BAT.create(server);
        try{bat.load(resident.bat.copy());}catch(RuntimeException invalid){return false;}
        bat.setResting(false);Vec3 at=releasePosition(bat);if(at==null)return false;
        bat.moveTo(at.x,at.y,at.z,0,0);bat.setDeltaMovement(0,.1,0);
        if(leavingHome)bat.getPersistentData().remove(BatResidents.HOME);
        else BatResidents.assign(bat,worldPosition);
        if(!server.addFreshEntity(bat))return false;
        resident.inside=false;changed();return true;
    }
    public void breakHome(){
        if(!(level instanceof ServerLevel server))return;
        // Removing the block frees its interior as a safe fallback when every outside exit is blocked.
        for(var resident:List.copyOf(residents)){
            BatColonies.get(server).release(resident.id);
            if(resident.inside&&!release(resident,true)){
                Bat bat=EntityType.BAT.create(server);
                try{bat.load(resident.bat.copy());bat.getPersistentData().remove(BatResidents.HOME);bat.setResting(false);
                    bat.moveTo(worldPosition.getX()+.5,worldPosition.getY()+.1,worldPosition.getZ()+.5);server.addFreshEntity(bat);
                }catch(RuntimeException invalid){/* Invalid external NBT is not executed. */}
            }
        }
        residents.clear();changed();
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,BatEnclosureBlockEntity home){
        if(level.getGameTime()%20!=0)return;
        home.reconcile((ServerLevel)level);
        if(NightCycle.isNight(level))for(var resident:List.copyOf(home.residents))if(resident.inside)home.release(resident,false);
    }
    /** Display entities belong only to this client-side block entity and never join the world. */
    public List<Bat> displayBats(){
        if(level==null||!level.isClientSide)return List.of();
        if(displayBats==null){
            displayBats=new ArrayList<>();
            for(var resident:residents)if(resident.inside){
                Bat bat=EntityType.BAT.create(level);bat.load(resident.bat.copy());bat.setNoAi(true);bat.setResting(true);
                // These copies never tick; saved flight rotations must not affect their hanging display pose.
                bat.setYRot(0);bat.yRotO=0;bat.setXRot(0);bat.xRotO=0;
                bat.yBodyRot=bat.yBodyRotO=0;bat.yHeadRot=bat.yHeadRotO=0;
                bat.flyAnimationState.stop();
                bat.restAnimationState.start(0);displayBats.add(bat);
            }
        }
        return displayBats;
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);residents.clear();displayBats=null;
        for(Tag value:tag.getList("Bats",Tag.TAG_COMPOUND)){
            CompoundTag row=(CompoundTag)value;
            if(row.hasUUID("UUID")&&residents.size()<CAPACITY&&residents.stream().noneMatch(r->r.id.equals(row.getUUID("UUID"))))
                residents.add(new Resident(row.getUUID("UUID"),row.getCompound("Entity"),row.getBoolean("Inside")));
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);var list=new ListTag();
        for(var resident:residents){var row=new CompoundTag();row.putUUID("UUID",resident.id);row.put("Entity",resident.bat.copy());row.putBoolean("Inside",resident.inside);list.add(row);}
        tag.put("Bats",list);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override @Nullable public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
