"""Two graphical clients: real sitting/feeding packets and natural predator AI.
Requires the separate diagnostic harness and disposable local dedicated world.
"""
import json,math,pathlib
from control import rpc,snapshot,wait_for,player
OUT=pathlib.Path('/workspace/multiplayer/guard-dogs-0.1.10-results.json')
checks=[]
def command(value):return rpc('server','command',command=value)
def ent(state,eid):return next((e for e in state['entities'] if e['id']==eid),None)
def step(ticks):
    until=snapshot('server')['gameTime']+ticks
    return wait_for(lambda:(s if (s:=snapshot('server'))['gameTime']>=until else None),timeout=max(30,ticks/10))
def check(name,**detail):
    checks.append(dict(name=name,passed=True,**detail));OUT.write_text(json.dumps(dict(passed=True,checks=checks),indent=2));print('PASS:',name,flush=True)
def tracked(eid):
    states={side:snapshot(side) for side in ['owner','guest']}
    return states if all(ent(s,eid) and str(eid) in s['wolfPoses'] for s in states.values()) else None
def hurt_sounds(state):return sum(v for k,v in state['wolfSounds'].items() if 'hurt' in k or 'armor.damage' in k)
def remove(eid):rpc('server','remove',entity=eid)

def run():
    rpc('server','setup');rpc('server','clear');command('time set 18000');command('gamerule sendCommandFeedback false')
    for side,name,x,yaw in [('owner','FFMOwner',12,0),('guest','FFMGuest',10,-20)]:
        command('gamemode creative '+name);command('effect clear '+name)
        rpc('server','inventory',player=name,slots=[])
        rpc('server','teleport',player=name,x=x,y=64,z=5,yaw=yaw,pitch=15)
        rpc(side,'cleanView',value=True);rpc(side,'close')
    dog=rpc('server','spawn',type='minecraft:wolf',x=12,y=64,z=7,ai=True,owner='FFMOwner');did=dog['id']
    wait_for(lambda:tracked(did),timeout=40)
    rpc('owner','interact',entity=did)
    wait_for(lambda:ent(snapshot('server'),did)['sitting'])
    step(5);base=wait_for(lambda:tracked(did));health=ent(snapshot('server'),did)['health']
    assert health==40 and all(ent(s,did)['sitting'] for s in base.values())
    tails={side:s['wolfPoses'][str(did)]['tail']['xRot'] for side,s in base.items()}
    sound_base={side:hurt_sounds(s) for side,s in base.items()}
    check('Real owner interaction seats a healthy guarding dog',dog=ent(snapshot('server'),did),baselineTailRotations=tails)
    prey=rpc('server','spawn',type='minecraft:sheep',x=16,y=64,z=5)
    pack=rpc('server','wolfPack',count=3,x=2,y=64,z=3);ids=[w['id'] for w in pack]
    def stopped_pack():
        s=snapshot('server')
        return s if all((w:=ent(s,i)) and w['mealUntil']>s['gameTime'] and w['target']==-1 for i in ids) else None
    state=wait_for(stopped_pack,timeout=20)
    assert len({ent(state,i)['mealUntil'] for i in ids})==1
    assert ent(state,did)['health']==38 and ent(state,prey['id'])['health']==8
    check('One seated dog deters a whole wild pack for one shared hunger cooldown and one heart',wolves=[ent(state,i) for i in ids],dog=ent(state,did),sheep=ent(state,prey['id']))
    def tired_pose():
        states=tracked(did)
        if not states:return None
        return states if all(ent(s,did)['health']==38 and ent(s,did)['sitting'] and s['wolfPoses'][str(did)]['tail']['xRot']<tails[side]-.02 for side,s in states.items()) else None
    states=wait_for(tired_pose,timeout=20)
    for side,s in states.items():
        assert s['wolfHurtMax'][str(did)]==0 and hurt_sounds(s)==sound_base[side]
    check('Both clients show the lower tail and health with no hurt animation or damage sound',clients={side:dict(dog=ent(s,did),pose=s['wolfPoses'][str(did)],hurtMax=s['wolfHurtMax'][str(did)],wolfSounds=s['wolfSounds']) for side,s in states.items()})
    rpc('owner','screenshot',file='guard-dog-fatigue.png')
    state=step(60)
    assert ent(state,did)['health']==38 and ent(state,prey['id'])['health']==8 and all(ent(state,i)['target']==-1 for i in ids)
    check('Repeated AI scans do not drain more health or hurt the guarded sheep',dog=ent(state,did))
    command('gamemode survival FFMOwner');rpc('server','inventory',player='FFMOwner',slots=[dict(slot=0,item='minecraft:beef',count=1)])
    rpc('owner','select',slot=0);step(4);rpc('owner','interact',entity=did)
    wait_for(lambda:ent(snapshot('server'),did)['health']==40)
    state=step(5);assert not any(s['item']=='minecraft:beef' and s['count'] for s in player(state,'FFMOwner')['inventory'])
    def healed_pose():
        states=tracked(did)
        return states if states and all(ent(c,did)['health']==40 and abs(c['wolfPoses'][str(did)]['tail']['xRot']-tails[side])<.005 for side,c in states.items()) else None
    states=wait_for(healed_pose,timeout=20)
    check('One real survival-mode feeding restores the health and tail and consumes one food',dog=ent(state,did),clients={side:c['wolfPoses'][str(did)] for side,c in states.items()})
    command('gamemode creative FFMOwner')
    for i in ids+[prey['id']]:remove(i)
    rpc('owner','interact',entity=did);wait_for(lambda:not ent(snapshot('server'),did)['sitting'])
    chicken=rpc('server','spawn',type='minecraft:chicken',x=22,y=64,z=5)
    fox=rpc('server','spawn',type='minecraft:fox',x=8,y=64,z=5,ai=True)
    state=wait_for(lambda:(s if ent(s:=snapshot('server'),fox['id'])['target']==chicken['id'] else None),timeout=35)
    assert ent(state,did)['health']==40 and ent(state,chicken['id'])['health']==4
    rpc('owner','interact',entity=did)
    state=wait_for(lambda:(s if ent(s:=snapshot('server'),fox['id'])['mealUntil']>s['gameTime'] and ent(s,fox['id'])['target']==-1 else None),timeout=20)
    assert ent(state,did)['health']==38 and ent(state,chicken['id'])['health']==4
    check('Sitting the dog interrupts a real fox chicken hunt and starts its one-day hunger cooldown',fox=ent(state,fox['id']),dog=ent(state,did),chicken=ent(state,chicken['id']))
    state=step(50);assert ent(state,did)['health']==38 and ent(state,fox['id'])['target']==-1
    check('The deterred fox cannot immediately resume hunting or repeatedly tire the dog',fox=ent(state,fox['id']),dog=ent(state,did))
    remove(fox['id']);remove(chicken['id'])
    dx,dy,dz=ent(snapshot('server'),did)['position']
    outside=rpc('server','spawn',type='minecraft:chicken',x=dx+25,y=dy,z=dz)
    fox=rpc('server','spawn',type='minecraft:fox',x=dx+14,y=dy,z=dz,ai=True)
    def normal_kill():
        s=snapshot('server');f=ent(s,fox['id']);c=ent(s,outside['id'])
        return s if f and f['mealUntil']>s['gameTime'] and (not c or not c['alive']) else None
    state=wait_for(normal_kill,timeout=45)
    assert ent(state,did)['health']==38
    check('A chicken outside the 24-block guard radius can be hunted and a real fox kill starts its meal cooldown',fox=ent(state,fox['id']),dog=ent(state,did))
    # With players creative and no wild wolves nearby, only guarded-prey avoidance drives this retreat.
    command('gamemode creative FFMOwner');remove(fox['id'])
    dx,dy,dz=ent(snapshot('server'),did)['position']
    close=rpc('server','spawn',type='minecraft:chicken',x=dx+4,y=dy,z=dz)
    fox=rpc('server','spawn',type='minecraft:fox',x=dx+8,y=dy,z=dz,ai=True)
    def retreated():
        s=snapshot('server');f,c=ent(s,fox['id']),ent(s,close['id'])
        return s if f and c and math.dist(f['position'],c['position'])>8 else None
    state=wait_for(retreated,timeout=20)
    assert ent(state,close['id'])['health']==4
    check('A nearby fox walks away from guarded chickens without another predator or survival player nearby',fox=ent(state,fox['id']),chicken=ent(state,close['id']),dog=ent(state,did))
    for side in ['owner','guest']:
        s=snapshot(side);assert s['connected'] and not s['renderErrors'] and s['wolfHurtMax'][str(did)]==0 and hurt_sounds(s)==sound_base[side]
    command('gamemode survival FFMOwner');step(4)
    rpc('owner','inventory');wait_for(lambda:snapshot('owner')['screen']=='InventoryScreen');rpc('owner','close')
    rpc('server','save')
    check('Both clients remain synchronized without hurt effects or render errors and inventory still opens')
    command('effect give FFMOwner minecraft:night_vision 30 0 true')
    dx,dy,dz=ent(snapshot('server'),did)['position']
    rpc('server','teleport',player='FFMOwner',x=dx-4,y=dy,z=dz,yaw=-90,pitch=10)
    step(6);rpc('owner','screenshot',file='guard-dog-profile.png')

if __name__=='__main__':
    try:run()
    except BaseException as error:
        OUT.write_text(json.dumps(dict(passed=False,checks=checks,error=repr(error)),indent=2));raise
