"""Real two-client Passable Foliage collision transitions with Kiwi and Mercenaries installed."""
import json,time
from control import rpc,snapshot,wait_for,TEST_ROOT
SIDES=('owner','guest');NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
RESULTS=TEST_ROOT/'birds-0.1.37-results.json';checks=[]
def cmd(command):return rpc('server','command',command=command)
def record(name,**evidence):
    checks.append({'name':name,'passed':True,**evidence});RESULTS.write_text(json.dumps(checks,indent=2));print('PASS:',name,flush=True)
def run():
    rpc('server','snapshot',timeout=180)
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,90)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('weather clear')
    for side in SIDES:
        rpc(side,'close');rpc(side,'cleanView',value=True);cmd('gamemode spectator '+NAMES[side]);rpc('server','teleport',player=NAMES[side],x=9.5,y=67,z=4.5,yaw=0,pitch=12)
    birds=rpc('server','birdFoliage',phase='fixture');ids=[b['id'] for b in birds];uuids={b['uuid'] for b in birds}
    guard=rpc('server','spawn',type='villagerscontract:defender',owner='FFMOwner',x=18,y=64,z=12)
    def observed(flying):
        evidence={s:[e for e in snapshot(s)['entities'] if e['uuid'] in uuids] for s in SIDES}
        return evidence if all(len(evidence[s])==4 and all(e['flying']==flying and e['leavesBelow'] and e['supportCollisionEmpty']==flying for e in evidence[s]) for s in SIDES) else None
    evidence=wait_for(lambda:observed(False),30)
    record('Both real clients connect with Kiwi, Passable Foliage and Mercenaries and see all four birds supported on leaves',observations=evidence,guard=guard)
    time.sleep(2) # Let asynchronous client chunk meshes catch up with block-state updates.
    for side in SIDES:rpc(side,'screenshot',file='birds-037/perched-'+side+'.png')
    rpc('server','birdFoliage',phase='flight',entities=ids)
    evidence=wait_for(lambda:observed(True),30)
    record('Taking off makes leaf collision empty for every bird on both clients',observations=evidence)
    crossing=rpc('server','birdFoliage',phase='cross',entities=ids)
    assert all(e['flying'] and e['position'][1]<67 for e in crossing),crossing
    record('Actual server movement carries all four flying birds through the leaf canopy',birds=crossing)
    rpc('server','birdFoliage',phase='perch',entities=ids)
    evidence=wait_for(lambda:observed(False),30)
    record('Returning to a perch restores leaf support on both clients',observations=evidence)
    for side in SIDES:assert not snapshot(side)['renderErrors'],snapshot(side)['renderErrors']
    record('No bird rendering errors occur during flight and perch transitions')
    for b in birds:rpc('server','remove',entity=b['id'])
    rpc('server','clear')
    cmd('fill 4 67 8 15 67 10 air');cmd('save-all')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('stop');print('COMPLETE:',len(checks),'two-client checks passed',flush=True)
if __name__=='__main__':run()
