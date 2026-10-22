"""Bounded dedicated-server JFR comparison; deliberately dense colonies/guardians, no graphical clients."""
import json,time,pathlib,subprocess,sys
from control import rpc,snapshot
phase=sys.argv[1];out=pathlib.Path('/workspace/audit-016');out.mkdir(exist_ok=True)
java='/workspace/toolchains/jdk-21.0.12.1+1/bin/'
pid=next(p.name for p in pathlib.Path('/proc').iterdir() if p.name.isdigit() and (p/'comm').exists() and (p/'comm').read_text().strip()=='java' and b'-Dffm.test.control=/workspace/multiplayer/control/server' in (p/'cmdline').read_bytes().split(b'\0'))
rpc('server','command',command='forceload add -32 -32 64 64')
rpc('server','command',command='gamerule doMobSpawning false')
rpc('server','command',command='gamerule doDaylightCycle false')
rpc('server','command',command='difficulty hard')
time.sleep(3)
results=[]
for kind,count in [('bats',40),('phantoms',120)]:
 print('Preparing',phase,kind,flush=True)
 # Let saved forced chunks finish entity loading before the real cleanup/spawn operation.
 rpc('server','auditFixture',timeout=90,kind=kind,count=0);time.sleep(3)
 rpc('server','auditFixture',timeout=90,kind=kind,count=count)
 time.sleep(10);rpc('server','resetTimings');first=snapshot('server');start=time.monotonic()
 profile=out/(phase+'-'+kind+'.jfr')
 subprocess.run([java+'jcmd',pid,'JFR.start','name=Audit','settings=profile','duration=30s','filename='+str(profile)],check=True)
 while time.monotonic()-start<32:time.sleep(.25)
 last=snapshot('server');elapsed=time.monotonic()-start
 expected=120 if kind=='bats' else 520
 assert len(last['entities'])==expected,('Contaminated or unloaded fixture',kind,len(last['entities']),expected)
 record=dict(kind=kind,phase=phase,elapsedSeconds=elapsed,ticks=last['tick']-first['tick'],tps=(last['tick']-first['tick'])/elapsed,
             timings={k:last[k] for k in ['tickSamples','meanTickMs','p95TickMs','maxTickMs']},entities=len(last['entities']))
 results.append(record);(out/(phase+'-metrics.json')).write_text(json.dumps(results,indent=2));print(json.dumps(record),flush=True)
print('PROFILE COMPLETE',phase,flush=True)
