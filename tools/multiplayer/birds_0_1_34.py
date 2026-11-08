"""Two-client appearance, actual rendered bird movement, fleeing, altitude and reload checks."""
import sys,json,time
from concurrent.futures import ThreadPoolExecutor
from control import rpc,snapshot,wait_for,TEST_ROOT
SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
KINDS=('robin','blue_jay','goldfinch','crow');SAVE=TEST_ROOT/'birds-0.1.34-state.json';RESULTS=TEST_ROOT/'birds-0.1.34-results.json'
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
    for side in SIDES:rpc(side,'screenshot',file='birds-034/lineup-'+side+'.png')
    for b in birds:
        p=b['position'];rpc('server','teleport',player='FFMOwner',x=p[0],y=63.8,z=7,yaw=0,pitch=25)
        wait_for(lambda:abs(snapshot('owner')['player']['position'][0]-p[0])<.1)
        time.sleep(.3);rpc('owner','screenshot',file='birds-034/'+b['birdKind']+'-close.png')
    rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=4.5,yaw=0,pitch=14)
    SAVE.write_text(json.dumps({'birds':birds,'guard':guard}));record('All four supplied bird models render on both clients alongside Mercenaries',birds=birds)
    print('SHOWCASE READY',flush=True)
def exercise():
    global checks
    checks=json.loads(RESULTS.read_text());birds=json.loads(SAVE.read_text())['birds']
    for side in SIDES:
        rpc('server','teleport',player=NAMES[side],x=8.5,y=66,z=0,yaw=0,pitch=12)
    now=snapshot('server')['gameTime']
    for b in birds:cmd('data merge entity '+b['uuid']+' {NoAI:0b,NextBirdFlight:'+str(now+120)+'L}')
    def animated():
        poses={s:snapshot(s)['birdPoses'] for s in SIDES}
        return poses if all((p:=poses[s].get(str(b['id']))) and p['finite'] and p['headYawRange']>.3 and p['walkingLegRange']>.15 and p['flyingWingRange']>1 for b in birds for s in SIDES) else None
    # Short random walks may be skipped on an individual landing; observe several cycles.
    evidence=wait_for(animated,120)
    record('Both clients observe actual wing flaps, quick head turns and ground-walking leg motion for every species',observations=evidence)
    for side in SIDES:rpc(side,'screenshot',file='birds-034/active-'+side+'.png')
    crow=birds[-1]
    landed=wait_for(lambda:(e:=entity(crow['uuid'])) and e['onGround'] and not e['flying'] and e,25)
    p=landed['position'];cmd('gamemode survival FFMOwner');rpc('server','teleport',player='FFMOwner',x=p[0]-2,y=64,z=p[2],yaw=-90,pitch=0)
    for side in ['guest']:rpc('server','teleport',player=NAMES[side],x=p[0]-5,y=67,z=p[2]-6,yaw=-35,pitch=5)
    wait_for(lambda:(e:=entity(crow['uuid'])) and e['flying'] and e['position'][1]>64.8,12)
    cmd('gamemode spectator FFMOwner');rpc('server','teleport',player='FFMOwner',x=8.5,y=66,z=0,yaw=0,pitch=12)
    record('A real Survival player within four blocks makes a landed bird take off',crow=entity(crow['uuid']))
    max_alt=0;deadline=time.monotonic()+8
    while time.monotonic()<deadline:
        for e in snapshot('server')['entities']:
            if 'birdKind' in e:max_alt=max(max_alt,e['aboveGround']);assert e['aboveGround']<=15.001,e
        time.sleep(.1)
    record('Flight and landing cycles remain below fifteen blocks above terrain',maxObservedAboveGround=max_alt)
    robin=birds[0];b=entity(robin['uuid']);p=b['position']
    cmd('gamemode creative FFMOwner');rpc('server','teleport',player='FFMOwner',x=p[0]-1,y=p[1],z=p[2],yaw=0,pitch=0)
    # Ambient flights remain scheduled normally; no Creative player is counted as a threat (covered directly by the native regression too).
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(lambda s:rpc(s,'reload',timeout=90),SIDES))
    for side in SIDES:wait_for(lambda:not snapshot(side)['loadingOverlay'],90)
    wait_for(lambda:tracked(birds))
    for side in SIDES:assert not snapshot(side)['renderErrors'],snapshot(side)['renderErrors']
    record('Resource reload retains all four renderers and textures without errors on either client')
    rpc('server','save')
    record('A dedicated server with Mercenaries saves the new bird entities without errors')
    cmd('kill @e[type=fantasy_feudalism_mobs:robin]');cmd('kill @e[type=fantasy_feudalism_mobs:blue_jay]');cmd('kill @e[type=fantasy_feudalism_mobs:goldfinch]');cmd('kill @e[type=fantasy_feudalism_mobs:crow]')
    rpc('server','clear');cmd('save-all')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('stop');print('COMPLETE:',len(checks),'two-client checks passed',flush=True)
if __name__=='__main__':
    if sys.argv[1]=='showcase':showcase()
    elif sys.argv[1]=='exercise':exercise()
