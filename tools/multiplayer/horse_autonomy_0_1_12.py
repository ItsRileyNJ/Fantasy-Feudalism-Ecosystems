"""Normal AI grazing and a full 150-minute automatic breeding cycle.
Only /tick sprint accelerates time; no horse timer, love state, goal or offspring is assigned.
Requires the separate diagnostic harness, Mercenaries and two local graphical clients.
"""
import json,pathlib,math
from control import rpc,snapshot,wait_for,player
OUT=pathlib.Path('/workspace/multiplayer/horse-autonomy-0.1.12-results.json')
checks=[]
def command(text):return rpc('server','command',command=text)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def horses(s):return [e for e in s['entities'] if e['type']=='minecraft:horse']
def check(name,**detail):
    checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def step(ticks):
    target=snapshot('server')['gameTime']+ticks
    return wait_for(lambda:(s if (s:=snapshot('server'))['gameTime']>=target else None),timeout=30)
def sprint_until(target):
    now=snapshot('server')['gameTime'];command('tick sprint '+str(max(1,target-now))+'t')
    return wait_for(lambda:(s if (s:=snapshot('server'))['gameTime']>=target else None),timeout=240)

def run():
    rpc('server','setup');rpc('server','clear')
    for text in ['forceload add 0 0 31 31','fill 0 64 0 31 70 31 minecraft:air',
                 'fill 0 63 0 31 63 31 minecraft:grass_block',
                 'fill 0 64 0 31 65 0 minecraft:oak_fence','fill 0 64 31 31 65 31 minecraft:oak_fence',
                 'fill 0 64 0 0 65 31 minecraft:oak_fence','fill 31 64 0 31 65 31 minecraft:oak_fence',
                 'time set 6000','gamerule doDaylightCycle true','gamerule mobGriefing true','gamerule randomTickSpeed 3']:
        command(text)
    for side,name,x,yaw in [('owner','FFMOwner',12,180),('guest','FFMGuest',9,-135)]:
        command('gamemode creative '+name);command('effect clear '+name)
        rpc('server','teleport',player=name,x=x,y=64,z=16,yaw=yaw,pitch=12)
        rpc('server','inventory',player=name,slots=[]);rpc(side,'close');rpc(side,'cleanView',value=True)
    rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item='minecraft:golden_carrot',count=1)])
    rpc('owner','select',slot=0)
    herd=rpc('server','horseHerd',count=2,x=12,y=64,z=12);ids=[h['id'] for h in herd]
    state=snapshot('server');start=state['gameTime'];deadline=min(h['nextBreed'] for h in herd)
    assert deadline>=start+179990 and len({h['herd'] for h in herd})==1
    check('Fresh natural adult horses retain the full 150-minute deadline',start=start,deadline=deadline,herd=herd)
    def rendered_grazing():
        states={side:snapshot(side) for side in ['owner','guest']}
        for eid in ids:
            if all((h:=ent(s,eid)) and h['grazing'] and h['eatAnim']>.2
                   and (p:=s['horsePoses'].get(str(eid))) and p['grazing'] and p['headX']>1 for s in states.values()):
                return dict(horse=eid,clients={side:dict(horse=ent(s,eid),pose=s['horsePoses'][str(eid)]) for side,s in states.items()})
        return None
    pose=wait_for(rendered_grazing,timeout=60)
    obs=rpc('server','horseObservation')
    first=obs['firstGrazes'][str(pose['horse'])]
    assert first<start+850 and first<deadline
    assert any(s['item']=='minecraft:golden_carrot' and s['count']==1 for s in player(snapshot('server'),'FFMOwner')['inventory'])
    check('Both clients render ordinary scheduled grazing while the owner keeps holding a golden carrot',firstGrazeTick=first,elapsedTicks=first-start,**pose)
    rpc('owner','screenshot',file='horse-autonomous-grazing.png')
    def both_grazed():
        obs=rpc('server','horseObservation');pasture=rpc('server','pasture')
        return dict(observation=obs,pasture=pasture) if all(obs['grazes'].get(str(i),0)>=1 for i in ids) and pasture['dirt']>0 else None
    detail=wait_for(both_grazed,timeout=60)
    assert not any(h['baby'] for h in horses(snapshot('server')))
    check('Both adults graze and consume real grass before breeding readiness',**detail)
    rpc('server','inventory',player='FFMOwner',slots=[])
    # Release food-following so the herd can roam the pasture through the entire elapsed-time test.
    state=sprint_until(start+144000)
    assert not any(h['baby'] for h in horses(state))
    assert all(ent(state,i)['nextBreed']==next(h['nextBreed'] for h in herd if h['id']==i) for i in ids)
    check('Two hours of normal world ticks produce repeated grazing but no premature foal',elapsedTicks=state['gameTime']-start,observation=rpc('server','horseObservation'),horses=horses(state))
    state=sprint_until(deadline+6000)
    foals=[h for h in horses(state) if h['baby']]
    assert len(foals)==1,foals
    foal=foals[0];obs=rpc('server','horseObservation');birth=obs['births'][str(foal['id'])]
    assert birth>=deadline
    assert all(ent(state,i)['nextBreed']==birth+180000 for i in ids)
    assert foal['herd']==herd[0]['herd'] and not foal['tamed'] and all(not ent(state,i)['tamed'] for i in ids)
    check('The full natural 150-minute clock produces exactly one untamed foal and resets both parents',deadline=deadline,birthTick=birth,elapsedTicks=state['gameTime']-start,foal=foal,parents=[ent(state,i) for i in ids],observation=obs)
    def tracked_family():
        states={side:snapshot(side) for side in ['owner','guest']}
        return states if all(all(ent(s,i) for i in ids+[foal['id']]) and ent(s,foal['id'])['baby'] for s in states.values()) else None
    states=wait_for(tracked_family,timeout=60)
    for side,s in states.items():assert s['connected'] and not s['renderErrors']
    check('Both graphical clients receive the same single foal without render errors',clients={side:[ent(s,i) for i in ids+[foal['id']]] for side,s in states.items()})
    state=sprint_until(state['gameTime']+500)
    assert len([h for h in horses(state) if h['baby']])==1
    command('gamemode survival FFMOwner');step(4);rpc('owner','inventory')
    wait_for(lambda:snapshot('owner')['screen']=='InventoryScreen');rpc('owner','close')
    rpc('server','save')
    check('The family remains stable without duplicate foals and the normal inventory still works',horses=horses(state))
    # Only the final illustration changes daylight; the elapsed-time checks above use natural world ticks.
    command('gamemode creative FFMOwner');command('time set 6000');command('gamerule doDaylightCycle false')
    command('effect give FFMOwner minecraft:night_vision 30 0 true')
    family=horses(snapshot('server'));cx=sum(h['position'][0] for h in family)/len(family);cz=sum(h['position'][2] for h in family)/len(family)
    px,pz=max(2,cx-8),max(2,cz-8)
    yaw=math.degrees(math.atan2(-(cx-px),cz-pz))
    rpc('server','teleport',player='FFMOwner',x=px,y=64,z=pz,yaw=yaw,pitch=5)
    step(40);rpc('owner','screenshot',file='horse-autonomous-family.png')
    command('forceload remove 0 0 31 31')

if __name__=='__main__':
    try:run()
    except BaseException as error:
        OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(error)),indent=2));raise
