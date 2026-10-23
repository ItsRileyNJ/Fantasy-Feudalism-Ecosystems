"""Live combat and bounded combined-mod load; run after all Gradle/reference tests finish."""
from control import *
import argparse

parser=argparse.ArgumentParser()
parser.add_argument('--combat-timeout',type=float,default=25,help='Observation window; long-range vanilla spread can miss multiple shots')
args=parser.parse_args()

REPORT=TEST_ROOT/'mercenaries-results.json'
checks=json.loads(REPORT.read_text())
def record(name,detail=None):
 checks.append({'name':name,'status':'passed','detail':detail});REPORT.write_text(json.dumps(checks,indent=2));print('PASS',name,json.dumps(detail) if detail else '',flush=True)
def byid(s,id):return next((e for e in s['entities'] if e['id']==id),None)
try:
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False)
 rpc('server','clear');rpc('server','invulnerable',value=True)
 for name,z in [('FFMOwner',0),('FFMGuest',2)]:rpc('server','teleport',player=name,x=-3,y=64,z=z,yaw=-90,pitch=0)
 for weapon,distance in [('bow',10),('crossbow',27)]:
  rpc('server','clear')
  guard=rpc('server','spawn',type='villagerscontract:defender',x=3+distance,y=64,z=1,owner='FFMOwner')
  skeleton=rpc('server','spawn',type='minecraft:skeleton',x=3,y=64,z=1,weapon='minecraft:'+weapon,seed=42)
  # Start the combat deadline only after both clients track the combatants.
  wait_for(lambda:all(byid(snapshot(side),guard['id']) and byid(snapshot(side),skeleton['id']) for side in ['owner','guest']))
  rpc('server','target',entity=skeleton['id'],target=guard['id'])
  started=time.monotonic()
  def hit():
   s=snapshot('server');g=byid(s,guard['id'])
   if not g or not g['health']<g['maxHealth']:return False
   for side in ['owner','guest']:
    c=snapshot(side);cg=byid(c,guard['id'])
    if not cg or not cg['health']<cg['maxHealth'] or c['renderErrors']:return False
   return g
  g=wait_for(hit,timeout=args.combat_timeout)
  record('Live '+weapon+' skeleton AI hits a mercenary at '+str(distance)+' blocks and both graphical clients receive its health change',{'health':g['health'],'maxHealth':g['maxHealth'],'secondsToObservedHit':time.monotonic()-started,'observationLimitSeconds':args.combat_timeout,'seed':42})
 rpc('server','clear')
 for name,z in [('FFMOwner',0),('FFMGuest',2)]:rpc('server','teleport',player=name,x=32,y=64,z=z,yaw=90,pitch=5)
 rpc('server','stress',count=180)
 for i in range(20):rpc('server','spawn',type='villagerscontract:defender',x=4+i%10*1.5,y=64,z=5+i//10*2,owner='FFMOwner',ai=True)
 time.sleep(2);rpc('server','resetTimings')
 first=snapshot('server');before={side:snapshot(side) for side in ['owner','guest']};started=time.monotonic()
 while time.monotonic()-started<35:
  for side in ['owner','guest']:
   c=snapshot(side);assert c['connected'] and not c['renderErrors'] and not c['beeBottleModelMissing']
  snapshot('server');time.sleep(.5)
 last=snapshot('server');elapsed=time.monotonic()-started
 clientFrames={}
 for side in ['owner','guest']:
  c=snapshot(side)
  assert c['frames']>before[side]['frames']+30 and c['guardFrames']>before[side]['guardFrames']+30
  clientFrames[side]={'framesAdvanced':c['frames']-before[side]['frames'],'observedFramesPerSecond':(c['frames']-before[side]['frames'])/elapsed}
  rpc(side,'screenshot',file='mercenary-mixed-load-'+side+'.png')
 detail={k:last[k] for k in ['tickSamples','meanTickMs','p95TickMs','maxTickMs']}
 detail.update(initialMobs=200,guards=20,elapsedSeconds=elapsed,ticksAdvanced=last['tick']-first['tick'],observedTicksPerSecond=(last['tick']-first['tick'])/elapsed,remainingEntities=len(last['entities']))
 detail['clients']=clientFrames
 record('180 active modified mobs plus 20 armed mercenaries remain responsive with two graphical clients, advancing frames and no render failures',detail)
 rpc('server','clear');rpc('server','save')
 print('COMBINED LIVE COMBAT AND LOAD COMPLETE',flush=True)
except Exception as e:
 checks.append({'name':'Failure','status':'failed','error':repr(e)});REPORT.write_text(json.dumps(checks,indent=2));raise
