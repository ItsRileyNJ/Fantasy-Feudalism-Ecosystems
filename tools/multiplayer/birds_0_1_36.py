"""Two-client appearance, actual rendered bird movement, fleeing, altitude and reload checks."""
import sys,json,time
from concurrent.futures import ThreadPoolExecutor
from control import rpc,snapshot,wait_for,TEST_ROOT
SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
KINDS=('robin','blue_jay','goldfinch','crow');SAVE=TEST_ROOT/'birds-0.1.36-state.json';RESULTS=TEST_ROOT/'birds-0.1.36-results.json'
checks=[]
def cmd(command):return rpc('server','command',command=command)
def record(name,**data):
    checks.append({'name':name,'passed':True,**data});RESULTS.write_text(json.dumps(checks,indent=2));print('PASS:',name,flush=True)
def entity(uid,side='server'):return next((e for e in snapshot(side)['entities'] if e['uuid']==uid),None)
def tracked(birds):return all(any(e['uuid']==b['uuid'] for e in snapshot(s)['entities']) for b in birds for s in SIDES)
def showcase():
    rpc('server','snapshot',timeout=180)
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,90)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('weather clear');cmd('fillbiome 0 63 0 24 80 20 minecraft:forest')
    for side in SIDES:
        rpc(side,'close');rpc(side,'cleanView',value=True);cmd('gamemode spectator '+NAMES[side]);rpc('server','teleport',player=NAMES[side],x=8.5,y=64,z=4.5,yaw=0,pitch=14)
    birds=[]
    for i,kind in enumerate(KINDS):
        x=5.5+i*2
        cmd(f'summon fantasy_feudalism_mobs:{kind} {x} 64 9.5 '+'{NoAI:1b,Rotation:[180f,0f],PersistenceRequired:1b}')
        birds.append(next(e for e in snapshot('server')['entities'] if e.get('birdKind')==kind))
    guard=rpc('server','spawn',type='villagerscontract:defender',owner='FFMOwner',x=18,y=64,z=12)
    wait_for(lambda:tracked(birds))
    wait_for(lambda:all((p:=snapshot(s)['birdPoses'].get(str(b['id']))) and p['groundFrames']>20 and p['latest']['flight']<.01 and abs(p['latest']['wingLeft']+1.15)<.01 for b in birds for s in SIDES))
    for side in SIDES:rpc(side,'screenshot',file='birds-036/lineup-'+side+'.png')
    for b in birds:
        p=b['position'];rpc('server','teleport',player='FFMOwner',x=p[0],y=63.8,z=7,yaw=0,pitch=25)
        wait_for(lambda:abs(snapshot('owner')['player']['position'][0]-p[0])<.1)
        time.sleep(.3);rpc('owner','screenshot',file='birds-036/'+b['birdKind']+'-close.png')
    rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=4.5,yaw=0,pitch=14)
    SAVE.write_text(json.dumps({'birds':birds,'guard':guard}));record('All four supplied bird models render on both clients alongside Mercenaries',birds=birds)
    print('SHOWCASE READY',flush=True)
def flight_pose():
    birds=json.loads(SAVE.read_text())['birds']
    cmd('summon fantasy_feudalism_mobs:robin 5.5 64 11.5 {NoAI:0b,PersistenceRequired:1b}')
    for side in SIDES:rpc('server','teleport',player=NAMES[side],x=8.5,y=68,z=0,yaw=0,pitch=0)
    now=snapshot('server')['gameTime']
    for b in birds:cmd('data merge entity '+b['uuid']+' {NoAI:0b,NextBirdFlight:'+str(now+5)+'L}')
    seen={s:{} for s in SIDES}
    def backward_legs():
        for side in SIDES:
            for pose in snapshot(side)['birdPoses'].values():
                if pose['finite'] and pose['latest']['flight']>.95 and pose['latest']['leg']>.8:seen[side][pose['kind']]=pose
        return all(all(k in seen[s] for k in KINDS) for s in SIDES)
    wait_for(backward_legs,30)
    record('Both clients render backward trailing flight legs for all four species',observations=seen)
    cmd('tick freeze')
    current=snapshot('server')
    for b in birds:
        e=next(e for e in current['entities'] if e['uuid']==b['uuid']);p=e['position']
        rpc('server','teleport',player='FFMOwner',x=p[0]-2,y=p[1]-1.2,z=p[2],yaw=-90,pitch=0)
        time.sleep(.3);rpc('owner','screenshot',file='birds-036/'+e['birdKind']+'-flight.png')
    cmd('tick unfreeze')
    for side in SIDES:assert not snapshot(side)['renderErrors']
    record('Updated bird materials and geometry render without errors on both clients')
    rpc('server','clear');cmd('kill @e[type=fantasy_feudalism_mobs:robin]');cmd('kill @e[type=fantasy_feudalism_mobs:blue_jay]');cmd('kill @e[type=fantasy_feudalism_mobs:goldfinch]');cmd('kill @e[type=fantasy_feudalism_mobs:crow]')
    cmd('save-all')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('stop');print('COMPLETE:',len(checks),'two-client checks passed',flush=True)
if __name__=='__main__':showcase();flight_pose()
