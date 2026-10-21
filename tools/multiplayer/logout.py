"""Verify the helper follows Minecraft's menu logout path without waiting for a server timeout."""
from control import *

rpc('server','snapshot',timeout=90)
for side in ['owner','guest']:
 rpc(side,'snapshot',timeout=90)
 rpc(side,'connect')
wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
for side in ['owner','guest']:wait_for(lambda:snapshot(side).get('connected'),timeout=60)
durations={}
for side,remaining in [('guest',1),('owner',0)]:
 start=time.monotonic()
 rpc(side,'disconnect')
 wait_for(lambda:len(snapshot('server')['players'])==remaining,timeout=5)
 durations[side]=time.monotonic()-start
 assert durations[side]<5
result={'name':'Both clients close their connections through the vanilla logout sequence without timeout','status':'passed','detail':{'logoutSeconds':durations}}
p=TEST_ROOT/'results.json';checks=json.loads(p.read_text());checks.append(result);p.write_text(json.dumps(checks,indent=2))
print(json.dumps(result,indent=2),flush=True)
