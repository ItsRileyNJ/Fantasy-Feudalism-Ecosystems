"""Repeat the previously failing hidden-inventory race on real connected clients."""
from control import *
from concurrent.futures import ThreadPoolExecutor
pool=ThreadPoolExecutor(max_workers=2)
def matching():
 s=snapshot('server');cs={side:snapshot(side) for side in ['owner','guest']}
 for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
  p=player(s,name);c=cs[side]['player']
  if c['inventory']!=p['inventory'] or c['cursor']!=p['cursor'] or c['menu']['type']!='GuardMenu' or c['menu']['slots']!=p['menu']['slots']:return False
 return s,cs
s=snapshot('server');guard=next(e for e in s['entities'] if e.get('guard',{}).get('isGuardCaptain'));gid=guard['id']
for side,name,z in [('owner','FFMOwner',0),('guest','FFMGuest',2)]:
 rpc(side,'close');rpc('server','teleport',player=name,x=3,y=64,z=z,yaw=-90,pitch=0)
 rpc(side,'shift',value=True);rpc(side,'interact',entity=gid)
wait_for(matching)
for trial in range(12):
 for name in ['FFMOwner','FFMGuest']:rpc('server','inventory',player=name,fill=True,slots=[{'slot':20,'item':'minecraft:air','count':0}])
 rpc('server','mercenary',op='supplies',entity=gid,slots=[{'slot':2,'item':'minecraft:string','count':17}]);wait_for(matching)
 for repeat in range(4):
  jobs=[pool.submit(rpc,side,'click',slot=11,type='QUICK_MOVE') for side in ['owner','guest']]
  for job in jobs:job.result()
 s,cs=wait_for(matching)
 assert sum(count(p,'minecraft:string') for p in s['players'])==17
 assert next(e for e in s['entities'] if e['id']==gid)['guard']['supplies'][2]['count']==0
 print('PASS shared inventory race',trial+1,flush=True)
for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False)
p=TEST_ROOT/'mercenaries-results.json';checks=json.loads(p.read_text());checks.append({'name':'Twelve repeated real-client inventory races conserve 17 items each and eliminate the previously reproduced losing-client ghost stack','status':'passed','detail':{'races':12,'concurrentQuickMoveRequests':96}});p.write_text(json.dumps(checks,indent=2))
