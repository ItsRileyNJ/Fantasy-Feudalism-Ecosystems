"""Two-client loopback regression for wolf packs and rare baby-villager cat play.
Requires final release plus separate local diagnostic harness; modifies disposable world.
"""
import json, math, pathlib, time
from control import rpc,snapshot,wait_for,player
OUT=pathlib.Path('/workspace/multiplayer/wolf-cohesion-0.1.9-results.json')
checks=[]
def command(s):return rpc('server','command',command=s)
def ent(s,eid):return next((e for e in s['entities'] if e['id']==eid),None)
def step(ticks):
    end=snapshot('server')['gameTime']+ticks
    return wait_for(lambda:(s if (s:=snapshot('server'))['gameTime']>=end else None),timeout=max(30,ticks/10))
def check(name,**evidence):
    checks.append(dict(name=name,passed=True,**evidence));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def clients():return {side:snapshot(side) for side in ['owner','guest']}
def tracked(ids):
    states=clients()
    return states if all(all(ent(s,i) for i in ids) for s in states.values()) else None

def run():
    rpc('server','setup');rpc('server','clear');command('time set 4000');command('gamerule sendCommandFeedback false')
    for side,name,x,yaw in [('owner','FFMOwner',7,0),('guest','FFMGuest',9,10)]:
        command('gamemode creative '+name);command('effect clear '+name)
        rpc('server','inventory',player=name,slots=[])
        rpc('server','teleport',player=name,x=x,y=64,z=-5,yaw=yaw,pitch=8)
        rpc(side,'cleanView',value=True)
    # Spread one companion away from the stable leader, then observe ordinary navigation.
    pack=rpc('server','wolfPack',count=3,x=2,y=64,z=3)
    import uuid
    def java_uuid_key(w):
        value=uuid.UUID(w['uuid']).int
        hi,lo=value>>64,value&((1<<64)-1)
        return (hi-(1<<64) if hi>>63 else hi,lo-(1<<64) if lo>>63 else lo)
    leader=min(pack,key=java_uuid_key);follower=next(w for w in pack if w['id']!=leader['id'])
    command('tp '+follower['uuid']+' 19 64 5')
    def regrouped():
        s=snapshot('server');a,b=ent(s,leader['id']),ent(s,follower['id'])
        return s if a and b and math.dist(a['position'],b['position'])<4 else None
    state=wait_for(regrouped,timeout=25)
    check('Separated wild wolf walks back into its pack',leader=ent(state,leader['id']),follower=ent(state,follower['id']))
    rpc('server','clear')
    prey=rpc('server','spawn',type='minecraft:sheep',x=16,y=64,z=5,maxHealth=40)
    spare=rpc('server','spawn',type='minecraft:sheep',x=18,y=64,z=10)
    pack=rpc('server','wolfPack',count=3,x=2,y=64,z=3);ids=[w['id'] for w in pack]
    pet=rpc('server','spawn',type='minecraft:wolf',x=7,y=64,z=3,ai=True,owner='FFMOwner')
    wait_for(lambda:tracked(ids+[pet['id']]),timeout=45)
    def stalking():
        s=snapshot('server')
        return s if all((w:=ent(s,i)) and w['stalking'] and w['target']==prey['id'] for i in ids) else None
    state=wait_for(stalking,timeout=20)
    assert len({ent(state,i)['pack'] for i in ids})==1
    assert ent(state,prey['id'])['health']==40
    starts=[ent(state,i)['firstStalk'] for i in ids]
    assert min(starts)>=0 and max(starts)-min(starts)<=1,starts
    check('All three wild wolves start stalking within one server tick',firstStalkTicks=starts)
    check('Wild pack shares one sheep and stalks without hitting from range',wolves=[ent(state,i) for i in ids])
    def rendered_stalk():
        states=clients()
        for s in states.values():
            poses=s['wolfPoses']
            if not any((p:=poses.get(str(i))) and p['stalking'] and p['body']['y']>16 and abs(p['tail']['xRot'])<.3 and p['rightFrontLeg']['scaleY']<.85 for i in ids):return None
        return states
    seen=wait_for(rendered_stalk,timeout=15)
    # Sample a moving, visible hunter twice to verify the crouch retains a walking gait.
    first={side:{i:s['wolfPoses'].get(str(i)) for i in ids} for side,s in seen.items()}
    step(10);seen=wait_for(rendered_stalk,timeout=10)
    for side,s in seen.items():
        assert any(first[side].get(i) and s['wolfPoses'].get(str(i)) and abs(first[side][i]['rightFrontLeg']['xRot']-s['wolfPoses'][str(i)]['rightFrontLeg']['xRot'])>.02 for i in ids)
    check('Both clients render low body, head/tail down and animated crouched steps',poses={side:{str(i):s['wolfPoses'].get(str(i)) for i in ids} for side,s in seen.items()})
    rpc('owner','screenshot',file='wolf-pack-stalking.png')
    for side,s in seen.items():
        pose=s['wolfPoses'].get(str(pet['id']))
        assert pose and pose['tamed'] and not pose['stalking'] and pose['body']['y']==14 and pose['head']['y']==13.5 and pose['rightFrontLeg']['scaleY']==1
    check('Nearby tamed wolves retain vanilla model pose while wild wolves crouch',poses={side:s['wolfPoses'][str(pet['id'])] for side,s in seen.items()})

    state=wait_for(lambda:(s if all(not ent(s:=snapshot('server'),i)['stalking'] for i in ids) else None),timeout=25)
    assert min(math.dist(ent(state,i)['position'],ent(state,prey['id'])['position']) for i in ids)<8
    check('The pack changes from stalking to charging together near seven blocks',wolves=[ent(state,i) for i in ids])
    def fed():
        s=snapshot('server')
        return s if all(ent(s,i)['mealUntil']>s['gameTime'] for i in ids) else None
    state=wait_for(fed,timeout=45);until=ent(state,ids[0])['mealUntil']
    assert all(ent(state,i)['mealUntil']==until for i in ids)
    state=step(80)
    assert ent(state,spare['id'])['health']==8
    for i in ids:assert ent(state,i)['target']==-1 and not ent(state,i)['stalking']
    check('One sheep kill feeds every pack member and leaves the next sheep unharmed',until=until,wolves=[ent(state,i) for i in ids],spare=ent(state,spare['id']))
    states=clients()
    for s in states.values():
        for i in ids:
            w=ent(s,i)
            assert w and not w['stalking'] and w['crouch']<.05
    check('Both clients return to the standing pose after the hunt',wolves={side:[ent(s,i) for i in ids] for side,s in states.items()})

    # Actual player attack requests tamed-wolf owner defense during the wild-pack cooldown.
    rpc('server','teleport',player='FFMOwner',x=17,y=64,z=8,yaw=0,pitch=0)
    rpc('owner','attack',entity=spare['id'])
    state=wait_for(lambda:(s if ent(s:=snapshot('server'),pet['id'])['target']==spare['id'] else None),timeout=20)
    assert ent(state,pet['id'])['tamed']
    check('Tamed owner-defense AI still targets sheep during the former pack cooldown',pet=ent(state,pet['id']))
    wait_for(lambda:not ent(snapshot('server'),spare['id']) or not ent(snapshot('server'),spare['id'])['alive'],timeout=25)
    state=snapshot('server');assert all(ent(state,i)['mealUntil']==until for i in ids)
    # Follow an owner and toggle sitting through the real interaction packet.
    rpc('server','teleport',player='FFMOwner',x=2,y=64,z=1,yaw=0,pitch=0)
    state=wait_for(lambda:(s if math.dist(ent(s:=snapshot('server'),pet['id'])['position'],player(s,'FFMOwner')['position'])<4 else None),timeout=25)
    rpc('owner','interact',entity=pet['id'])
    wait_for(lambda:ent(snapshot('server'),pet['id'])['sitting'],timeout=10)
    rpc('owner','interact',entity=pet['id'])
    wait_for(lambda:not ent(snapshot('server'),pet['id'])['sitting'],timeout=10)
    check('Tamed wolves still follow their owner and obey real sit/follow interactions')
    rpc('server','save')

    command('fill 12 63 -10 80 63 40 minecraft:grass_block')
    command('fill 12 64 -10 80 70 40 minecraft:air')
    for side,name,x in [('owner','FFMOwner',17),('guest','FFMGuest',20)]:
        rpc('server','teleport',player=name,x=x,y=64,z=0,yaw=0,pitch=8)
    fixture=rpc('server','villagePlay');child,cat=fixture['child'],fixture['cat'];end=child['playUntil'];start=end-160
    wait_for(lambda:tracked([child['id'],cat['id']]),timeout=20)
    state=step(35)
    actual=ent(state,child['id']);kitty=ent(state,cat['id'])
    assert actual['playCat']==cat['uuid'] and math.dist(actual['position'],child['position'])>.5
    assert actual['health']==child['health'] and kitty['health']==cat['health']
    check('Baby villager follows a moving untamed village cat without attacking',child=actual,cat=kitty,started=start)
    midx=(actual['position'][0]+kitty['position'][0])/2
    midz=(actual['position'][2]+kitty['position'][2])/2
    rpc('server','teleport',player='FFMOwner',x=midx,y=64,z=midz-8,yaw=0,pitch=9)
    step(3)
    rpc('owner','screenshot',file='baby-villager-cat-play.png')
    state=wait_for(lambda:(s if (s:=snapshot('server'))['gameTime']>=end+2 else None),timeout=20)
    actual=ent(state,child['id']);kitty=ent(state,cat['id'])
    assert actual['playCat']=='' and actual['playUntil']==0
    assert actual['health']==child['health'] and kitty['health']==cat['health']
    check('The harmless play chase stops after 160 server ticks',until=end,observed=state['gameTime'],child=actual,cat=kitty)
    states=clients();assert all(s['connected'] and not s['renderErrors'] for s in states.values())
    check('Both clients stay connected with no recorded render errors',clients={side:dict(connected=s['connected'],renderErrors=s['renderErrors']) for side,s in states.items()})

if __name__=='__main__':
    try:run()
    except Exception as error:
        OUT.write_text(json.dumps(dict(passed=False,error=str(error),checks=checks),indent=2));raise
