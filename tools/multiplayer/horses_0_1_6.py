"""Opt-in loopback fixture: final release + Mercenaries on server and two clients.

Requires the existing local diagnostic harness; never packaged into Mobs.
Run after both graphical clients have connected. Alters only the disposable test world.
"""
import json, math, pathlib, shutil, struct, time, uuid
from control import rpc, snapshot, wait_for, player, count

OUT = pathlib.Path('/workspace/multiplayer/horses-0.1.6-results.json')
MOD = 'fantasy_feudalism_mobs:'
results = []

def command(text):
    return rpc('server', 'command', command=text)

def entity(state, eid):
    return next((e for e in state['entities'] if e['id'] == eid), None)

def horses(state):
    return [e for e in state['entities'] if e['type'] == 'minecraft:horse']

def check(name, **evidence):
    results.append(dict(name=name, passed=True, **evidence))
    OUT.write_text(json.dumps(dict(passed=True, checks=results), indent=2))
    print('PASS:', name, flush=True)

def both_entities(ids):
    states = {side: snapshot(side) for side in ['owner', 'guest']}
    return states if all(all(entity(s, eid) for eid in ids) for s in states.values()) else None

def steps(ticks):
    end = snapshot('server')['gameTime'] + ticks
    return wait_for(lambda: (s if (s := snapshot('server'))['gameTime'] >= end else None), timeout=max(30, ticks/10))

def run():
    wait_for(lambda: len(snapshot('server')['players']) == 2, timeout=90)
    rpc('server', 'setup'); rpc('server', 'clear')
    command('time set 6000'); command('gamerule doDaylightCycle false');command('gamerule doMobSpawning false')
    command('fill -10 63 -10 25 63 18 minecraft:grass_block')
    for name,x in [('FFMOwner',4),('FFMGuest',7)]:
        command('gamemode creative '+name)
        command('effect clear '+name)
        command('effect give '+name+' minecraft:night_vision infinite 0 true')
        rpc('server','teleport',player=name,x=x,y=64,z=-6,yaw=0,pitch=12)
    herd=rpc('server','horseHerd',count=3,x=2,y=64,z=2)
    ids=[h['id'] for h in herd]
    assert len({h['herd'] for h in herd})==1 and herd[0]['herd']
    clients=wait_for(lambda:both_entities(ids),timeout=45)
    check('Server and both clients track one finalized horse spawn group',herd=herd)

    ordered=sorted(herd,key=lambda h:struct.unpack('>qq',uuid.UUID(h['uuid']).bytes))
    leader,follower=ordered[:2]
    command('data merge entity '+leader['uuid']+' {NoAI:1b}')
    command('tp '+leader['uuid']+' 3 64 3')
    command('tp '+follower['uuid']+' 14 64 3')
    def regrouped():
        state=snapshot('server');a=entity(state,leader['id']);b=entity(state,follower['id'])
        return state if math.dist(a['position'],b['position'])<4 else None
    state=wait_for(regrouped,timeout=40)
    check('A separated horse walks back to its own herd',leader=entity(state,leader['id']),follower=entity(state,follower['id']))

    for i,h in enumerate(herd):
        command('data merge entity '+h['uuid']+' {NoAI:1b}')
        command('tp '+h['uuid']+' '+str(2+i*2)+' 64 2')
    for h in herd[:2]:
        # Advance only saved cooldown eligibility; normal grazing and breeding AI
        # choose the mate and produce the foal. No love/offspring is assigned here.
        command('data merge entity '+h['uuid']+' {NeoForgeData:{ffm_horse_next_breed:0L}}')
    for h in herd:command('data merge entity '+h['uuid']+' {NoAI:0b}')
    posed=None
    end=time.monotonic()+60
    while time.monotonic()<end:
        state=snapshot('server')
        clients={side:snapshot(side) for side in ['owner','guest']}
        for h in herd[:2]:
            if all((e:=entity(s,h['id'])) and e['grazing'] and e['eatAnim']>.1
                   and s['horsePoses'].get(str(h['id']),{}).get('headX',0)>1
                   for s in clients.values()):
                posed=dict(entity=h['id'],server=entity(state,h['id']),clients={side:s['horsePoses'][str(h['id'])] for side,s in clients.items()})
                break
        if posed:break
        assert not any(h['baby'] for h in horses(state)), 'A foal appeared before both clients observed grazing'
        time.sleep(.05)
    assert posed, 'Both clients must render the synchronized native grazing pose'
    check('Both graphical clients render the native grazing head pose',**posed)
    rpc('owner','screenshot',file='horse-grazing.png')
    def one_foal():
        state=snapshot('server')
        return state if len([h for h in horses(state) if h['baby']])==1 else None
    state=wait_for(one_foal,timeout=45)
    foal=next(h for h in horses(state) if h['baby'])
    clients=wait_for(lambda:both_entities(ids+[foal['id']]),timeout=30)
    assert all(entity(s,foal['id'])['baby'] for s in clients.values())
    assert foal['herd']==herd[0]['herd'] and not foal['tamed']
    for parent in herd[:2]:
        actual=entity(state,parent['id'])
        assert actual['nextBreed']>=state['gameTime']+335800 and not actual['tamed']
    later=steps(160)
    assert len([h for h in horses(later) if h['baby']])==1
    rpc('server','save')
    check('Grass-triggered natural breeding creates exactly one synchronized foal and resets both timers',foal=foal,parents=[entity(later,h['id']) for h in herd[:2]])

    for side in ['owner','guest']:
        state=snapshot(side)
        for name,model in state['horseMeatModels'].items():
            assert not model['missing'] and model['sprite'].startswith(MOD+'item/')
    check('Both clients load the original raw and cooked horse meat sprites',models={side:snapshot(side)['horseMeatModels'] for side in ['owner','guest']})

    command('kill '+herd[2]['uuid'])
    state=steps(4)
    meat=[e for e in state['entities'] if e.get('item',{}).get('item')==MOD+'raw_horse']
    assert 1<=sum(e['item']['count'] for e in meat)<=2
    check('An adult horse drops one or two raw horse in the live server',drops=meat)

    for name in ['FFMOwner','FFMGuest']:command('gamemode survival '+name)
    rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item=MOD+'raw_horse',count=3),dict(slot=1,item=MOD+'cooked_horse',count=3)])
    rpc('server','inventory',player='FFMGuest',slots=[])
    rpc('server','food',player='FFMOwner',level=10,saturation=0)
    rpc('owner','select',slot=1);rpc('owner','look',yaw=0,pitch=-70)
    wait_for(lambda:player(snapshot('server'),'FFMOwner')['food']==10)
    rpc('owner','use',value=True)
    try:
        state=wait_for(lambda:(s if count(player(s:=snapshot('server'),'FFMOwner'),MOD+'cooked_horse')==2 else None),timeout=20)
    finally:rpc('owner','use',value=False)
    p=player(state,'FFMOwner')
    assert p['food']==16 and abs(p['saturation']-9.6)<.01
    client=wait_for(lambda:(s if (s:=snapshot('owner'))['player']['food']==16 and count(s['player'],MOD+'cooked_horse')==2 else None))
    check('Eating cooked horse consumes one item and synchronizes cooked-mutton hunger/saturation',serverPlayer=p,clientPlayer=client['player'])

    command('setblock 8 64 3 minecraft:smoker')
    command('item replace block 8 64 3 container.0 with '+MOD+'raw_horse 1')
    for side,name,x in [('owner','FFMOwner',8),('guest','FFMGuest',9)]:
        rpc('server','teleport',player=name,x=x,y=64,z=1,yaw=0,pitch=15)
        rpc(side,'select',slot=8)
        rpc(side,'useBlock',x=8,y=64,z=3)
    wait_for(lambda:all(snapshot(side)['player']['menu']['type']=='SmokerMenu' for side in ['owner','guest']),timeout=30)
    command('item replace block 8 64 3 container.1 with minecraft:coal 1')
    wait_for(lambda:all(snapshot(side)['player']['menu']['slots'][2]['item']==MOD+'cooked_horse' for side in ['owner','guest']),timeout=30)
    rpc('owner','screenshot',file='horse-smoker-two-viewers.png')
    rpc('owner','click',slot=2,button=0,type='PICKUP')
    def extracted():
        state=snapshot('server');owner=player(state,'FFMOwner');guest=player(state,'FFMGuest')
        return state if owner['cursor']['item']==MOD+'cooked_horse' and owner['cursor']['count']==1 and guest['menu']['slots'][2]['count']==0 else None
    state=wait_for(extracted)
    client=wait_for(lambda:(s if (s:=snapshot('guest'))['player']['menu']['slots'][2]['count']==0 else None))
    assert client['player']['cursor']['count']==0
    for side in ['owner','guest']:rpc(side,'close')
    state=wait_for(lambda:(s if count(player(s:=snapshot('server'),'FFMOwner'),MOD+'cooked_horse')==3 else None))
    assert count(player(state,'FFMGuest'),MOD+'cooked_horse')==0
    check('Two viewers share the smoker output without a duplicated or ghost item',players=state['players'])
    clients={side:snapshot(side) for side in ['owner','guest']}
    assert all(s['connected'] and not s['renderErrors'] for s in clients.values())
    check('Both clients remain connected with no recorded model-render errors',clients={side:dict(connected=s['connected'],renderErrors=s['renderErrors']) for side,s in clients.items()})

if __name__=='__main__':
    try:run()
    except Exception as error:
        OUT.write_text(json.dumps(dict(passed=False,error=str(error),checks=results),indent=2))
        for side in ['owner','guest']:
            try:rpc(side,'use',value=False,timeout=5)
            except Exception:pass
        raise
