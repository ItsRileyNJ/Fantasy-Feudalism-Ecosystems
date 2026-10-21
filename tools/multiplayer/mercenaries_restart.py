"""Run after a normal server save/stop/restart, with both test clients on title screens."""
from control import *
import uuid,struct

REPORT=TEST_ROOT/'mercenaries-results.json'
checks=json.loads(REPORT.read_text())
def record(name,detail=None):
 checks.append({'name':name,'status':'passed','detail':detail});REPORT.write_text(json.dumps(checks,indent=2));print('PASS',name,json.dumps(detail) if detail else '',flush=True)
def equal():
 s=snapshot('server');cs={side:snapshot(side) for side in ['owner','guest']}
 if len(s['players'])!=2:return False
 for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
  c=cs[side]
  if not c.get('connected'):return False
  p=player(s,name)
  if c['player']['inventory']!=p['inventory'] or c['player']['cursor']!=p['cursor']:return False
 return s,cs
try:
 before=json.loads((TEST_ROOT/'mercenaries-restart.json').read_text())
 rpc('server','snapshot',timeout=90)
 for side in ['owner','guest']:rpc(side,'connect')
 s,cs=wait_for(equal,timeout=90)
 for old in before['players']:
  now=player(s,old['name']);assert now['inventory']==old['inventory'] and now['cursor']==old['cursor']
 for old in before['guards']:
  now=next(e for e in s['entities'] if e['uuid']==old['uuid'])
  # Guards continue their hunger clock while the server runs before shutdown and after boot.
  # Persistent identity, orders, equipment and supplies must survive unchanged.
  for k in ['needsContract','isGuardCaptain','skin','order','supplies','weapons','redeemer']:
   assert now['guard'][k]==old['guard'][k],(k,old,now)
  assert now['health']==old['health'],(old,now)
  for side in cs:
   client=wait_for(lambda:next((e for e in snapshot(side)['entities'] if e['uuid']==old['uuid']),False))
   for k in ['needsContract','isGuardCaptain','hunger','skin']:assert client['guard'][k]==now['guard'][k]
   assert not client['privateHighlight']
 for old in before['pets']:
  now=next(e for e in s['entities'] if e['uuid']==old['uuid'])
  for k in ['owner','sitting','health','maxHealth','cooldowns']:assert now[k]==old[k],(k,old,now)
  for side in cs:
   client=wait_for(lambda:next((e for e in snapshot(side)['entities'] if e['uuid']==old['uuid']),False))
   for k in ['owner','sitting','maxHealth']:assert client[k]==now[k]
 record('Full process restart preserves both mods: guard contracts, equipment, supplies, orders and skins; spider ownership/cooldowns; both player inventories')
 pet=before['pets'][-1];livePet=next(e for e in s['entities'] if e['uuid']==pet['uuid'])
 rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':'minecraft:glass_bottle','count':2}]);rpc('owner','select',slot=0);wait_for(equal)
 rpc('owner','interact',entity=livePet['id'],count=100);s,cs=wait_for(equal)
 assert count(player(s,'FFMOwner'),'minecraft:glass_bottle')==2 and count(player(s,'FFMOwner'),'minecraft:potion')==0
 record('Both-mod restart and guard persistence cannot reset spider poison harvesting cooldown')
 guard=next(e for e in s['entities'] if e['uuid']==before['guards'][0]['uuid']);gid=guard['id']
 # Vanilla summon command creates a legitimately owned/saddled horse; the GUI performs mounting.
 rpc('server','mercenary',op='op',player='FFMOwner')
 ownerInts=','.join(str(x) for x in struct.unpack('>4i',uuid.UUID(guard['guard']['redeemer']).bytes))
 command='summon minecraft:horse 4 64 1 {Tame:1b,Owner:[I;'+ownerInts+'],SaddleItem:{id:"minecraft:saddle",count:1},PersistenceRequired:1b}'
 rpc('owner','command',command=command)
 wait_for(lambda:any(e['type']=='minecraft:horse' for e in snapshot('server')['entities']))
 for name,z in [('FFMOwner',0),('FFMGuest',2)]:rpc('server','teleport',player=name,x=3,y=64,z=z,yaw=-90,pitch=-12)
 rpc('server','mercenary',op='ai',entity=gid,value=True)
 rpc('owner','shift',value=True);rpc('owner','interact',entity=gid)
 wait_for(lambda:snapshot('owner')['player']['menu']['type']=='GuardMenu')
 rpc('owner','button',button=3);time.sleep(3)
 for side in ['owner','guest']:
  rpc(side,'shift',value=True);rpc(side,'interact',entity=gid)
 def mounted():
  s,cs=equal() or (None,None)
  if not s:return False
  return (s,cs) if all(c['player']['menu'].get('mountState')==2 for c in cs.values()) else False
 s,cs=wait_for(mounted,timeout=20)
 rpc('owner','screenshot',file='mercenary-mounted-menu.png')
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False);rpc(side,'look',yaw=-63 if side=='owner' else -116,pitch=-18)
 time.sleep(1);rpc('owner','screenshot',file='mercenary-mounted-world.png')
 for side in ['owner','guest']:rpc(side,'shift',value=True);rpc(side,'interact',entity=gid)
 wait_for(mounted);rpc('owner','button',button=3)
 wait_for(lambda:all(snapshot(side)['player']['menu'].get('mountState')==0 for side in ['owner','guest']))
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False)
 s,cs=wait_for(equal);assert all(not c['renderErrors'] for c in cs.values())
 record('Real guard GUI mounts and dismounts an owned horse with matching state and valid rendering on both clients')
 rpc('server','save')
 print('MERCENARIES RESTART PHASE COMPLETE',flush=True)
except Exception as e:
 checks.append({'name':'Failure','status':'failed','error':repr(e)});REPORT.write_text(json.dumps(checks,indent=2));raise
