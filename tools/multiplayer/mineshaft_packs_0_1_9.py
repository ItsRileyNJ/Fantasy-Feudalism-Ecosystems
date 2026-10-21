"""Normal natural-spawner packs in the disposable seed-1 generated mineshaft."""
from control import *
import math

results = []
report = TEST_ROOT / 'mineshaft-packs-0.1.9-results.json'
isolation = False
protected = set()
isolation_clears = 0

def server_state():
    global isolation_clears
    state = snapshot('server')
    if isolation:
        for mob in state['entities']:
            if mob['type'] != 'minecraft:silverfish' and mob['id'] not in protected:
                rpc('server', 'remove', entity=mob['id'])
                isolation_clears += 1
    return state
def record(name, detail):
    results.append(dict(name=name, status='passed', detail=detail))
    report.write_text(json.dumps(results, indent=2))
    print('PASS', name, flush=True)
def command(value):
    return rpc('server', 'command', command=value)
def fish(state):
    return [e for e in state['entities'] if e['type'] == 'minecraft:silverfish' and e['alive']]
def distance(a, b):
    return math.dist(a['position'], b['position'])
def observe(anchor):
    x, y, z = anchor['position']
    site = rpc('server', 'zombiePosition', entity=anchor['id'])
    dx, dz = x - site['x'], z - site['z']
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = math.degrees(math.atan2(site['y'] + 1.62 - (y + .2), math.hypot(dx, dz)))
    for name in ['FFMOwner', 'FFMGuest']:
        rpc('server', 'teleport', player=name, **site, yaw=yaw, pitch=pitch)

rpc('server', 'snapshot', timeout=600)
for side in ['owner', 'guest']:
    if not rpc(side, 'snapshot', timeout=600)['connected']:
        rpc(side, 'connect')
wait_for(lambda: len(server_state()['players']) == 2, timeout=180)
command('gamerule doMobSpawning false')
command('difficulty normal')
command('gamemode creative @a')
command('time set 6000')
command('gamerule doDaylightCycle false')
command('gamerule mobGriefing true')
command('effect give @a minecraft:night_vision infinite 0 true')
shaft = rpc('server', 'mineshaft', timeout=60)
# The unchanged seed-1 structure is used; no tunnels, spawning floors or blocks are built.
room = shaft['pieces'][0]
cx = (room['min'][0] + room['max'][0]) / 2
cy = room['min'][1] + 2
cz = room['min'][2] - 35
for name, dx in [('FFMOwner', 0), ('FFMGuest', 2)]:
    rpc('server', 'teleport', player=name, x=cx + dx, y=cy, z=cz, yaw=0, pitch=10)
    rpc('server', 'inventory', player=name, slots=[])
start = server_state()['tick']
wait_for(lambda: server_state()['tick'] >= start + 40)
rpc('server', 'clear')
command('gamerule doMobSpawning true')
end = time.monotonic() + 180
last_clear = time.monotonic()
cleared = 0
pack = []
try:
    while time.monotonic() < end:
        state = server_state()
        candidates = [e for e in fish(state) if e['spawnType'] == 'NATURAL' and e['mineshaft']
                      and not e['burrowed'] and not e['killedZombie']
                      and e['health'] == e['maxHealth'] and not e['effects']]
        for anchor in candidates:
            nearby = [e for e in candidates if distance(e, anchor) <= 16]
            if len(nearby) >= 4:
                pack = nearby
                break
        if pack:
            break
        # Free competing mob-cap slots in this controlled fixture; never create silverfish.
        if time.monotonic() - last_clear >= 8:
            cleared += rpc('server', 'clear', **{'except': 'minecraft:silverfish'})
            last_clear = time.monotonic()
        time.sleep(.25)
    assert len(pack) >= 4, {'fish': fish(server_state()), 'births': rpc('server', 'naturalSilverfish')}
finally:
    command('gamerule doMobSpawning false')

births = rpc('server', 'naturalSilverfish')
ids = {e['id'] for e in pack}
assert ids.issubset({b['entity']['id'] for b in births if b['entity']['mineshaft']}), births
record('A group of at least four live silverfish spawned naturally inside an unchanged generated mineshaft',
       dict(pack=pack, births=births, competingEntitiesCleared=cleared, structure=shaft))
rpc('server', 'clear', **{'except': 'minecraft:silverfish'})
isolation = True
observe(pack[0])
start = server_state()['tick']
wait_for(lambda: server_state()['tick'] >= start + 220)
remaining = fish(server_state())
assert ids.issubset({e['id'] for e in remaining}), remaining
record('The natural pack remains above ground for over ten seconds with mob griefing enabled', remaining)

# Observe the same entities through real multiplayer tracking before exercising hunting.
anchor = next(e for e in remaining if e['id'] in ids)
x, y, z = anchor['position']
observe(anchor)
for side in ['owner', 'guest']:
    wait_for(lambda: ids.issubset({e['id'] for e in fish(snapshot(side))}), timeout=45)
    assert not snapshot(side)['renderErrors']
record('Both graphical clients track the same live pack without recorded rendering errors', sorted(ids))
# Keep the other player's model out of each camera when capturing the shared site.
for side,other in [('owner','FFMGuest'),('guest','FFMOwner')]:
    anchor=next(e for e in fish(server_state()) if e['id'] in ids)
    observe(anchor)
    other_player=player(server_state(),other)
    x,y,z=other_player['position']
    rpc('server','teleport',player=other,x=x,y=y+12,z=z,yaw=0,pitch=0)
    time.sleep(.5)
    rpc(side,'screenshot',file='mineshaft-pack-0.1.9-'+side+'.png')
rpc('server','save')
