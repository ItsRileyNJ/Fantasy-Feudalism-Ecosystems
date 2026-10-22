"""Real two-client checks for patched vanilla mechanics; no gameplay state is assigned by diagnostic RPCs."""
import json,pathlib,time
from control import rpc,snapshot,wait_for,player,count
OUT=pathlib.Path('/workspace/multiplayer/vanilla-audit-0.1.16-results.json');checks=[];MOD='fantasy_feudalism_mobs:'
def cmd(c):return rpc('server','command',command=c)
def ent(s,i):return next((e for e in s['entities'] if e['id']==i),None)
def check(name,**detail):
 checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def inventory(item,n=1):
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=item,count=n)]);rpc('owner','select',slot=0)
 wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==item)
def run():
 rpc('server','setup');rpc('server','clear');rpc('server','invulnerable',value=False)
 cmd('time set 6000');cmd('gamerule randomTickSpeed 0');cmd('gamemode survival FFMOwner');cmd('gamemode creative FFMGuest')
 cmd('effect give FFMOwner minecraft:instant_health 1 5 true')
 rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=5.5,yaw=0,pitch=10)
 rpc('server','teleport',player='FFMGuest',x=11,y=64,z=5.5,yaw=25,pitch=10)
 for side in ['owner','guest']:rpc(side,'close');rpc(side,'shift',value=False)
 bee=rpc('server','spawn',type='minecraft:bee',x=8.5,y=65,z=7)
 wait_for(lambda:ent(snapshot('owner'),bee['id']));inventory('minecraft:lead');rpc('owner','interact',entity=bee['id'])
 wait_for(lambda:count(player(snapshot('server'),'FFMOwner'),'minecraft:lead')==0)
 inventory('minecraft:glass_bottle');rpc('owner','interact',entity=bee['id'])
 wait_for(lambda:count(player(snapshot('server'),'FFMOwner'),MOD+'bee_bottle')==1)
 s=snapshot('server');leads=count(player(s,'FFMOwner'),'minecraft:lead')+sum(e['item']['count'] for e in s['entities'] if e['type']=='minecraft:item' and e['item']['item']=='minecraft:lead')
 assert leads==1 and not ent(s,bee['id'])
 check('A real lead attaches to a bee; glass-bottle capture returns exactly one lead and one Bee Bottle',leads=leads)
 rpc('server','clear');cmd('time set 18000');cmd('attribute FFMOwner minecraft:player.entity_interaction_range base set 6')
 rpc('server','teleport',player='FFMOwner',x=8.5,y=64,z=5.5,yaw=0,pitch=0)
 wither=rpc('server','spawn',type='minecraft:wither_skeleton',x=8.5,y=64,z=11,weapon='minecraft:stone_sword',shield=True)
 # A native slowness effect holds the blocking approach at 5.5 blocks. Native reach=6
 # permits an actual networked axe hit before its ordinary five-block charge transition.
 cmd('tp '+wither['uuid']+' 8.5 64 11 180 0')
 cmd('effect give '+wither['uuid']+' minecraft:slowness 60 255 true')
 owner=player(snapshot('server'),'FFMOwner');rpc('server','target',entity=wither['id'],target=owner['id'])
 inventory('minecraft:iron_axe');wait_for(lambda:(w:=ent(snapshot('server'),wither['id'])) and w['blocking']);time.sleep(.6)
 for side in ['owner','guest']:wait_for(lambda side=side:(w:=ent(snapshot(side),wither['id'])) and w['blocking'])
 rpc('owner','attack',entity=wither['id'])
 disabled=wait_for(lambda:(w if (w:=ent(snapshot('server'),wither['id'])) and w['shieldDisabledUntil']>snapshot('server')['gameTime'] and not w['usingItem'] else None))
 for side in ['owner','guest']:wait_for(lambda side=side:(w:=ent(snapshot(side),wither['id'])) and not w['usingItem'])
 check('A real axe attack disables the raised shield and both clients see blocking end',wither=disabled)
 time.sleep(2);assert not ent(snapshot('server'),wither['id'])['blocking']
 wait_for(lambda:(w:=ent(snapshot('server'),wither['id'])) and w['blocking'],timeout=8)
 check('Shield AI honors the five-second axe cooldown and can raise the shield again afterward')
 cmd('attribute FFMOwner minecraft:player.entity_interaction_range base set 3');rpc('server','clear');cmd('time set 6000')
 cmd('gamerule mobGriefing false');cmd('fill 7 63 8 13 63 14 minecraft:stone_bricks');cmd('summon minecraft:creeper 10.5 64 11.5 {NoAI:1b,Fuse:1s,ignited:1b}')
 time.sleep(.8)
 observed={}
 for side in ['server','owner','guest']:
  observed[side]=[rpc(side,'block',x=x,y=63,z=z)['block'] for x,z in [(9,10),(10,11),(11,12)]]
  assert observed[side]==['minecraft:stone_bricks']*3
 check('Native creeper explosions respect mobGriefing=false on the server and both graphical clients',blocks=observed)
 cmd('gamerule mobGriefing true');cmd('setblock 8 63 7 minecraft:farmland');cmd('setblock 8 64 7 minecraft:wheat[age=0]')
 inventory('minecraft:bone_meal',3);rpc('owner','useBlock',x=8,y=64,z=7)
 wait_for(lambda:count(player(snapshot('server'),'FFMOwner'),'minecraft:bone_meal')==2)
 state=rpc('server','block',x=8,y=64,z=7);assert int(state['properties']['age'])>0
 check('Ordinary bone meal retains native crop growth and consumes one item',crop=state)
 inventory(MOD+'fertilizer',2);rpc('owner','useBlock',x=8,y=64,z=7)
 for side in ['server','owner','guest']:wait_for(lambda side=side:rpc(side,'block',x=8,y=64,z=7)['properties'].get('age')=='7')
 assert count(player(snapshot('server'),'FFMOwner'),MOD+'fertilizer')==1
 check('Fertilizer reaches maximum crop age without disrupting the ordinary bone-meal path')
 cmd('effect give FFMOwner minecraft:slowness 10 0 false');inventory('minecraft:milk_bucket');rpc('owner','look',yaw=180,pitch=-30);rpc('owner','use',value=True)
 try:wait_for(lambda:count(player(snapshot('server'),'FFMOwner'),'minecraft:bucket')==1,timeout=8)
 finally:rpc('owner','use',value=False)
 assert 'minecraft:slowness' not in player(snapshot('server'),'FFMOwner')['effects']
 check('Drinking milk through the real client cures effects and returns the vanilla empty bucket')
 # Actual furnace processing and hopper extraction of the mod's meat; no output is seeded.
 cmd('setblock 9 64 7 minecraft:air');cmd('setblock 9 64 7 minecraft:furnace')
 cmd('setblock 9 63 7 minecraft:hopper[facing=east]')
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=MOD+'raw_horse',count=1),dict(slot=1,item='minecraft:coal',count=1)])
 rpc('owner','select',slot=0);wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==MOD+'raw_horse')
 rpc('owner','useBlock',x=9,y=64,z=7);wait_for(lambda:snapshot('owner')['screen']=='FurnaceScreen')
 rpc('owner','click',slot=0,type='SWAP',button=0);rpc('owner','click',slot=1,type='SWAP',button=1);rpc('owner','close')
 wait_for(lambda:any(s['item']==MOD+'cooked_horse' and s['count']==1 for s in rpc('server','block',x=9,y=63,z=7)['slots']),timeout=18)
 check('A vanilla furnace cooks Raw Horse and a vanilla hopper extracts exactly one Cooked Horse')
 for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
  rpc('server','teleport',player=name,x=9,y=64,z=5,yaw=0,pitch=15);rpc(side,'useBlock',x=9,y=64,z=7)
  wait_for(lambda side=side:snapshot(side)['screen']=='FurnaceScreen')
  rpc(side,'screenshot',file='audit-016/furnace-'+side+'.png');rpc(side,'close')
 check('Both players can open and close the native furnace GUI after mod item automation')
 assert all(not snapshot(side)['renderErrors'] for side in ['owner','guest'])
 rpc('server','save');check('The vanilla audit world saves and both clients report no render errors')
if __name__=='__main__':
 try:run()
 except BaseException as e:
  OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2))
  cmd('gamemode creative FFMOwner');cmd('attribute FFMOwner minecraft:player.entity_interaction_range base set 3');rpc('server','clear');raise
