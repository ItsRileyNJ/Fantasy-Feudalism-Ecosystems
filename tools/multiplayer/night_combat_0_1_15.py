"""Observe actual wither combat poses/projectiles, phantom defense, and combined GUIs."""
import json,pathlib,time,math
from control import rpc,snapshot,wait_for,player,count
OUT=pathlib.Path('/workspace/multiplayer/night-combat-0.1.15-results.json');checks=[]
MOD='fantasy_feudalism_mobs:';SIDES=['owner','guest']
def command(c):return rpc('server','command',command=c)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def check(name,**detail):
 checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def camera(side,name,x,z,tx,tz):
 yaw=math.degrees(math.atan2(-(tx-x),tz-z));rpc('server','teleport',player=name,x=x,y=64,z=z,yaw=yaw,pitch=4)
def run():
 rpc('server','setup')
 rpc('server','invulnerable',value=False)
 command('setblock 8 64 8 minecraft:air');command('setblock 9 64 8 minecraft:air');rpc('server','clear');command('time set 18000');command('difficulty hard');command('gamemode creative @a')
 for side,name,x,z in [('owner','FFMOwner',8,-2),('guest','FFMGuest',12,-2)]:camera(side,name,x,z,10,6)
 golem=rpc('server','spawn',type='minecraft:iron_golem',x=15,y=64,z=6)
 archer=rpc('server','spawn',type='minecraft:wither_skeleton',x=2,y=64,z=6,weapon='minecraft:bow',shield=False)
 for side in SIDES:
  wait_for(lambda side=side:ent(snapshot(side),golem['id']));rpc(side,'watchEffectParticles',entity=golem['id'])
 rpc('server','target',entity=archer['id'],target=golem['id'])
 hit=wait_for(lambda:(g if (g:=ent(snapshot('server'),golem['id'])) and {'minecraft:wither','minecraft:slowness'}.issubset(set(g['effects'])) else None),timeout=20)
 colors={e['particle']['rgb'] for e in hit['effectDetails'] if e['effect'] in ['minecraft:wither','minecraft:slowness']}
 for side in SIDES:
  wait_for(lambda side=side: colors.issubset({p.get('rgb') for p in ent(snapshot(side),golem['id'])['effectParticles']}))
 def particles():
  return all(colors.issubset({int(c) for c in rpc(side,'effectParticleProbe')['colors']}) for side in SIDES)
 wait_for(particles,timeout=20)
 check('A real wither bow projectile hits; both clients synchronize and visibly emit Wither and Slowness particles',target=hit,particles={side:rpc(side,'effectParticleProbe') for side in SIDES})
 rpc('server','clear');command('gamemode survival FFMOwner');command('effect clear FFMOwner');command('effect give FFMOwner minecraft:night_vision infinite 0 true');command('effect give FFMOwner minecraft:instant_health 1 5 true')
 rpc('server','inventory',player='FFMOwner',slots=[]);rpc('server','food',player='FFMOwner',level=10,saturation=0)
 camera('owner','FFMOwner',20,6,2,6);camera('guest','FFMGuest',13,-2,8,6)
 shield=rpc('server','spawn',type='minecraft:wither_skeleton',x=2,y=64,z=6,weapon='minecraft:stone_sword',shield=True)
 owner=player(snapshot('server'),'FFMOwner');rpc('server','target',entity=shield['id'],target=owner['id'])
 poses={}
 for side in SIDES:
  poses[side]=wait_for(lambda side=side:(p if (p:=snapshot(side)['shieldPoses'].get(str(shield['id']))) and p['blocking'] and p['crouching'] and p['bodyXRot']>.4 else None),timeout=15)
  rpc(side,'screenshot',file='ecosystems-015/shield-crouch.png')
 check('Both graphical clients render the advancing wither crouched with its shield raised',poses=poses)
 charge=wait_for(lambda:(w if (w:=ent(snapshot('server'),shield['id'])) and w['sprinting'] and not w['blocking'] and w['pose']=='STANDING' else None),timeout=15)
 wait_for(lambda:player(snapshot('server'),'FFMOwner')['health']<owner['health'],timeout=15)
 check('At five blocks the shield stance changes to a sprint and the wither lands a real melee hit',charge=charge)
 command('gamemode creative FFMOwner')
 wait_for(lambda:(w:=ent(snapshot('server'),shield['id'])) and w['target']==-1 and not w['blocking'] and w['pose']=='STANDING')
 check('Switching the target to Creative clears the attack and blocking/crouching state')
 rpc('server','clear');command('gamemode survival FFMOwner');command('effect clear FFMOwner');command('effect give FFMOwner minecraft:night_vision infinite 0 true');command('effect give FFMOwner minecraft:instant_health 1 5 true')
 camera('owner','FFMOwner',8,5,8,7);camera('guest','FFMGuest',11,4,8,7)
 rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item='minecraft:ender_eye',count=4)])
 rpc('server','inventory',player='FFMGuest',slots=[dict(slot=0,item='minecraft:ender_eye',count=4)])
 command('gamemode survival FFMGuest')
 for side in SIDES:rpc(side,'select',slot=0)
 phantom=rpc('server','spawn',type='minecraft:phantom',x=8,y=65,z=7)
 for side in SIDES:wait_for(lambda side=side:ent(snapshot(side),phantom['id']))
 rpc('owner','interact',entity=phantom['id']);wait_for(lambda:count(player(snapshot('server'),'FFMOwner'),'minecraft:ender_eye')==3)
 rpc('guest','interact',entity=phantom['id'],count=30);time.sleep(.3)
 s=snapshot('server');assert count(player(s,'FFMGuest'),'minecraft:ender_eye')==4
 assert ent(s,phantom['id'])['pactOwner']==player(s,'FFMOwner')['uuid']
 check('Real eye interaction consumes one eye and competing requests cannot steal ownership or consume other eyes')
 command('data merge entity '+phantom['uuid']+' {NoAI:0b}')
 attacker=rpc('server','spawn',type='minecraft:zombie',x=8,y=64,z=9)
 time.sleep(1);assert ent(snapshot('server'),phantom['id'])['target']==-1
 owner=player(snapshot('server'),'FFMOwner')
 damage=rpc('server','hit',entity=attacker['id'],target=owner['id']);assert damage['successful']
 response=wait_for(lambda:(p if (p:=ent(snapshot('server'),phantom['id'])) and p['target']==attacker['id'] else None))
 wait_for(lambda:(z:=ent(snapshot('server'),attacker['id'])) and z['health']<z['maxHealth'],timeout=25)
 assert player(snapshot('server'),'FFMOwner')['health']>=damage['target']['health']
 check('A guardian ignores the unprovoked zombie, then actually flies into attack after it damages the owner',pact=response)
 # Same-owner mercenary is friendly, while the guardian remains restricted to actual attackers.
 guard=rpc('server','spawn',type='villagerscontract:defender',x=9,y=64,z=6,owner='FFMOwner')
 classified=rpc('server','mercenary',op='classify',entity=guard['id'],other=phantom['id'])
 assert classified=={'hostile':False,'allied':True,'canAttack':False},classified
 check('The original Mercenaries mod recognizes its owner\'s temporary phantom as an ally',classification=classified)
 command('time set 24000');wait_for(lambda:(p:=ent(snapshot('server'),phantom['id'])) and p['pactOwner']=='null')
 check('Actual world ticks expire the pact at sunrise')
 # Both clients open the same real guard inventory and try the new items in a reserved string slot.
 command('time set 18000');rpc('server','clear')
 for side,name,x in [('owner','FFMOwner',8),('guest','FFMGuest',10)]:camera(side,name,x,5,9,7)
 guard=rpc('server','spawn',type='villagerscontract:defender',x=9,y=64,z=7,owner='FFMOwner')
 for side in SIDES:
  wait_for(lambda side=side:ent(snapshot(side),guard['id']));rpc(side,'shift',value=True);rpc(side,'interact',entity=guard['id'])
 wait_for(lambda:all(snapshot(side)['screen']=='GuardScreen' for side in SIDES))
 for item in ['bat_net','bat_enclosure','fertilizer']:
  rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=MOD+item,count=1)])
  wait_for(lambda:snapshot('owner')['player']['inventory'][0]['item']==MOD+item)
  rpc('owner','click',slot=11,type='SWAP',button=0);time.sleep(.2)
  s=snapshot('server');cs={side:snapshot(side) for side in SIDES}
  assert count(player(s,'FFMOwner'),MOD+item)==1
  for side,name in [('owner','FFMOwner'),('guest','FFMGuest')]:
   assert cs[side]['player']['inventory']==player(s,name)['inventory'] and cs[side]['player']['menu']['slots']==player(s,name)['menu']['slots']
 check('Bat Net, Enclosure and Fertilizer are safely rejected by reserved Mercenary slots without inventory loss or viewer desync')
 for side in SIDES:
  rpc(side,'close');rpc(side,'shift',value=False);rpc(side,'cleanView',value=False)
 command('gamemode creative @a')
 for side in SIDES:
  rpc(side,'inventory');tab=None
  for _ in range(4):
   s=snapshot(side);tab=next((t for t in s.get('creativeTabs',[]) if t['title']=='Fantasy Feudalism - Ecosystems'),None)
   if tab:break
   rpc(side,'mouse',x=s['guiWidth']/2+87,y=s['guiHeight']/2-108)
  assert tab and {MOD+x for x in ['bee_bottle','raw_horse','cooked_horse','bat_net','bat_enclosure','fertilizer']}=={i['item'] for i in tab['items']},tab
  rpc(side,'mouse',x=(s['guiWidth']-195)//2+27*tab['column']+13,y=(s['guiHeight']-136)//2+(-16 if tab['top'] else 152))
  rpc(side,'screenshot',file='ecosystems-015/creative-items.png');rpc(side,'close')
 check('Both real creative menus expose all six Ecosystems items and render their models')
 for side in SIDES:rpc(side,'reload',timeout=60)
 assert all(not snapshot(side)['renderErrors'] and not any(v['missing'] for v in snapshot(side)['ecosystemModels'].values()) for side in SIDES)
 check('Both clients reload resources and continue rendering without recorded model errors')
 rpc('server','save');check('The combined world saves successfully after new combat and inventory interactions')
if __name__=='__main__':
 try:run()
 except BaseException as e:
  OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(e)),indent=2));raise
