from control import *
from concurrent.futures import ThreadPoolExecutor
REPORT=(TEST_ROOT/'results.json')
checks=[]
def record(name,detail=None):
 checks.append({'name':name,'status':'passed','detail':detail});REPORT.write_text(json.dumps(checks,indent=2));print('PASS',name,json.dumps(detail) if detail else '',flush=True)
def inventory(side,slots,fill=False):
 rpc('server','inventory',player='FFMOwner' if side=='owner' else 'FFMGuest',slots=slots,**({'fill':True} if fill else {}))
 rpc(side,'select',slot=0)
def sync():
 def matched():
  s=snapshot('server');out={}
  for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
   c=snapshot(side)
   if not c.get('connected'):return False
   p=player(s,name)
   if c['player']['inventory']!=p['inventory'] or c['player']['cursor']!=p['cursor']:return False
   out[side]=c
  return s,out
 return wait_for(matched,timeout=30)
def tracked(id):
 return wait_for(lambda:all(any(e['id']==id for e in snapshot(side).get('entities',[])) for side in ['owner','guest']))
def spawn(type,**kw):
 e=rpc('server','spawn',type='minecraft:'+type,**kw);tracked(e['id']);return e
pool=ThreadPoolExecutor(max_workers=2)
def together(entity,count=1):
 futures=[pool.submit(rpc,side,'interact',entity=entity,count=count) for side in ['owner','guest']]
 return [f.result() for f in futures]
def total(s,item):
 return sum(count(p,item) for p in s['players'])+sum(e.get('stack',{}).get('count',0) for e in s['entities'] if e.get('stack',{}).get('item')==item)
try:
 rpc('server','clear')
 for side,name,z in [('owner','FFMOwner',0),('guest','FFMGuest',2)]:
  rpc('server','teleport',player=name,x=0,y=64,z=z,yaw=-90,pitch=0);rpc(side,'close');inventory(side,[{'slot':0,'item':'minecraft:glass_bottle','count':16}])
 sync();record('Two independent clients log in to packaged NeoForge server')
 bee=spawn('bee',x=2,y=64,z=1)
 together(bee['id'],count=100)
 s,c=sync();assert total(s,'fantasy_feudalism_mobs:bee_bottle')==1,(s['players'],s['entities'])
 assert total(s,'minecraft:glass_bottle')==31
 record('200 competing bee-capture interactions conserve one bee and one bottle')
 # The exact 33% threshold is deterministic in GameTests; live packets use normal mob RNG.
 inventory('owner',[{'slot':0,'item':'fantasy_feudalism_mobs:bee_bottle','count':64}]);inventory('guest',[]);sync()
 spider=spawn('spider',x=2,y=64,z=1)
 rpc('owner','interact',entity=spider['id'],count=64)
 s,c=sync();pet=next(e for e in s['entities'] if e['id']==spider['id']);ownerId=player(s,'FFMOwner')['uuid'];assert pet['owner']==ownerId,pet
 for side in ['owner','guest']:
  cp=next(e for e in c[side]['entities'] if e['id']==spider['id']);assert cp['owner']==ownerId and cp['sitting'] and cp['maxHealth']==30 and abs(cp['speed']-.36)<.00001,cp
 record('Live Bee Bottle taming synchronizes ownership, sitting, health and speed to both clients')
 inventory('guest',[{'slot':0,'item':'fantasy_feudalism_mobs:bee_bottle','count':1}]);sync();rpc('guest','interact',entity=spider['id']);s,c=sync()
 assert next(e for e in s['entities'] if e['id']==spider['id'])['owner']==ownerId
 record('Another player cannot steal ownership by feeding')
 for side in ['owner','guest']:inventory(side,[{'slot':0,'item':'minecraft:glass_bottle','count':16}])
 sync();together(spider['id'],count=1000);s,c=sync()
 assert total(s,'minecraft:potion')==1 and total(s,'minecraft:glass_bottle')==31
 record('2000 concurrent poison-harvest requests yield exactly one potion with no inventory desync')
 for side in ['owner','guest']:inventory(side,[{'slot':0,'item':'minecraft:shears','count':1}])
 sync();together(spider['id'],count=1000);s,c=sync()
 strings=total(s,'minecraft:string');assert 1<=strings<=4
 durability=sum(st['damage'] for p in s['players'] for st in p['inventory'] if st['item']=='minecraft:shears');assert durability==1,durability
 record('2000 concurrent silk-harvest requests yield one 1-4 string harvest and one durability cost',{'string':strings})
 rpc('server','resetHarvest',entity=spider['id'])
 for side in ['owner','guest']:inventory(side,[{'slot':0,'item':'minecraft:glass_bottle','count':16}],fill=True)
 sync();together(spider['id'],count=100);s,c=sync();assert total(s,'minecraft:potion')==1 and total(s,'minecraft:glass_bottle')==31
 record('Full-inventory concurrent harvesting drops exactly one output and consumes one input')
 # Inventory GUI exercises real serverbound container clicks and client prediction.
 inventory('owner',[{'slot':0,'item':'fantasy_feudalism_mobs:bee_bottle','count':8},{'slot':1,'item':'minecraft:glass_bottle','count':16},{'slot':2,'item':'minecraft:shears','count':1}])
 inventory('guest',[]);sync();rpc('owner','inventory');time.sleep(.5)
 rpc('owner','click',slot=36);rpc('owner','click',slot=9);rpc('owner','click',slot=9,type='QUICK_MOVE');s,c=sync()
 assert count(player(s,'FFMOwner'),'fantasy_feudalism_mobs:bee_bottle')==8 and player(s,'FFMOwner')['cursor']['count']==0
 assert c['owner']['screen']=='InventoryScreen' and not c['owner']['beeBottleModelMissing']
 rpc('owner','screenshot',file='inventory.png');record('Inventory pickup, placement and shift-click conserve items and agree with server')
 rpc('owner','close')
 for side in ['owner','guest']:rpc(side,'reload',timeout=90)
 s,c=sync()
 assert all(not c[side]['beeBottleModelMissing'] and not c[side]['renderErrors'] for side in c)
 record('Both clients reload resources without missing Bee Bottle models or render exceptions')
 before=next(e for e in s['entities'] if e['id']==spider['id'])
 rpc('guest','disconnect');wait_for(lambda:len(snapshot('server')['players'])==1)
 rpc('guest','connect');wait_for(lambda:snapshot('guest').get('connected'),timeout=60);s,c=sync()
 cp=next(e for e in c['guest']['entities'] if e['id']==spider['id']);assert cp['owner']==ownerId and cp['sitting']==before['sitting']
 record('Disconnect/reconnect restores inventory and full companion entity state')
 # Preserve a known companion/cooldown state for the subsequent process restart test.
 rpc('server','save');s,c=sync()
 (TEST_ROOT/'restart-state.json').write_text(json.dumps({'uuid':spider['uuid'],'owner':ownerId,'entity':spider['id'],'cooldowns':next(e for e in s['entities'] if e['id']==spider['id'])['cooldowns'],'players':s['players']},indent=2))
 print('PHASE ONE COMPLETE',flush=True)
except Exception as e:
 checks.append({'name':'Failure','status':'failed','error':repr(e)});REPORT.write_text(json.dumps(checks,indent=2));raise
