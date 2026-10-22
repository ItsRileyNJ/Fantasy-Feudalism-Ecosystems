"""Two real clients observe server-owned frog escape physics and native jumping poses."""
import json,pathlib,time
from control import rpc,snapshot,wait_for
OUT=pathlib.Path('/workspace/multiplayer/frogs-0.1.13-results.json')
checks=[]
def command(c):return rpc('server','command',command=c)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def check(name,**detail):
    checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def run():
    rpc('server','setup');rpc('server','clear')
    command('fill 0 62 0 25 62 16 minecraft:stone')
    command('fill 0 63 0 25 63 16 minecraft:grass_block')
    command('fill 0 64 0 25 70 16 minecraft:air')
    command('time set 6000')
    for side,name,x,z,yaw in [('owner','FFMOwner',1,8,-90),('guest','FFMGuest',8,0,0)]:
        command('gamemode creative '+name)
        rpc('server','teleport',player=name,x=x,y=64,z=z,yaw=yaw,pitch=10)
        rpc(side,'close')
    command('fill 10 63 6 14 63 10 minecraft:water')
    # Begin beyond the approach radius, then genuinely approach through the normal client movement input.
    frog=rpc('server','spawn',type='minecraft:frog',x=8,y=64,z=8,ai=True);eid=frog['id']
    time.sleep(.5)
    rpc('owner','walk',key='forward',value=True)
    try:
        wait_for(lambda:(f if (f:=ent(snapshot('server'),eid)) and f['pose']=='LONG_JUMPING' else None),timeout=10)
    finally:rpc('owner','walk',key='forward',value=False)
    landed=wait_for(lambda:(f if (f:=ent(snapshot('server'),eid)) and f['inWater'] and f['pose']=='STANDING' else None),timeout=15)
    clients={side:wait_for(lambda side=side:(s if (s:=snapshot(side))['frogJumpFrames'].get(str(eid),0)>0 else None)) for side in ['owner','guest']}
    assert all(not s['renderErrors'] for s in clients.values())
    check('A real player approach launches the frog; both graphical clients render its synchronized jump and water landing',frog=landed,
          clients={side:dict(jumpFrames=s['frogJumpFrames'][str(eid)],frog=ent(s,eid),renderErrors=s['renderErrors']) for side,s in clients.items()})
    for side in ['owner','guest']:
        wait_for(lambda side=side:(f if (f:=ent(snapshot(side),eid)) and f['inWater'] and f['pose']=='STANDING' else None))
    check('Both clients converge on the standing swimming pose after landing')
    rpc('server','save')
    command('gamemode survival FFMOwner')
    rpc('owner','inventory');wait_for(lambda:snapshot('owner')['screen']=='InventoryScreen');rpc('owner','close')
    check('World save and the normal player inventory work after repeated client updates')
if __name__=='__main__':
    try:run()
    except BaseException as e:
        OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2));raise
