"""Two-client Mercenaries/bat integration, capture races, and dedicated-server restart checks."""
import sys,json,time
from concurrent.futures import ThreadPoolExecutor
from control import rpc,snapshot,wait_for,TEST_ROOT

MOD='fantasy_feudalism_mobs:'
PATH=TEST_ROOT/'server-stability-0.1.24-results.json'
SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
checks=[]
def save():PATH.write_text(json.dumps(checks,indent=2))
def check(name,**data):checks.append({'name':name,'passed':True,**data});save();print('PASS:',name,flush=True)
def cmd(s):return rpc('server','command',command=s)
def home(x=8):return rpc('server','enclosure',x=x,y=65,z=8)
def connect():
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
def inventory(side):
    rpc('server','inventory',player=NAMES[side],slots=[{'slot':0,'item':MOD+'bat_net'}]);rpc(side,'select',slot=0)
def both_models(holder,occupied):
    expected=MOD+'item/bat_net'+('_occupied' if occupied else '')
    def ready():
        models={side:rpc(side,'heldItemModel',player=NAMES[holder]) for side in SIDES}
        return models if all(m['occupied']==occupied and m['sprite']==expected and not m['customRenderer'] for m in models.values()) else None
    return wait_for(ready)
def tracked(entity):wait_for(lambda:all(any(e['id']==entity['id'] for e in snapshot(side)['entities']) for side in SIDES))
def capture(side):
    bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65.4,z=6.8);tracked(bat)
    rpc(side,'interact',entity=bat['id']);both_models(side,True)
    wait_for(lambda:snapshot(side)['player']['inventory'][0].get('batUUID')==bat['uuid']);return bat

def prepare():
    rpc('server','snapshot',timeout=180);connect();rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000')
    for side,x in [('owner',7.8),('guest',9.2)]:
        rpc(side,'close');rpc(side,'shift',value=False);rpc(side,'cleanView',value=True);cmd('gamemode survival '+NAMES[side])
        rpc('server','teleport',player=NAMES[side],x=x,y=64,z=6.2,yaw=0,pitch=0);inventory(side);both_models(side,False)
    cmd('setblock 8 64 8 minecraft:stone');cmd('setblock 8 65 8 '+MOD+'bat_enclosure[facing=north]')
    guard=rpc('server','spawn',type='villagerscontract:defender',owner='FFMOwner',x=10,y=64,z=8);tracked(guard)
    bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65.4,z=6.8);tracked(bat)
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(lambda side:rpc(side,'interact',entity=bat['id']),SIDES))
    def winner_ready():
        p={side:snapshot(side)['player']['inventory'][0].get('batUUID') for side in SIDES}
        return next((side for side in SIDES if p[side]==bat['uuid']),None)
    winner=wait_for(winner_ready);loser=next(s for s in SIDES if s!=winner)
    before=both_models(winner,True);both_models(loser,False)
    state=snapshot('server');holders=[p for p in state['players'] if any(i.get('batUUID')==bat['uuid'] for i in p['inventory'])]
    assert len(holders)==1 and not any(e['uuid']==bat['uuid'] for e in state['entities']),state
    check('Competing real-client captures produce one captured bat, with synchronized owner and observer sprites',winner=winner,bat=bat['uuid'],models=before,mercenary=guard)
    rpc(winner,'useBlock',x=8,y=65,z=8);both_models(winner,False);assert home()['ids']==[bat['uuid']]
    check('Successful transfer preserves the original UUID and resets the sprite on both clients',home=home())
    carried=capture('owner');check('Both clients observe a second occupied net before restart',bat=carried['uuid'],models=both_models('owner',True))
    (TEST_ROOT/'server-stability-0.1.24-restart.json').write_text(json.dumps({'stored':bat['uuid'],'carried':carried['uuid'],'guard':guard['uuid']}))
    rpc('server','save')
    for side in SIDES:rpc(side,'disconnect')
    wait_for(lambda:len(snapshot('server')['players'])==0);cmd('stop')
    print('PREPARE COMPLETE: restart the dedicated server, then run verify',flush=True)

def verify():
    global checks
    checks=json.loads(PATH.read_text());expected=json.loads((TEST_ROOT/'server-stability-0.1.24-restart.json').read_text())
    rpc('server','snapshot',timeout=180);connect()
    assert home()['ids']==[expected['stored']]
    carried=snapshot('owner')['player']['inventory'][0];assert carried['batUUID']==expected['carried']
    state=snapshot('server');assert not any(e['uuid'] in [expected['stored'],expected['carried']] for e in state['entities'])
    check('Dedicated-server restart preserves enclosure and captured-item identities without duplicate world bats',home=home(),models=both_models('owner',True))
    rpc('owner','useBlock',x=8,y=65,z=8);both_models('owner',False)
    assert set(home()['ids'])=={expected['stored'],expected['carried']}
    third=capture('guest');rpc('guest','useBlock',x=8,y=65,z=8);both_models('guest',False)
    assert home()['inside']==3
    fourth=capture('owner');rpc('owner','useBlock',x=8,y=65,z=8,count=10)
    models=both_models('owner',True)
    assert home()['inside']==3 and fourth['uuid'] not in home()['ids']
    assert snapshot('owner')['player']['inventory'][0]['batUUID']==fourth['uuid']
    check('A full enclosure rejects repeated inserts without losing the bat or resetting its occupied sprite',home=home(),models=models)
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(lambda side:rpc(side,'reload',timeout=90),SIDES))
    both_models('owner',True)
    for side in SIDES:rpc(side,'installBatAttachmentProbe');rpc(side,'resetEnclosurePoseProbe')
    time.sleep(3)
    observations={}
    for side in SIDES:
        probes=rpc(side,'batAttachmentProbe');samples={k:v for k,v in probes.items() if k in home()['ids']}
        assert len(samples)==3 and all(v['frames']>10 and v['maxRoofError']<.00001 for v in samples.values()),(side,samples)
        assert not snapshot(side)['renderErrors']
        observations[side]=samples;rpc(side,'screenshot',file='server-stability-024/'+side+'.png')
    check('Both clients reload resources and render three roof-attached bats correctly with Mercenaries installed',observations=observations)
    cmd('setblock 11 64 8 minecraft:stone');cmd('setblock 11 65 8 '+MOD+'bat_enclosure[facing=north]')
    rpc('server','teleport',player='FFMOwner',x=11.5,y=64,z=6.2,yaw=0,pitch=0)
    rpc('owner','useBlock',x=11,y=65,z=8);both_models('owner',False)
    assert home(11)['ids']==[fourth['uuid']]
    final=home()['ids']+home(11)['ids'];assert len(final)==len(set(final))==4
    check('All four original bat identities are conserved across both enclosures and nets return to their empty sprites',bats=final)
    rpc('server','save');print('TWO-CLIENT SERVER CHECKS COMPLETE',flush=True)

if __name__=='__main__':
    if sys.argv[1]=='prepare':prepare()
    elif sys.argv[1]=='verify':verify()
    else:raise SystemExit('Expected prepare or verify')
