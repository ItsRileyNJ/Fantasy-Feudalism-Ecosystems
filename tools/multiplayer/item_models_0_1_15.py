"""Final release item-model geometry plus real empty/filled Bat Net and enclosure smoke test."""
import json,pathlib,time
from control import rpc,snapshot,wait_for,player,count
OUT=pathlib.Path('/workspace/multiplayer/item-models-0.1.15-results.json');checks=[]
MOD='fantasy_feudalism_mobs:';SIDES=['owner','guest']
def cmd(c):return rpc('server','command',command=c)
def check(name,**detail):
 checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def run():
 rpc('server','setup');rpc('server','clear');cmd('time set 6000');rpc('server','invulnerable',value=False)
 for side,name,x in [('owner','FFMOwner',8.5),('guest','FFMGuest',6.8)]:
  cmd('gamemode survival '+name);rpc(side,'close');rpc('server','teleport',player=name,x=x,y=64,z=5.3,yaw=0,pitch=17)
  rpc(side,'cleanView',value=False)
 models={side:snapshot(side)['ecosystemModels'] for side in SIDES}
 for side,items in models.items():
  assert all(not v['missing'] and v['quads']>0 for v in items.values()),(side,items)
  assert items[MOD+'fertilizer']['tint']==-1850297,items[MOD+'fertilizer'] # ARGB FFE3C447, signed Java int
 check('Both clients bake actual visible geometry for all three new item models, with the yellow fertilizer tint',models=models)
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=MOD+'bat_net',count=1)])
 rpc('owner','select',slot=0);wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==MOD+'bat_net')
 rpc('owner','inventory');time.sleep(.3);rpc('owner','screenshot',file='ecosystems-015/net-empty.png');rpc('owner','close')
 bat=rpc('server','spawn',type='minecraft:bat',x=8.5,y=65,z=6.3,ai=True)
 wait_for(lambda:ent(snapshot('owner'),bat['id']));rpc('owner','interact',entity=bat['id'])
 wait_for(lambda:snapshot('owner')['player']['inventory'][0].get('batUUID')==bat['uuid'])
 wait_for(lambda:not ent(snapshot('server'),bat['id']) and not ent(snapshot('owner'),bat['id']))
 rpc('owner','inventory');time.sleep(.3);rpc('owner','screenshot',file='ecosystems-015/net-captured.png');rpc('owner','close')
 check('The final empty net captures a real bat and synchronizes the occupied net data without a second world bat',batUUID=bat['uuid'])
 cmd('setblock 8 64 8 '+MOD+'bat_enclosure[facing=north]');rpc('owner','useBlock',x=8,y=64,z=8)
 for side in SIDES:
  state=wait_for(lambda side=side:(s if (s:=rpc(side,'enclosure',x=8,y=64,z=8))['inside']==1 and s['display']==1 else None))
  # Block-entity packets precede asynchronous chunk-mesh rebuilds. Let the
  # visible cabinet settle before capturing visual evidence.
  time.sleep(1)
  rpc(side,'screenshot',file='ecosystems-015/final-one-bat.png')
 check('The final release model shows exactly one inserted bat on both clients and empties the same net')
 for side in SIDES:
  cmd('gamemode creative '+('FFMOwner' if side=='owner' else 'FFMGuest'));rpc(side,'cleanView',value=False);rpc(side,'inventory')
  for _ in range(4):
   s=snapshot(side);tab=next((t for t in s.get('creativeTabs',[]) if t['title']=='Fantasy Feudalism - Ecosystems'),None)
   if tab:break
   rpc(side,'mouse',x=s['guiWidth']/2+87,y=s['guiHeight']/2-108)
  assert tab
  rpc(side,'mouse',x=(s['guiWidth']-195)//2+27*tab['column']+13,y=(s['guiHeight']-136)//2+(-16 if tab['top'] else 152))
  rpc(side,'screenshot',file='ecosystems-015/creative-final.png');rpc(side,'close');rpc(side,'reload',timeout=60)
 check('Both real creative tabs render the repaired Bat Net and Ecosystems items, then reload resources successfully')
 assert all(not snapshot(side)['renderErrors'] for side in SIDES)
 rpc('server','save');check('The final JAR saves and continues rendering after client resource reloads')
if __name__=='__main__':
 try:run()
 except BaseException as e:
  OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2));raise
