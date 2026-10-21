"""Bounded live-client load check; run in the disposable fixture documented in VALIDATION.md."""
from control import *

rpc('server','clear')
rpc('server','invulnerable',value=True)
assert len(snapshot('server')['players'])==2
rpc('server','stress',count=240)
first=snapshot('server')
before={side:snapshot(side) for side in ['owner','guest']}
started=time.monotonic()
while time.monotonic()-started<35:
 for side in ['owner','guest']:
  c=snapshot(side)
  assert c['connected'] and not c['renderErrors'] and not c['beeBottleModelMissing']
 snapshot('server')
 time.sleep(.5)
last=snapshot('server')
elapsed=time.monotonic()-started
for side in ['owner','guest']:
 c=snapshot(side)
 assert c['frames']>before[side]['frames']+30
 rpc(side,'screenshot',file='load.png')
detail={k:last[k] for k in ['tickSamples','meanTickMs','p95TickMs','maxTickMs']}
detail.update(spawnedMobs=240,remainingEntities=len(last['entities']),elapsedSeconds=elapsed,ticksAdvanced=last['tick']-first['tick'],observedTicksPerSecond=(last['tick']-first['tick'])/elapsed)
result={'name':'240-mob load check without concurrent Gradle/GameTest processes','status':'passed','detail':detail}
p=TEST_ROOT/'results.json'
checks=json.loads(p.read_text());checks.append(result);p.write_text(json.dumps(checks,indent=2))
print(json.dumps(result,indent=2),flush=True)
rpc('server','clear')
rpc('server','save')
