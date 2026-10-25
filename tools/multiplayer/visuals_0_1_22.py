import sys,time,json
from pathlib import Path
sys.path.insert(0,'tools/multiplayer');from control import rpc,snapshot,wait_for
out={};MOD='fantasy_feudalism_mobs:'
def cmd(s):return rpc('server','command',command=s)
def enclosure():return rpc('server','enclosure',x=8,y=65,z=8)
rpc('server','snapshot',timeout=120);rpc('owner','snapshot',timeout=180);rpc('owner','connect');wait_for(lambda:len(snapshot('server')['players'])==1)
rpc('server','setup',timeout=90);rpc('server','clear');cmd('time set 6000');cmd('gamemode survival FFMOwner');rpc('server','invulnerable',value=False)
rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=6.0,yaw=0,pitch=0)
rpc('owner','cleanView',value=True);rpc('owner','guiScale',scale=2)
cmd('setblock 8 64 8 minecraft:stone');cmd('setblock 8 65 8 '+MOD+'bat_enclosure[facing=north]')
for n in range(1,4):
 rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':MOD+'bat_net'}]);rpc('owner','select',slot=0)
 bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65.4,z=6.6)
 wait_for(lambda:any(e['id']==bat['id'] for e in snapshot('owner')['entities']))
 rpc('owner','interact',entity=bat['id']);wait_for(lambda:snapshot('owner')['player']['inventory'][0].get('batUUID')==bat['uuid'])
 rpc('owner','useBlock',x=8,y=65,z=8);wait_for(lambda:enclosure()['inside']==n)
 rpc('owner','resetEnclosurePoseProbe');time.sleep(2)
 state=snapshot('owner');ids=set(enclosure()['ids']);poses={k:v for k,v in state['enclosureBatPoses'].items() if k in ids}
 assert len(poses)==n and all(v['maxPoseDelta']==0 and v['minPartial']==v['maxPartial']==0 and v['frames']>10 for v in poses.values()),poses
 assert not state['renderErrors']
 out['count_'+str(n)]={'home':enclosure(),'poses':poses,'screenshot':rpc('owner','screenshot',file='visuals-022/bats-'+str(n)+'.png')}
 print('PASS: native capture/insertion and stable ceiling display, count',n,flush=True)
for facing,x,z,yaw in [('north',8.5,6,0),('east',11,8.5,90),('south',8.5,11,180),('west',6,8.5,-90)]:
 cmd('setblock 8 65 8 '+MOD+'bat_enclosure[facing='+facing+']')
 rpc('server','teleport',player='FFMOwner',x=x,y=64,z=z,yaw=yaw,pitch=0);time.sleep(1)
 out['facing_'+facing]={'home':enclosure(),'screenshot':rpc('owner','screenshot',file='visuals-022/enclosure-'+facing+'.png')}
 assert enclosure()['inside']==3
rpc('owner','cleanView',value=False)
rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':MOD+'bat_net'},{'slot':1,'item':MOD+'raw_horse'},{'slot':2,'item':MOD+'cooked_horse'},{'slot':3,'item':MOD+'bat_enclosure'}])
rpc('owner','inventory');time.sleep(1)
out['inventory_screenshot']=rpc('owner','screenshot',file='visuals-022/items.png');out['client']=snapshot('owner')
assert not out['client']['renderErrors']
assert all(not m['missing'] and m['quads']>0 for m in out['client']['ecosystemModels'].values())
rpc('owner','close');out['reload']=rpc('owner','reload',timeout=90)
assert not snapshot('owner')['renderErrors']
Path('/workspace/multiplayer/visuals-0.1.22-results.json').write_text(json.dumps(out,indent=2))
print('PASS: all four enclosure directions, flat net and meat item sprites, native resource reload',flush=True)
