"""Two real-client pig digging/model reset and mushroom interaction checks with Mercenaries."""
import json,time
from control import rpc,snapshot,wait_for,TEST_ROOT

SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
checks=[]
def cmd(command):return rpc('server','command',command=command)
def record(name,**evidence):
    checks.append({'name':name,'passed':True,**evidence})
    (TEST_ROOT/'pigs-0.1.26-results.json').write_text(json.dumps(checks,indent=2));print('PASS:',name,flush=True)
def run():
    rpc('server','snapshot',timeout=180)
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,90)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('weather clear')
    cmd('setblock 8 63 8 minecraft:podzol')
    for side,x in [('owner',7.8),('guest',9.2)]:
        rpc(side,'close');cmd('gamemode creative '+NAMES[side])
        rpc('server','teleport',player=NAMES[side],x=x,y=64,z=5.5,yaw=0,pitch=10)
    rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':'minecraft:pig_spawn_egg'}]);rpc('owner','select',slot=0)
    rpc('owner','useBlock',x=8,y=63,z=8)
    pig=wait_for(lambda:next((e for e in snapshot('server')['entities'] if e['type']=='minecraft:pig'),None))
    uid=pig['uuid'];eid=str(pig['id'])
    cmd('summon minecraft:pig 10.5 64 8.5 {NoAI:1b,PersistenceRequired:1b,Tags:["ffm_idle_pig"]}')
    idle=next(e for e in snapshot('server')['entities'] if e['type']=='minecraft:pig' and e['uuid']!=uid)
    cmd('data merge entity '+uid+' {Age:0,PersistenceRequired:1b,NeoForgeData:{ffm_pig_next_dig:0L}}')
    def poses(digging):
        states={side:snapshot(side) for side in SIDES}
        ps={side:s['pigPoses'].get(eid) for side,s in states.items()}
        if not all(p and p['digging']==digging and (p['amount']>.95 if digging else p['amount']<.01) for p in ps.values()):return None
        return ps
    digging=wait_for(lambda:poses(True))
    assert all(p['head']['y']>p['head']['restY']+2 for p in digging.values()),digging
    for side in SIDES:
        p=wait_for(lambda:snapshot(side)['pigPoses'].get(str(idle['id'])))
        assert not p['digging'] and abs(p['head']['y']-p['head']['restY'])<1e-6,p
        rpc(side,'screenshot',file='pigs-026/digging-'+side+'.png')
    record('A real client spawn egg creates a pig whose lowered-snouted digging animation synchronizes on both clients; idle pigs keep their normal pose',pig=uid,clients=digging)
    normal=wait_for(lambda:poses(False))
    assert all(abs(p['head']['y']-p['head']['restY'])<1e-6 for p in normal.values()),normal
    record('Both clients return to the native idle pose after a complete dig',clients=normal)
    cmd('data merge entity '+uid+' {Age:-24000,NoAI:1b}')
    for side in SIDES:
        cmd('gamemode survival '+NAMES[side]);rpc('server','inventory',player=NAMES[side],slots=[{'slot':0,'item':'minecraft:brown_mushroom','count':2}]);rpc(side,'select',slot=0)
        rpc('server','teleport',player=NAMES[side],x=8.5,y=64,z=7,yaw=0,pitch=10)
    wait_for(lambda:all(any(e['uuid']==uid and e['baby'] for e in snapshot(side)['entities']) for side in SIDES))
    before=next(e for e in snapshot('server')['entities'] if e['uuid']==uid)['age'];rpc('owner','interact',entity=pig['id'])
    after=next(e for e in snapshot('server')['entities'] if e['uuid']==uid)['age']
    assert abs(after-before*.8)<15,(before,after)
    wait_for(lambda:snapshot('owner')['player']['inventory'][0]['count']==1)
    record('A real Survival client feeds one mushroom and removes twenty percent of remaining piglet growth',beforeAge=before,afterAge=after)
    rpc('guest','reload',timeout=90);wait_for(lambda:not snapshot('guest')['loadingOverlay'],90)
    for side in SIDES:
        assert not snapshot(side)['renderErrors'],snapshot(side)['renderErrors']
    record('Resource reload and adult-to-piglet transition render without errors on both clients with Mercenaries installed')
    cmd('kill @e[type=minecraft:pig]');cmd('save-all')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('stop');print('COMPLETE:',len(checks),'two-client checks passed',flush=True)

if __name__=='__main__':run()
