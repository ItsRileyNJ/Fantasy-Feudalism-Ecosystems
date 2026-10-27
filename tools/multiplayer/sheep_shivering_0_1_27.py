"""Measure actual animated sheep model poses on two clients in snowy taiga and rain."""
import json,time
from control import rpc,snapshot,wait_for,TEST_ROOT
SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
checks=[]
def cmd(command):return rpc('server','command',command=command)
def record(name,**evidence):
    checks.append({'name':name,'passed':True,**evidence})
    (TEST_ROOT/'sheep-shivering-0.1.27-results.json').write_text(json.dumps(checks,indent=2));print('PASS:',name,flush=True)
def find(uid,side='server'):return next((e for e in snapshot(side)['entities'] if e['uuid']==uid),None)
def spawned(tag,x,z,sheared=True,y=64):
    cmd('summon minecraft:sheep '+f'{x} {y} {z} '+'{NoAI:1b,Sheared:'+('1b' if sheared else '0b')+',PersistenceRequired:1b,Tags:["ffm_shiver_test","'+tag+'"]}')
    return next(e for e in snapshot('server')['entities'] if e['type']=='minecraft:sheep' and abs(e['position'][0]-x)<.01 and abs(e['position'][2]-z)<.01)
def measured(sheep):
    eid=str(sheep['id']);uid=sheep['uuid'];samples={side:[] for side in SIDES}
    wait_for(lambda:all((s:=find(uid,side)) and s['cold'] and s['nativeShivering'] for side in SIDES))
    wait_for(lambda:all(snapshot(side)['sheepPoses'].get(eid) for side in SIDES))
    for _ in range(12):
        for side in SIDES:samples[side].append(snapshot(side)['sheepPoses'][eid])
        time.sleep(.08)
    result={}
    for side,poses in samples.items():
        headRange=max(p['head']['x'] for p in poses)-min(p['head']['x'] for p in poses)
        bodyRange=max(p['body']['zRot'] for p in poses)-min(p['body']['zRot'] for p in poses)
        assert headRange>.4 and bodyRange>.05,(side,headRange,bodyRange)
        result[side]={'samples':len(poses),'headTravelModelPixels':headRange,'bodyRollRangeRadians':bodyRange}
    return result
def reset_pose(sheep,side):
    pose=snapshot(side)['sheepPoses'].get(str(sheep['id']))
    return pose if pose and not pose['cold'] and all(abs(pose[n]['x']-pose[n]['restX'])<1e-6 and abs(pose[n]['zRot']-pose[n]['restZRot'])<1e-6 for n in ['head','body','rightFrontLeg','leftFrontLeg']) else None

def run():
    rpc('server','snapshot',timeout=180)
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,90)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('weather clear')
    cmd('fillbiome 0 63 0 20 72 16 minecraft:snowy_taiga');cmd('setblock 8 64 8 minecraft:snow[layers=6]')
    for side,x in [('owner',7.8),('guest',9.2)]:
        rpc(side,'close');cmd('gamemode creative '+NAMES[side]);rpc('server','teleport',player=NAMES[side],x=x,y=64,z=5.5,yaw=0,pitch=10)
    cold=spawned('taiga',8.5,8.5,y=64.625);warm=spawned('wooled',10.5,8.5,False)
    record('Sheared sheep visibly move their head and body on snow in snowy taiga on both clients',measurements=measured(cold))
    for side in SIDES:
        wait_for(lambda:reset_pose(warm,side));rpc(side,'screenshot',file='sheep-shivering-027/taiga-'+side+'.png')
    record('A woolled sheep rendered beside a cold sheep retains its exact normal pose')
    cmd('fillbiome 0 63 0 20 72 16 minecraft:plains');cmd('weather rain')
    # Use a newly spawned sheared sheep so rain starts cold from a warm state, rather than carrying over taiga cold.
    wet=spawned('rain',8.5,10.5)
    record('Actual rain in a warm plains biome starts cold and visible shivering on both clients',measurements=measured(wet))
    cmd('setblock 8 67 10 minecraft:glass');start=snapshot('server')['gameTime']
    wait_for(lambda:snapshot('server')['gameTime']>=start+300,25)
    assert all(find(wet['uuid'],side)['cold'] for side in SIDES)
    wait_for(lambda:all(reset_pose(wet,side) for side in SIDES),30)
    elapsed=snapshot('server')['gameTime']-start;assert 600<=elapsed<=660,elapsed
    record('Rain shelter retains shivering halfway through recovery and restores all model offsets after thirty seconds',elapsedServerTicks=elapsed)
    for side in SIDES:
        rpc(side,'reload',timeout=90);wait_for(lambda:not snapshot(side)['loadingOverlay'],90)
    record('Both clients retain visible motion after resource reload',measurements=measured(cold))
    for side in SIDES:
        wait_for(lambda:reset_pose(warm,side));wait_for(lambda:reset_pose(wet,side));assert not snapshot(side)['renderErrors']
    record('Sheltered and woolled sheep have no leftover pose offsets or rendering errors')
    cmd('kill @e[tag=ffm_shiver_test]');cmd('setblock 8 64 8 minecraft:air');cmd('setblock 8 67 10 minecraft:air');cmd('weather clear');cmd('save-all')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('stop');print('COMPLETE:',len(checks),'two-client checks passed',flush=True)
if __name__=='__main__':run()
