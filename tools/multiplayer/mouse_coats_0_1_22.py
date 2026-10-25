import sys,time,math,json
from pathlib import Path
sys.path.insert(0,'tools/multiplayer')
from control import rpc,snapshot,wait_for,player
out={}
def cmd(s):return rpc('server','command',command=s)
def ent(uid,side='server'):return next((e for e in snapshot(side)['entities'] if e['uuid']==uid),None)
rpc('server','snapshot',timeout=120);rpc('owner','snapshot',timeout=180);rpc('owner','connect')
wait_for(lambda:len(snapshot('server')['players'])==1,timeout=90)
rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('gamemode creative FFMOwner')
rpc('server','inventory',player='FFMOwner',slots=[]);rpc('owner','close');rpc('owner','cleanView',value=True)
white=rpc('server','spawn',type='fantasy_feudalism_mobs:field_mouse',x=7.7,y=64,z=8)
amber=rpc('server','spawn',type='fantasy_feudalism_mobs:field_mouse',x=8.5,y=64,z=8)
cmd('data merge entity '+amber['uuid']+' {Variant:1}')
wait_for(lambda:ent(white['uuid'],'owner') is not None and ent(amber['uuid'],'owner') is not None)
wait_for(lambda:ent(amber['uuid'],'owner')['variant']==1)
assert ent(white['uuid'],'owner')['variant']==0
rpc('server','teleport',player='FFMOwner',x=8.1,y=64,z=6.7,yaw=0,pitch=47)
wait_for(lambda:all(snapshot('owner')['mouseRenderFrames'].get(str(e['id']),0)>15 for e in [white,amber]))
time.sleep(2)
out['white_server']=ent(white['uuid']);out['amber_server']=ent(amber['uuid']);out['white_client']=ent(white['uuid'],'owner');out['amber_client']=ent(amber['uuid'],'owner')
out['screenshot']=rpc('owner','screenshot',file='mouse-coats-0.1.22.png')
assert not snapshot('owner')['renderErrors']
rpc('server','save');rpc('owner','disconnect');wait_for(lambda:len(snapshot('server')['players'])==0);rpc('owner','connect');wait_for(lambda:len(snapshot('server')['players'])==1)
wait_for(lambda:ent(amber['uuid'],'owner') is not None and ent(amber['uuid'],'owner')['variant']==1)
out['after_reconnect']=ent(amber['uuid'],'owner')
Path('/workspace/multiplayer/mouse-coats-0.1.22-results.json').write_text(json.dumps(out,indent=2))
print('PASS: server/client white and amber coats agree, both render, and amber survives reconnect',flush=True)
