"""Disposable server only: clears a test arena and force-loads chunks; compare immutable JARs without concurrent builds/clients."""
import argparse,json,time
from pathlib import Path
from control import rpc,TEST_ROOT

parser=argparse.ArgumentParser()
parser.add_argument('--label',required=True)
parser.add_argument('--seconds',type=float,default=25)
parser.add_argument('--rounds',type=int,default=2)
parser.add_argument('--count',type=int,default=200)
args=parser.parse_args();results=[]
OUT=Path('/workspace/performance-audit/mice')/(args.label+'.json')
OUT.parent.mkdir(parents=True,exist_ok=True)
def metrics():return rpc('server','mouseProfileMetrics')
def observe(seconds):
    started=time.monotonic();first=metrics()
    while time.monotonic()-started<seconds:time.sleep(.5);metrics()
    last=metrics();elapsed=time.monotonic()-started
    last.update(elapsedSeconds=elapsed,ticksAdvanced=last['tick']-first['tick'],observedTicksPerSecond=(last['tick']-first['tick'])/elapsed,
                griefChecksDuringWindow=last['griefChecks']-first['griefChecks'])
    assert last['livingMice']==args.count,last
    return last
for round_ in range(args.rounds):
    for scenario in ('blocked','barren','foraging','threats'):
        fixture=rpc('server','mouseProfile',scenario=scenario,count=args.count,timeout=90)
        cold=observe(5)
        rpc('server','mouseProfile',scenario='reset')
        steady=observe(args.seconds)
        result=dict(round=round_,scenario=scenario,fixture=fixture,cold=cold,steady=steady);results.append(result)
        OUT.write_text(json.dumps(dict(label=args.label,results=results),indent=2));print(json.dumps(result),flush=True)
rpc('server','mouseProfile',scenario='village',timeout=90)
village=[rpc('server','mouseProfile',scenario='morning') for _ in range(5)]
OUT.write_text(json.dumps(dict(label=args.label,results=results,cropFreeMorning96Cats=village),indent=2));print('MORNING',json.dumps(village),flush=True)
rpc('server','clear');rpc('server','save')
