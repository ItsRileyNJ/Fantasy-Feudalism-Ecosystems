import sys,json,time
from pathlib import Path
sys.path.insert(0,'tools/multiplayer')
from control import rpc,snapshot,wait_for,player
results={}
print('Waiting for both graphical clients',flush=True)
rpc('server','snapshot',timeout=180)
for side in ['owner','guest']:
 rpc(side,'snapshot',timeout=180);rpc(side,'connect')
wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
rpc('server','setup',timeout=90);rpc('server','clear');rpc('server','invulnerable',value=False)
for name,x in [('FFMOwner',4),('FFMGuest',10)]:
 rpc('server','command',command='gamemode survival '+name)
 rpc('server','command',command='advancement revoke '+name+' through fantasy_feudalism_mobs:ecosystems/bottled_buzz')
 rpc('server','teleport',player=name,x=x,y=64,z=4)
results['fresh']={name:rpc('server','advancementState',player=name) for name in ['FFMOwner','FFMGuest']}
assert not any(v['done'] for goals in results['fresh'].values() for v in goals.values())
rpc('server','inventory',player='FFMOwner',slots=[{'slot':0,'item':'minecraft:glass_bottle'}]);rpc('owner','select',slot=0)
bee=rpc('server','spawn',type='minecraft:bee',x=5,y=64,z=4)
wait_for(lambda: any(e['id']==bee['id'] for e in snapshot('owner')['entities']))
rpc('owner','interact',entity=bee['id'])
wait_for(lambda:rpc('server','advancementState',player='FFMOwner')['bottled_buzz']['done'])
assert not rpc('server','advancementState',player='FFMGuest')['bottled_buzz']['done']
cat=rpc('server','spawn',type='minecraft:cat',owner='FFMGuest',x=10,y=64,z=6)
mouse=rpc('server','spawn',type='fantasy_feudalism_mobs:field_mouse',x=11,y=64,z=6)
rpc('server','hit',entity=cat['id'],target=mouse['id'])
results['guest_without_root']=rpc('server','advancementState',player='FFMGuest')
assert results['guest_without_root']['pest_control']['done'] and not results['guest_without_root']['bottled_buzz']['done']
for side in ['owner','guest']:
 rpc(side,'guiScale',scale=2);rpc(side,'advancements');time.sleep(1)
 results[side+'_gui']=snapshot(side)
 results[side+'_screenshot']=rpc(side,'screenshot',file='achievements-0.1.21-'+side+'.png')
print('Bee capture, separate pet ownership and native screens passed',flush=True)
for side in ['owner','guest']:rpc(side,'close')
rpc('server','command',command='difficulty hard');rpc('server','inventory',player='FFMOwner',slots=[])
rpc('server','command',command='effect clear FFMOwner')
p=player(snapshot('server'),'FFMOwner');rpc('server','health',entity=p['id'],health=20)
# Ordinary login immunity has expired while the clients have interacted and opened screens.
time.sleep(4)
endo=rpc('server','spawn',type='minecraft:enderman',x=5,y=64,z=4)
results['fatal']=rpc('server','hit',entity=endo['id'],target=p['id'])
results['owner_after_fatal']=rpc('server','advancementState',player='FFMOwner')
assert results['owner_after_fatal']['everyones_got_a_plan_until']['done']
wait_for(lambda:snapshot('owner')['screen']=='DeathScreen')
rpc('owner','respawn');wait_for(lambda:player(snapshot('server'),'FFMOwner')['health']>0)
rpc('server','save')
for side in ['owner','guest']:rpc(side,'disconnect')
wait_for(lambda:len(snapshot('server')['players'])==0)
for side in ['owner','guest']:rpc(side,'connect')
wait_for(lambda:len(snapshot('server')['players'])==2)
results['reconnected']={name:rpc('server','advancementState',player=name) for name in ['FFMOwner','FFMGuest']}
assert results['reconnected']['FFMOwner']['bottled_buzz']['done'] and results['reconnected']['FFMOwner']['everyones_got_a_plan_until']['done']
assert results['reconnected']['FFMGuest']['pest_control']['done'] and not results['reconnected']['FFMGuest']['bottled_buzz']['done']
Path('/workspace/multiplayer/achievements-0.1.21-results.json').write_text(json.dumps(results,indent=2))
print('PASS: actual multiplayer actions, death/respawn, advancement screens and reconnect persistence',flush=True)
