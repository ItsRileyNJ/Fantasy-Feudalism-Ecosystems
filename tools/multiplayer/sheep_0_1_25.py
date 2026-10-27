"""Observe synchronized sheep cold, native shivering and recovery on two real clients."""
import json,time
from control import rpc,snapshot,wait_for,TEST_ROOT

SIDES=('owner','guest')
NAMES={'owner':'FFMOwner','guest':'FFMGuest'}
checks=[]
def cmd(command):return rpc('server','command',command=command)
def record(name,**evidence):
    checks.append({'name':name,'passed':True,**evidence})
    (TEST_ROOT/'sheep-0.1.25-results.json').write_text(json.dumps(checks,indent=2))
    print('PASS:',name,flush=True)
def run():
    rpc('server','snapshot',timeout=180)
    for side in SIDES:rpc(side,'snapshot',timeout=180);rpc(side,'connect')
    wait_for(lambda:len(snapshot('server')['players'])==2,90)
    rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('weather clear')
    cmd('fillbiome 0 63 0 20 72 16 minecraft:snowy_plains')
    for side,x in [('owner',7.8),('guest',9.2)]:
        rpc(side,'close');cmd('gamemode creative '+NAMES[side])
        rpc('server','teleport',player=NAMES[side],x=x,y=64,z=5.5,yaw=0,pitch=10)
    cmd('summon minecraft:sheep 8.5 64 8.5 {NoAI:1b,Sheared:1b,PersistenceRequired:1b,Tags:["ffm_sheep_test"]}')
    sheep=wait_for(lambda:next((e for e in snapshot('server')['entities'] if e['type']=='minecraft:sheep'),None))
    uid=sheep['uuid']
    def state(side):return next((e for e in snapshot(side)['entities'] if e['uuid']==uid),None)
    def both(cold):
        states={side:state(side) for side in SIDES}
        return states if all(s and s['cold']==cold and s['shivering']==cold for s in states.values()) else None
    cold=wait_for(lambda:both(True))
    assert all(abs(s['speed']-.23*.75)<1e-6 for s in cold.values()),cold
    wait_for(lambda:all(snapshot(side)['sheepRenderFrames'].get(str(sheep['id']),0)>10 for side in SIDES))
    record('Both clients receive cold metadata, reduced speed and the native shivering renderer',sheep=uid,clients=cold)
    # Stay sheltered for the full real server timer; check near its middle before recovery.
    cmd('setblock 8 67 8 minecraft:glass')
    start=snapshot('server')['gameTime']
    wait_for(lambda:snapshot('server')['gameTime']>=start+300,25)
    assert both(True)
    record('Glass shelter preserves cold and shivering halfway through the thirty-second recovery',clients=both(True))
    warm=wait_for(lambda:both(False),30)
    elapsed=snapshot('server')['gameTime']-start
    assert 600<=elapsed<=660,elapsed
    assert all(abs(s['speed']-.23)<1e-6 for s in warm.values()),warm
    record('Both clients stop shivering and regain normal speed after thirty seconds of shelter',elapsedServerTicks=elapsed,clients=warm)
    rpc('owner','reload',timeout=90)
    wait_for(lambda:not snapshot('owner')['loadingOverlay'],90)
    wait_for(lambda:both(False))
    cmd('setblock 8 67 8 minecraft:air');wait_for(lambda:both(True))
    record('Resource reload and renewed outdoor exposure preserve correct rendering and synchronization',clients=both(True))
    for side in SIDES:
        errors=snapshot(side)['renderErrors'];assert not errors,errors
        rpc(side,'screenshot',file='sheep-025/'+side+'.png')
    cmd('kill @e[tag=ffm_sheep_test]');cmd('fillbiome 0 63 0 20 72 16 minecraft:plains');cmd('weather clear')
    record('Both clients render sheep without errors while Mercenaries is installed')
    for side in SIDES:rpc(side,'disconnect');rpc(side,'quit')
    cmd('save-all');cmd('stop')
    print('COMPLETE:',len(checks),'two-client checks passed',flush=True)

if __name__=='__main__':run()
