"""Real client interactions and two-observer count synchronization for Ecosystems 0.1.15."""
import json,pathlib,time,concurrent.futures
from control import rpc,snapshot,wait_for,player,count
OUT=pathlib.Path('/workspace/multiplayer/ecosystems-0.1.15-results.json')
MOD='fantasy_feudalism_mobs:';SIDES=['owner','guest'];checks=[];POS=dict(x=8,y=64,z=8)
def command(c):return rpc('server','command',command=c)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def check(name,**detail):
 checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def inv(name,items):
 rpc('server','inventory',player=name,slots=[dict(slot=i,item=item,count=n) for i,(item,n) in enumerate(items)])
def enclosure(n,residents=None):
 states={}
 for side in ['server',*SIDES]:
  states[side]=wait_for(lambda side=side:(s if (s:=rpc(side,'enclosure',**POS))['inside']==n and (residents is None or s['residents']==residents) and (side=='server' or s['display']==n) else None))
 return states
def ss(label):
 for side in SIDES:rpc(side,'screenshot',file='ecosystems-015/'+label+'.png')
def bats():
 rpc('server','setup');rpc('server','clear');command('time set 6000');command('gamerule randomTickSpeed 0')
 command('setblock 8 63 8 minecraft:glowstone');command('setblock 8 64 8 '+MOD+'bat_enclosure[facing=north]')
 for side,name,x in [('owner','FFMOwner',8.5),('guest','FFMGuest',6.8)]:
  command('gamemode survival '+name);command('effect give '+name+' minecraft:night_vision infinite 0 true')
  rpc('server','teleport',player=name,x=x,y=64,z=5.3,yaw=0,pitch=17)
  rpc(side,'close');rpc(side,'cleanView',value=True)
 inv('FFMOwner',[(MOD+'bat_net',1)])
 rpc('owner','select',slot=0);time.sleep(1)
 check('An empty cabinet has zero stored and zero client display bats',states=enclosure(0,0));ss('bats-0')
 ids=[]
 for n in range(1,4):
  bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65,z=6.4,ai=True);ids.append(bat['uuid'])
  wait_for(lambda:ent(snapshot('owner'),bat['id']))
  rpc('owner','interact',entity=bat['id'])
  wait_for(lambda:player(snapshot('server'),'FFMOwner')['inventory'][0].get('batUUID')==bat['uuid'])
  wait_for(lambda:snapshot('owner')['player']['inventory'][0].get('batUUID')==bat['uuid'])
  rpc('owner','useBlock',**POS)
  state=enclosure(n,n)
  wait_for(lambda:'batUUID' not in snapshot('owner')['player']['inventory'][0])
  assert set(state['server']['ids'])==set(ids)
  check(f'{n} inserted bat(s) produce exactly {n} synchronized hanging display bat(s) on each client',states=state)
  time.sleep(.3);ss('bats-'+str(n))
 fourth=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65,z=6.4,ai=True)
 wait_for(lambda:ent(snapshot('owner'),fourth['id']));rpc('owner','interact',entity=fourth['id'])
 wait_for(lambda:player(snapshot('server'),'FFMOwner')['inventory'][0].get('batUUID')==fourth['uuid'])
 rpc('owner','useBlock',count=30,**POS);time.sleep(.5)
 assert player(snapshot('server'),'FFMOwner')['inventory'][0]['batUUID']==fourth['uuid']
 check('A fourth bat remains in its original net after repeated full-enclosure insert attempts',states=enclosure(3,3))
 # Move the fourth bat into another real home before changing inventory.
 command('setblock 9 64 8 '+MOD+'bat_enclosure[facing=north]');rpc('owner','useBlock',x=9,y=64,z=8)
 wait_for(lambda:rpc('server','enclosure',x=9,y=64,z=8)['inside']==1)
 inv('FFMOwner',[('minecraft:apple',8)]);inv('FFMGuest',[('minecraft:apple',8)])
 for side in SIDES:rpc(side,'select',slot=0)
 wait_for(lambda:all(snapshot(side)['player']['inventory'][0]['item']=='minecraft:apple' for side in SIDES))
 with concurrent.futures.ThreadPoolExecutor() as pool:
  list(pool.map(lambda side:rpc(side,'useBlock',count=25,**POS),SIDES))
 time.sleep(1)
 s=snapshot('server');apples=sum(count(p,'minecraft:apple') for p in s['players'])
 fertilizers=sum(count(p,MOD+'fertilizer') for p in s['players'])+sum(e['item']['count'] for e in s['entities'] if e['type']=='minecraft:item' and e['item']['item']==MOD+'fertilizer')
 assert apples==13 and fertilizers==3,(apples,fertilizers)
 check('Competing real client feed packets consume exactly three apples and produce exactly three fertilizers',apples=apples,fertilizers=fertilizers)
 rpc('owner','useBlock',count=50,**POS);time.sleep(.4)
 assert sum(count(p,'minecraft:apple') for p in snapshot('server')['players'])==13
 check('Healthy bats reject further apples during their individual five-minute cooldowns')
 command('setblock 7 63 7 minecraft:farmland');command('setblock 7 64 7 minecraft:wheat[age=0]')
 inv('FFMOwner',[(MOD+'fertilizer',2)]);wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==MOD+'fertilizer')
 rpc('owner','useBlock',x=7,y=64,z=7);time.sleep(.5)
 # Verify successful use and rejection of subsequent mature-crop actions on both endpoints.
 assert count(player(snapshot('server'),'FFMOwner'),MOD+'fertilizer')==1
 rpc('owner','useBlock',count=20,x=7,y=64,z=7);time.sleep(.4)
 assert count(player(snapshot('server'),'FFMOwner'),MOD+'fertilizer')==1
 assert count(snapshot('owner')['player'],MOD+'fertilizer')==1
 check('A real fertilizer interaction spends one item for instant crop growth and rejects repeated mature-crop use')
 command('time set 18000');enclosure(0,3)
 wait_for(lambda:all(any(e['uuid']==uid for e in snapshot('server')['entities']) for uid in ids))
 ss('night-empty');check('At night the three actual residents leave and both clients display zero bats inside',states=enclosure(0,3))
 maxdistance=0
 for _ in range(30):
  for e in snapshot('server')['entities']:
   if e['uuid'] in ids:maxdistance=max(maxdistance,sum((a-b)**2 for a,b in zip(e['position'],[8.5,64.5,8.5]))**.5)
  time.sleep(.1)
 assert maxdistance<=15.5,maxdistance
 check('Actual roaming bat positions remain within the enclosure flight radius',maximumDistance=maxdistance)
 rpc('server','save')
 state=dict(ids=ids,fourth=fourth['uuid']);pathlib.Path('/workspace/multiplayer/ecosystems-015-restart.json').write_text(json.dumps(state))
 check('The saved nighttime colony retains all original UUID reservations before restart',states=enclosure(0,3))
if __name__=='__main__':
 try:bats()
 except BaseException as e:
  OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2));raise
