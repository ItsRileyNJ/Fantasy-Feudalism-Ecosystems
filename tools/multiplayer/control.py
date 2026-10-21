import json,time,pathlib,os
TEST_ROOT=pathlib.Path(os.environ.get('FFM_TEST_ROOT','/workspace/multiplayer'))
ROOT=TEST_ROOT/'control'
def rpc(side, action, timeout=30, **params):
 root=ROOT/side;root.mkdir(parents=True,exist_ok=True);req={'id':time.monotonic_ns(),'action':action,**params}
 tmp=root/'command.tmp';tmp.write_text(json.dumps(req));os.replace(tmp,root/'command.json')
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  try:
   r=json.loads((root/'response.json').read_text())
   if r['id']==req['id']:
    if r.get('error'):raise RuntimeError(side+': '+r['error'])
    return r['result']
  except(FileNotFoundError,json.JSONDecodeError):pass
  time.sleep(.05)
 raise TimeoutError(side+' action '+action)
def snapshot(side):return rpc(side,'snapshot')
def wait_for(fn, timeout=30):
 end=time.monotonic()+timeout;last=None
 while time.monotonic()<end:
  try:
   last=fn()
   if last:return last
  except(TimeoutError,RuntimeError) as e:last=str(e)
  time.sleep(.1)
 raise AssertionError('Condition timed out: '+repr(last))
def player(state,name):return next(p for p in state['players'] if p['name']==name)
def count(p,item):return sum(s['count'] for s in p['inventory'] if s['item']==item)
if __name__=='__main__':
 print(json.dumps(rpc('server','setup',timeout=90)),flush=True)
 for side in ['owner','guest']:
  print(side,'boot',rpc(side,'snapshot',timeout=90),flush=True)
  print(side,'connect',rpc(side,'connect'),flush=True)
 wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
 print('Both clients connected',json.dumps(snapshot('server')['players']),flush=True)
