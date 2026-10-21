"""Normal natural-spawner packs in the disposable seed-1 generated mineshaft."""
from control import *
import math

results = []
report = TEST_ROOT / 'mineshaft-packs-0.1.4-results.json'
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
            if len(nearby) >= 3:
                pack = nearby
                break
        if pack:
            break
        # Free competing mob-cap slots in this controlled fixture; never create silverfish.
        if time.monotonic() - last_clear >= 8:
            cleared += rpc('server', 'clear', **{'except': 'minecraft:silverfish'})
            last_clear = time.monotonic()
        time.sleep(.25)
    assert len(pack) >= 3, {'fish': fish(server_state()), 'births': rpc('server', 'naturalSilverfish')}
finally:
    command('gamerule doMobSpawning false')

births = rpc('server', 'naturalSilverfish')
ids = {e['id'] for e in pack}
assert ids.issubset({b['entity']['id'] for b in births if b['entity']['mineshaft']}), births
record('A group of at least three live silverfish spawned naturally inside an unchanged generated mineshaft',
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
command('effect give FFMGuest minecraft:resistance infinite 4 true')
command('gamemode survival FFMGuest')
guest_id = player(server_state(), 'FFMGuest')['id']
wait_for(lambda: any(e['target'] == guest_id for e in fish(server_state())), timeout=30)
player_hunters = {e['id'] for e in fish(server_state()) if e['target'] == guest_id}
record('The fixture establishes silverfish already pursuing a nearby survival player', sorted(player_hunters))
current = server_state()
hunter = next(e for e in fish(current) if e['id'] in player_hunters)
site = rpc('server', 'zombiePosition', entity=hunter['id'])
zombie = rpc('server', 'spawn', type='minecraft:zombie', maxHealth=200, **site)
protected.add(zombie['id'])
def hunting():
    state = server_state()
    target = next(e for e in state['entities'] if e['id'] == zombie['id'])
    return target['health'] < 200 and any(e['id'] in player_hunters and e['target'] == zombie['id'] for e in fish(state))
wait_for(hunting, timeout=45)
record('Naturally spawned silverfish switch from the player to acquire, chase and damage a zombie without forced targets', server_state())
command('gamemode creative FFMGuest')
for side in ['owner', 'guest']:
    rpc(side, 'screenshot', file='natural-mineshaft-pack-' + side + '.png')
before = {e['id'] for e in fish(server_state()) if e['killedZombie']}
rpc('server', 'health', entity=zombie['id'], health=1)
wait_for(lambda: any(k['zombie'] == zombie['uuid'] for k in rpc('server', 'silverfishKills')), timeout=30)
kill = next(k for k in rpc('server', 'silverfishKills') if k['zombie'] == zombie['uuid'])
killer_id = kill['killer']
wait_for(lambda: any(e['id'] == killer_id and e['burrowed'] for e in fish(server_state())), timeout=30)
state = server_state()
new_credit = {e['id'] for e in fish(state) if e['killedZombie']} - before
assert new_credit == {killer_id}, (new_credit, kill)
killer = next(e for e in fish(state) if e['id'] == killer_id)
deadline = killer['burrowUntil']
assert killer['invisible'] and killer['silent'] and killer['noAi']
survivors = ids - {killer_id}
assert all(not e['burrowed'] and not e['killedZombie'] for e in fish(state) if e['id'] in survivors)
record('Only the actual final hitter earns credit and burrows; surviving pack members keep roaming',
       dict(kill=kill, killer=killer, survivors=sorted(survivors)))
for side in ['owner', 'guest']:
    wait_for(lambda: any(e['id'] == killer_id and e['invisible'] and e['noAi'] for e in snapshot(side)['entities']))
record('Both clients receive the killer\'s hidden inert state', killer_id)
for effect in ['slowness', 'weakness', 'poison']:
    command('effect give ' + killer['uuid'] + ' minecraft:' + effect + ' 6 0 false')
wait_for(lambda: len(next(e for e in fish(server_state()) if e['id'] == killer_id)['effects']) == 3)
def effects_expired():
    state = server_state()
    actor = next(e for e in fish(state) if e['id'] == killer_id)
    if actor['effects'] or actor['effectParticles'] or not actor['invisible'] or actor['burrowUntil'] != deadline:
        return False
    for side in ['owner', 'guest']:
        actor = next(e for e in snapshot(side)['entities'] if e['id'] == killer_id)
        if actor['effectParticles'] or not actor['invisible']:
            return False
    return True
wait_for(effects_expired, timeout=30)
record('Six-second effects and their particles expire while hidden without extending the burrow deadline', deadline)
wait_for(lambda: server_state()['gameTime'] >= deadline - 40, timeout=90)
state = server_state()
assert state['gameTime'] < deadline and any(e['id'] == killer_id for e in fish(state)), state
wait_for(lambda: server_state()['gameTime'] >= deadline and not any(e['id'] == killer_id for e in fish(server_state())), timeout=15)
for side in ['owner', 'guest']:
    wait_for(lambda: not any(e['id'] == killer_id and e['alive'] and not e['removed'] for e in snapshot(side)['entities']))
final = server_state()
assert survivors.issubset({e['id'] for e in fish(final) if not e['burrowed'] and not e['killedZombie']}), final
record('The hidden killer despawns after thirty seconds on server and both clients while the rest keep roaming',
       dict(deadline=deadline, removedAt=final['gameTime'], remaining=fish(final), isolationEntitiesCleared=isolation_clears))
rpc('server', 'save')
