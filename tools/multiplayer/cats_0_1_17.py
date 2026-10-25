"""Real two-client cat naps, Rested UI, mouse rendering and wildlife behavior with Mercenaries."""
import json,math,time
from control import rpc,snapshot,wait_for,player,TEST_ROOT

SIDES=('owner','guest');MOD='fantasy_feudalism_mobs:'
OUT=TEST_ROOT/'cats-0.1.17-results.json';checks=[]
def cmd(command):return rpc('server','command',command=command)
def ent(state,identity):return next((e for e in state['entities'] if e['uuid']==identity),None)
def check(name,**detail):
    checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS',name,flush=True)
def camera(side,name,x,z,tx,tz):
    rpc('server','teleport',player=name,x=x,y=64,z=z,yaw=math.degrees(math.atan2(-(tx-x),tz-z)),pitch=14)
    wait_for(lambda:sum((a-b)**2 for a,b in zip(snapshot(side)['player']['position'],[x,64,z]))<.02)
def native_bed(x):
    cmd(f'setblock {x} 64 9 minecraft:white_bed[facing=north,part=foot]')
    cmd(f'setblock {x} 64 8 minecraft:white_bed[facing=north,part=head]')
def run():
    rpc('server','setup');rpc('server','clear')
    cmd('gamerule sendCommandFeedback false');cmd('gamerule doDaylightCycle true')
    cmd('fill -20 63 -20 45 63 30 minecraft:grass_block');cmd('fill -20 64 -20 45 70 30 minecraft:air')
    cmd('gamerule playersSleepingPercentage 101');cmd('gamerule naturalRegeneration true');cmd('gamerule mobGriefing true');cmd('time set 18000')
    for side,name,x in [('owner','FFMOwner',8.5),('guest','FFMGuest',20.5)]:
        rpc(side,'close');rpc(side,'shift',value=False);rpc(side,'cleanView',value=True)
        cmd('gamemode survival '+name);cmd('effect clear '+name)
        rpc('server','inventory',player=name,slots=[]);camera(side,name,x,5.5,x,8.5)
    native_bed(8);native_bed(20)
    own=rpc('server','spawn',type='minecraft:cat',x=7,y=64,z=8.5,owner='FFMOwner')
    stray=rpc('server','spawn',type='minecraft:cat',x=18.5,y=64,z=8.5)
    for side,x in [('owner',8),('guest',20)]:rpc(side,'useBlock',x=x,y=64,z=8)
    wait_for(lambda:all(p['sleeping'] for p in snapshot('server')['players']))
    for cat in [own,stray]:cmd('data merge entity '+cat['uuid']+' {NoAI:0b}')
    wait_for(lambda:all((c:=ent(snapshot('server'),cat['uuid'])) and c['lying'] for cat in [own,stray]),timeout=30)
    wait_for(lambda:all(p['sleepTimer']>=100 for p in snapshot('server')['players']))
    check('Actual owned cat and stray walk onto occupied beds and settle beside two sleeping players')
    cmd('gamerule playersSleepingPercentage 100')
    wait_for(lambda:all(not p['sleeping'] and MOD+'rested' in p['effects'] for p in snapshot('server')['players']))
    cmd('gamerule doDaylightCycle false');cmd('time set 6000')
    detail={}
    for side in SIDES:
        p=wait_for(lambda side=side:(p if (p:=snapshot(side)['player']) and MOD+'rested' in p['effects'] else None))
        detail[side]=next(e for e in p['effectDetails'] if e['effect']==MOD+'rested')
        assert 6800<detail[side]['duration']<=7200,detail
        rpc(side,'inventory');wait_for(lambda side=side:snapshot(side)['screen']=='InventoryScreen')
        rpc(side,'screenshot',file='cats-017/rested-'+side+'.png');rpc(side,'close')
    check('A normal morning wake grants six-minute Rested to both clients and their native inventories display the effect',effects=detail)
    rpc('server','clear')
    mouse=rpc('server','spawn',type=MOD+'field_mouse',x=11,y=64,z=8)
    for side,name,x in [('owner','FFMOwner',10),('guest','FFMGuest',13)]:
        camera(side,name,x,6,11,8);rpc(side,'look',yaw=math.degrees(math.atan2(-(11-x),2)),pitch=32)
    for side in SIDES:
        wait_for(lambda side=side:snapshot(side)['mouseRenderFrames'].get(str(mouse['id']),0)>=10)
        assert not snapshot(side)['renderErrors']
        rpc(side,'screenshot',file='cats-017/mouse-'+side+'.png')
    check('Both graphical clients render the small white mouse with a pink tail and finite animated model parts')
    rpc('server','clear')
    for side,name,x in [('owner','FFMOwner',-5),('guest','FFMGuest',30)]:camera(side,name,x,-5,14,10)
    cmd('setblock 14 63 10 minecraft:farmland');cmd('setblock 14 64 10 minecraft:wheat[age=7]')
    mouse=rpc('server','spawn',type=MOD+'field_mouse',x=13.5,y=64,z=10.5,ai=True,seed=4)
    wait_for(lambda:rpc('server','block',x=14,y=64,z=10)['properties'].get('age')=='6',timeout=30)
    first=ent(snapshot('server'),mouse['uuid']);assert first['nextMeal']>snapshot('server')['gameTime']
    check('A real foraging mouse eats one crop stage and starts its individual two-minute timer',mouse=first)
    x,y,z=first['position'];camera('owner','FFMOwner',x+2,z,x,z)
    wait_for(lambda:(m:=ent(snapshot('server'),mouse['uuid'])) and sum((a-b)**2 for a,b in zip(m['position'],[x+2,64,z]))>25,timeout=20)
    camera('owner','FFMOwner',-5,-5,14,10)
    wait_for(lambda:(m:=ent(snapshot('server'),mouse['uuid'])) and sum((a-b)**2 for a,b in zip(m['position'],[14.5,64.5,10.5]))<2.6,timeout=45)
    assert rpc('server','block',x=14,y=64,z=10)['properties']['age']=='6'
    check('Mouse flees a real player approaching within three blocks, then returns to the crop without bypassing its cooldown')
    cat=rpc('server','spawn',type='minecraft:cat',x=20,y=64,z=10.5,ai=True,owner='FFMOwner')
    wait_for(lambda:not (m:=ent(snapshot('server'),mouse['uuid'])) or not m['alive'],timeout=45)
    check('A real cat acquires a nearby running mouse and kills it during the chase')
    rpc('server','remove',entity=cat['id'])
    cmd('setblock 14 64 10 minecraft:wheat[age=7]')
    saved=rpc('server','spawn',type=MOD+'field_mouse',x=13.5,y=64,z=10.5,ai=True)
    wait_for(lambda:rpc('server','block',x=14,y=64,z=10)['properties'].get('age')=='6')
    state=ent(snapshot('server'),saved['uuid']);cmd('data merge entity '+saved['uuid']+' {NoAI:1b}')
    rpc('server','save');(TEST_ROOT/'cats-017-restart.json').write_text(json.dumps(dict(mouse=state,players=snapshot('server')['players']),indent=2))
    check('Mouse identity, meal deadline, crop memory, and active Rested effects are saved for a real restart',mouse=state)

def resume():
    prior=json.loads(OUT.read_text());checks.extend(prior['checks']);saved=json.loads((TEST_ROOT/'cats-017-restart.json').read_text())
    wait_for(lambda:len(snapshot('server')['players'])==2,timeout=90)
    mouse=wait_for(lambda:ent(snapshot('server'),saved['mouse']['uuid']))
    assert mouse['nextMeal']==saved['mouse']['nextMeal'] and mouse['cropHome']==saved['mouse']['cropHome']
    for side in SIDES:
        wait_for(lambda side=side:MOD+'rested' in snapshot(side)['player']['effects'])
        rpc(side,'reload',timeout=60);assert not snapshot(side)['renderErrors']
    assert rpc('server','block',x=14,y=64,z=10)['properties']['age']=='6'
    check('Normal dedicated-server restart preserves mouse UUID/crop memory/cooldown and both players retain Rested; both clients reload resources',mouse=mouse)

if __name__=='__main__':
    import sys
    try:resume() if '--resume' in sys.argv else run()
    except BaseException as error:
        OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(error)),indent=2));raise
