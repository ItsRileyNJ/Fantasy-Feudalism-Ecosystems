"""Actual two-client interactions for Mobs + the supplied Mercenaries 1.15.7 JAR."""
from control import *
from concurrent.futures import ThreadPoolExecutor

REPORT=TEST_ROOT/'mercenaries-results.json'
checks=[]
pool=ThreadPoolExecutor(max_workers=2)
def record(name,detail=None):
 checks.append({'name':name,'status':'passed','detail':detail});REPORT.write_text(json.dumps(checks,indent=2));print('PASS',name,json.dumps(detail) if detail else '',flush=True)
def sync():
 def equal():
  s=snapshot('server');cs={side:snapshot(side) for side in ['owner','guest']}
  if len(s['players'])!=2:return False
  for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
   c=cs[side]
   if not c.get('connected'):return False
   p=player(s,name)
   if c['player']['inventory']!=p['inventory'] or c['player']['cursor']!=p['cursor']:return False
   if p['menu']['type']=='GuardMenu':
    m=c['player']['menu'];expected=p['menu']
    if m['type']!='GuardMenu' or c['screen']!='GuardScreen':return False
    for k in ['id','slots','order','health','maxHealth','hunger','mountState']:
     if m[k]!=expected[k]:return False
  return s,cs
 return wait_for(equal,timeout=30)
def inventory(side,slots=(),fill=False):
 rpc('server','inventory',player='FFMOwner' if side=='owner' else 'FFMGuest',slots=list(slots),**({'fill':True} if fill else {}));rpc(side,'select',slot=0)
def tracked(id):
 return wait_for(lambda:all(any(e['id']==id for e in snapshot(side).get('entities',[])) for side in ['owner','guest']))
def together(action,**params):
 return [f.result() for f in [pool.submit(rpc,side,action,**params) for side in ['owner','guest']]]
def entity(s,id):return next(e for e in s['entities'] if e['id']==id)
def open_menus(id):
 for side in ['owner','guest']:
  rpc(side,'shift',value=True);rpc(side,'interact',entity=id)
 wait_for(lambda:all(snapshot(side)['player']['menu']['type']=='GuardMenu' for side in ['owner','guest']))
 return sync()
def close_menus():
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False)
 wait_for(lambda:all(p['menu']['type']=='InventoryMenu' for p in snapshot('server')['players']))
try:
 rpc('server','clear');rpc('server','setup')
 for side,name,z in [('owner','FFMOwner',0),('guest','FFMGuest',2)]:
  rpc(side,'close');rpc(side,'shift',value=False)
  rpc('server','teleport',player=name,x=0,y=64,z=z,yaw=-90,pitch=5)
  inventory(side,[{'slot':0,'item':'villagerscontract:villagers_seal','count':16}])
 sync()
 patrol=rpc('server','mercenary',op='patrol',count=2);captain=patrol[0]['id'];tracked(captain)
 together('interact',entity=captain,count=100)
 def converted():
  s=snapshot('server');guards=[e for e in s['entities'] if e['type']=='villagerscontract:defender']
  return (s,guards) if len(guards)==2 else False
 s,guards=wait_for(converted);s,cs=sync()
 assert sum(count(p,'villagerscontract:villagers_seal') for p in s['players'])==31
 assert not any(e['type']=='minecraft:pillager' for e in s['entities'])
 guard=min(guards,key=lambda e:e['position'][0]);gid=guard['id'];tracked(gid)
 record('200 competing contract packets redeem one two-pillager patrol exactly once and consume one contract')
 ownerId=player(s,'FFMOwner')['uuid']
 pets=[]
 for kind,z in [('spider',4),('cave_spider',-2)]:
  p=rpc('server','spawn',type='minecraft:'+kind,x=2,y=64,z=z,owner='FFMOwner');tracked(p['id']);pets.append(p)
  classified=rpc('server','mercenary',op='classify',entity=gid,other=p['id'])
  assert classified=={'hostile':False,'allied':True,'canAttack':False},classified
 record('Both spider companion types are allied to mercenaries and excluded from guard targeting')
 # Full inventories leave only one hidden, non-hotbar slot for the shared withdrawal.
 for side in ['owner','guest']:inventory(side,[{'slot':20,'item':'minecraft:air','count':0}],fill=True)
 rpc('server','mercenary',op='supplies',entity=gid,slots=[{'slot':2,'item':'minecraft:string','count':13}])
 s,cs=open_menus(gid)
 assert len(cs['owner']['player']['menu']['slots'])==25
 for _ in range(30):together('click',slot=11,type='QUICK_MOVE')
 s,cs=sync()
 assert sum(count(p,'minecraft:string') for p in s['players'])==13
 assert entity(s,gid)['guard']['supplies'][2]['count']==0
 assert all(c['player']['cursor']['count']==0 for c in cs.values())
 record('Two real GuardScreens compete for shared storage: 13 string conserved with authoritative hidden-inventory updates')
 # Arrow slots intentionally accept deposits but refuse extraction.
 before=[cs[side]['player']['menu']['slots'][21:25] for side in ['owner','guest']]
 for _ in range(10):together('click',slot=21)
 s,cs=sync()
 for i,side in enumerate(['owner','guest']):
  assert cs[side]['player']['menu']['slots'][21:25]==before[i] and cs[side]['player']['cursor']['count']==0
 record('Concurrent clicks cannot extract or duplicate the mercenary ammunition slots')
 together('button',button=1);s,cs=sync()
 assert entity(s,gid)['guard']['order']=='HOLD'
 assert all(c['player']['menu']['order']=='HOLD' for c in cs.values())
 record('Guard order buttons synchronize authoritative state to both open viewers')
 for scale in [1,2,3]:
  rpc('owner','guiScale',scale=scale);time.sleep(.6);rpc('owner','screenshot',file='mercenary-menu-scale-'+str(scale)+'.png')
  s,cs=sync();assert cs['owner']['screen']=='GuardScreen' and not cs['owner']['renderErrors']
 rpc('owner','guiScale',scale=2)
 record('Mercenary GUI renders and preserves synchronized slots at GUI scales 1, 2 and 3')
 close_menus()
 # Real hotbar-to-guard SWAP packets exercise combined item registries without bypassing menu validation.
 inventory('owner',[{'slot':0,'item':'minecraft:string','count':4}]);inventory('guest');s,cs=open_menus(gid)
 rpc('owner','click',slot=11,type='SWAP',button=0);s,cs=sync()
 assert count(player(s,'FFMOwner'),'minecraft:string')==0 and entity(s,gid)['guard']['supplies'][2]['count']==4
 rpc('guest','click',slot=11,type='QUICK_MOVE');s,cs=sync()
 assert count(player(s,'FFMGuest'),'minecraft:string')==4
 close_menus();inventory('owner',[{'slot':0,'item':'fantasy_feudalism_mobs:bee_bottle','count':3}]);s,cs=open_menus(gid)
 rpc('owner','click',slot=11,type='SWAP',button=0);s,cs=sync()
 assert count(player(s,'FFMOwner'),'fantasy_feudalism_mobs:bee_bottle')==3 and entity(s,gid)['guard']['supplies'][2]['count']==0
 record('Shared guard storage accepts vanilla string and safely rejects Bee Bottles without loss, duplication or desync')
 close_menus()
 # Exercise a real harvested poison potion against the positive-potion-only guard slots.
 rpc('server','teleport',player='FFMOwner',x=0,y=64,z=0,yaw=-90,pitch=5)
 rpc('server','teleport',player='FFMGuest',x=0,y=64,z=2,yaw=-90,pitch=5)
 pet=rpc('server','spawn',type='minecraft:spider',x=2,y=64,z=1,owner='FFMOwner');tracked(pet['id'])
 inventory('owner',[{'slot':0,'item':'minecraft:glass_bottle','count':2}]);rpc('owner','interact',entity=pet['id']);s,cs=sync()
 assert count(player(s,'FFMOwner'),'minecraft:potion')==1
 assert player(s,'FFMOwner')['inventory'][1]['potion']=='minecraft:poison'
 s,cs=open_menus(gid);rpc('owner','click',slot=7,type='SWAP',button=1);s,cs=sync()
 assert count(player(s,'FFMOwner'),'minecraft:potion')==1 and entity(s,gid)['guard']['supplies'][16]['count']==0
 record('Spider-harvested Poison remains intact when the guard GUI rejects a negative potion')
 close_menus()
 rpc('server','mercenary',op='op',player='FFMOwner');rpc('owner','command',command='ffm find')
 def highlighted():
  c=snapshot('owner');return any(e.get('privateHighlight') for e in c.get('entities',[]) if e['id']==gid)
 wait_for(highlighted)
 c=snapshot('guest');assert not any(e.get('privateHighlight') for e in c.get('entities',[]) if e['id']==gid)
 rpc('owner','screenshot',file='mercenary-private-highlight.png')
 record('Mercenary custom highlight payload reaches only its requesting client')
 for side in ['owner','guest']:rpc(side,'reload',timeout=90)
 s,cs=sync();assert all(not c['beeBottleModelMissing'] and not c['renderErrors'] for c in cs.values())
 s,cs=open_menus(gid);rpc('owner','screenshot',file='mercenary-reloaded-menu.png')
 record('Both mods survive resource reload with valid Bee Bottle models, guard rendering and usable GuardScreens')
 # Hold a cursor stack across logout: vanilla menu removal must return it once.
 rpc('server','mercenary',op='supplies',entity=gid,slots=[{'slot':2,'item':'minecraft:string','count':5}]);sync()
 rpc('guest','click',slot=11);s,cs=sync();assert cs['guest']['player']['cursor']['count']==5
 rpc('guest','disconnect');wait_for(lambda:len(snapshot('server')['players'])==1,timeout=5)
 rpc('guest','connect');wait_for(lambda:snapshot('guest').get('connected'),timeout=60);s,cs=sync()
 # Vanilla drops a disconnected player's cursor. A nearby guard can immediately loot it.
 # Count every holder rather than assuming that the stack returns to the player's inventory.
 strings=sum(count(p,'minecraft:string')+ (p['cursor']['count'] if p['cursor']['item']=='minecraft:string' else 0) for p in s['players'])
 strings+=sum(st['count'] for e in s['entities'] for st in e.get('guard',{}).get('supplies',[]) if st['item']=='minecraft:string')
 strings+=sum(e['stack']['count'] for e in s['entities'] if e.get('stack',{}).get('item')=='minecraft:string')
 assert strings==9 and player(s,'FFMGuest')['cursor']['count']==0,strings
 record('Disconnecting with a guard-menu cursor stack conserves all items across player, world-drop and guard-loot ownership; reconnect inventories agree')
 close_menus();rpc('server','save');s,cs=sync()
 saved={'guards':[e for e in s['entities'] if e['type']=='villagerscontract:defender'],'pets':[e for e in s['entities'] if e['type'] in ['minecraft:spider','minecraft:cave_spider']],'players':s['players']}
 (TEST_ROOT/'mercenaries-restart.json').write_text(json.dumps(saved,indent=2))
 print('MERCENARIES PHASE ONE COMPLETE',flush=True)
except Exception as e:
 checks.append({'name':'Failure','status':'failed','error':repr(e)});REPORT.write_text(json.dumps(checks,indent=2));raise
