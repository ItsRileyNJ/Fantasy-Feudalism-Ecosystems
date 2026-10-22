"""Resume the saved real colony after a normal server restart, then dawn and resource reload."""
import json,pathlib,time
from control import rpc,snapshot,wait_for,player,count
from ecosystems_0_1_15 import enclosure,check,checks,OUT,ss,command,MOD,SIDES
state=json.loads(pathlib.Path('/workspace/multiplayer/ecosystems-015-restart.json').read_text())
ids=set(state['ids']);allids=ids|{state['fourth']}
def run():
 prior=json.loads(OUT.read_text());checks.extend(prior['checks'])
 wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
 # All bats were genuinely roaming when saved; the server restores those original entities.
 s=wait_for(lambda:(s if (s:=snapshot('server')) and allids.issubset({e['uuid'] for e in s['entities']}) else None),timeout=30)
 bats=[e for e in s['entities'] if e['type']=='minecraft:bat'];assert len(bats)==4
 check('A normal restart restores exactly the four original roaming bat identities, without replacement or duplication',bats=bats,states=enclosure(0,3))
 command('time set 24000');states=enclosure(3,3)
 wait_for(lambda:not any(e['uuid'] in allids for e in snapshot('server')['entities']),timeout=30)
 check('At dawn the original bats return; both clients display exactly three hanging occupants again',states=states)
 ss('dawn-three')
 for side in SIDES:rpc(side,'reload',timeout=60)
 check('Resource reload rebuilds exactly the real resident count on both clients',states=enclosure(3,3))
 # Feed cooldown must survive both the roaming entity save and stored return.
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item='minecraft:apple',count=5)])
 rpc('owner','select',slot=0);wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']=='minecraft:apple')
 rpc('owner','useBlock',x=8,y=64,z=8,count=50);time.sleep(.5)
 assert count(player(snapshot('server'),'FFMOwner'),'minecraft:apple')==5
 check('A save, normal restart, night flight and morning return cannot reset fertilizer cooldowns')
 # The real server destroys the cabinet; no item NBT clones its stored residents.
 command('setblock 8 64 8 minecraft:air destroy')
 wait_for(lambda:ids.issubset({e['uuid'] for e in snapshot('server')['entities']}))
 s=snapshot('server');bats=[e for e in s['entities'] if e['type']=='minecraft:bat']
 assert len(bats)==3 and {e['uuid'] for e in bats}==ids
 for side in SIDES:assert not snapshot(side)['renderErrors']
 check('Breaking the occupied cabinet releases exactly its three original bats and keeps the other colony stored',bats=bats)
 command('setblock 8 64 8 '+MOD+'bat_enclosure[facing=north]')
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=MOD+'bat_net',count=1)])
 wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==MOD+'bat_net')
 enclosure(0,0);ss('bats-0')
 check('The final cabinet model has no decorative bats when empty',states=enclosure(0,0))
 for n,uid in enumerate(sorted(ids),1):
  bat=next(e for e in snapshot('server')['entities'] if e['uuid']==uid)
  x,y,z=bat['position'];rpc('server','teleport',player='FFMOwner',x=x,y=y,z=z-1,yaw=0,pitch=0)
  wait_for(lambda:next((e for e in snapshot('owner')['entities'] if e['id']==bat['id']),None))
  rpc('owner','interact',entity=bat['id'])
  wait_for(lambda:player(snapshot('server'),'FFMOwner')['inventory'][0].get('batUUID')==uid)
  rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=5.3,yaw=0,pitch=17)
  rpc('owner','useBlock',x=8,y=64,z=8)
  states=enclosure(n,n);time.sleep(.4);ss('bats-'+str(n))
  check(f'Final visible models: exactly {n} stored bat(s), with no display shadows or roof clipping',states=states)

if __name__=='__main__':
 try:run()
 except BaseException as e:
  OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2));raise
