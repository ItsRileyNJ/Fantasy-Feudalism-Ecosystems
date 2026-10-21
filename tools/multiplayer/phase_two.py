from control import *
REPORT=(TEST_ROOT/'results.json')
checks=json.loads(REPORT.read_text())
def record(name,detail=None):
 checks.append({'name':name,'status':'passed','detail':detail});REPORT.write_text(json.dumps(checks,indent=2));print('PASS',name,json.dumps(detail) if detail else '',flush=True)
def inventories_match():
 s=snapshot('server'); cs={side:snapshot(side) for side in ['owner','guest']}
 if len(s['players'])!=2 or not all(c.get('connected') for c in cs.values()):return False
 for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
  p=player(s,name)
  if cs[side]['player']['inventory']!=p['inventory'] or cs[side]['player']['cursor']!=p['cursor']:return False
 return s,cs
try:
 rpc('server','snapshot',timeout=90)
 for side in ['owner','guest']:rpc(side,'connect')
 s,cs=wait_for(inventories_match,timeout=90)
 before=json.loads((TEST_ROOT/'restart-state.json').read_text())
 pet=wait_for(lambda:next((e for e in snapshot('server')['entities'] if e['uuid']==before['uuid']),False))
 assert pet['owner']==before['owner'] and pet['sitting'] and pet['cooldowns']==before['cooldowns'],pet
 for side in ['owner','guest']:
  cp=wait_for(lambda:next((e for e in snapshot(side)['entities'] if e['uuid']==before['uuid']),False))
  assert cp['owner']==before['owner'] and cp['sitting'] and cp['maxHealth']==30,cp
 for old in before['players']:
  current=player(s,old['name']);assert current['inventory']==old['inventory'] and current['cursor']==old['cursor']
 record('Normal dedicated-server stop/restart preserves companion UUID, ownership, orders, both cooldowns and player inventories')
 rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':'minecraft:glass_bottle','count':16}]);rpc('owner','select',slot=0);wait_for(inventories_match)
 rpc('owner','interact',entity=pet['id'],count=100)
 s,cs=wait_for(inventories_match);assert count(player(s,'FFMOwner'),'minecraft:glass_bottle')==16
 assert count(player(s,'FFMOwner'),'minecraft:potion')==0
 record('Saved harvest cooldown cannot be bypassed by restarting the server')
 rpc('server','clear');rpc('server','invulnerable',value=True)
 # Daylight makes screenshots easier to inspect; these fixture skeletons are invulnerable.
 golem=rpc('server','spawn',type='minecraft:iron_golem',x=30,y=64,z=1,invulnerable=True)
 skeletons=[]
 for kind,z in [('skeleton',-1),('stray',1),('bogged',3)]:
  mob=rpc('server','spawn',type='minecraft:'+kind,x=3,y=64,z=z,weapon='minecraft:crossbow',invulnerable=True)
  skeletons.append(mob)
  rpc('server','target',entity=mob['id'],target=golem['id'])
 for side in ['owner','guest']:rpc(side,'look',yaw=-90,pitch=6)
 baseline={side:snapshot(side) for side in ['owner','guest']}
 start=time.monotonic()
 while time.monotonic()-start<12:
  time.sleep(.5)
 final={side:snapshot(side) for side in ['owner','guest']}
 for side,c in final.items():
  assert c['frames']>baseline[side]['frames']+30 and c['crossbowFrames']>baseline[side]['crossbowFrames']+30,c
  assert not c['renderErrors'] and not c['beeBottleModelMissing']
  rpc(side,'screenshot',file='crossbows.png')
 record('Both graphical clients render active skeleton, stray and bogged crossbows with finite model poses', {side:{k:final[side][k]-baseline[side][k] for k in ['frames','crossbowFrames']} for side in final})
 rpc('server','clear');rpc('server','stress',count=240)
 first=snapshot('server');firstClients={side:snapshot(side) for side in ['owner','guest']}
 started=time.monotonic()
 while time.monotonic()-started<35:
  for side in ['owner','guest']:
   c=snapshot(side);assert c['connected'] and not c['renderErrors']
  snapshot('server');time.sleep(.5)
 last=snapshot('server');elapsed=time.monotonic()-started
 lastClients={side:snapshot(side) for side in ['owner','guest']}
 for side in lastClients:assert lastClients[side]['frames']>firstClients[side]['frames']+30
 detail={k:last[k] for k in ['tickSamples','meanTickMs','p95TickMs','maxTickMs']}
 detail.update({'spawnedMobs':240,'remainingEntities':len(last['entities']),'elapsedSeconds':elapsed,'ticksAdvanced':last['tick']-first['tick'],'observedTicksPerSecond':(last['tick']-first['tick'])/elapsed})
 record('240 active mixed mobs with both graphical clients remain responsive without disconnects or render failures',detail)
 rpc('server','clear');rpc('server','save')
 print('PHASE TWO COMPLETE',flush=True)
except Exception as e:
 checks.append({'name':'Failure','status':'failed','error':repr(e)});REPORT.write_text(json.dumps(checks,indent=2));raise
