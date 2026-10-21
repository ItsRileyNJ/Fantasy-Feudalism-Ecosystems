"""Real movement, falls and bee feeding in the disposable Mobs-only fixture."""
from control import *

REPORT = TEST_ROOT / 'chase-spiders-0.1.2-results.json'
checks = []
def record(name, detail):
    checks.append(dict(name=name, status='passed', detail=detail))
    REPORT.write_text(json.dumps(checks, indent=2))
    summary = {k: v for k, v in detail.items() if k != 'trace'} if isinstance(detail, dict) else detail
    print('PASS', name, json.dumps(summary), flush=True)
def command(text):
    rpc('server', 'command', command=text)
def entity(identity):
    return next(e for e in snapshot('server')['entities'] if e['id'] == identity)
def teleport(name, x, z, yaw=0):
    rpc('server', 'teleport', player=name, x=x, y=64, z=z, yaw=yaw, pitch=15)

command('gamerule doMobSpawning false')
rpc('server', 'clear')
teleport('FFMOwner', 8, 10)
teleport('FFMGuest', 12, 0, yaw=10)
command('forceload add -16 -16 48 48')
ready = 'FFM_CHASE_CHUNKS_' + str(time.monotonic_ns())
def chunks_loaded():
    command('execute if loaded 48 64 48 run say ' + ready)
    return ready in (TEST_ROOT / 'server.log').read_text()
wait_for(chunks_loaded)
command('fill -12 63 21 40 63 40 grass_block')
command('fill -12 64 21 40 70 40 air')
ready_floor = 'FFM_CHASE_FLOOR_' + str(time.monotonic_ns())
command('execute if block 8 63 30 minecraft:grass_block run say ' + ready_floor)
wait_for(lambda: ready_floor in (TEST_ROOT / 'server.log').read_text())
command('gamemode creative @a')
command('effect give FFMOwner minecraft:resistance infinite 4 true')
teleport('FFMOwner', 8, 10)
teleport('FFMGuest', 12, 0, yaw=10)
command('gamemode survival FFMOwner')
owner_id = player(snapshot('server'), 'FFMOwner')['id']
zombie = rpc('server', 'spawn', type='minecraft:zombie', x=8, z=0, invulnerable=True)
rpc('server', 'target', entity=zombie['id'], target=owner_id)
# Vanilla melee goals may wait up to 20 ticks for their initial acquisition.
# Measure established pursuit, rather than that startup delay.
wait_for(lambda: entity(zombie['id'])['position'][2] > zombie['position'][2] + 0.3)
start = snapshot('server')
start_zombie = next(e for e in start['entities'] if e['id'] == zombie['id'])
trace = []
try:
    rpc('owner', 'walk', value=True)
    while True:
        state = snapshot('server')
        mob = next(e for e in state['entities'] if e['id'] == zombie['id'])
        user = player(state, 'FFMOwner')
        trace.append(dict(tick=state['tick'], zombie=mob['position'], player=user['position'], target=mob['target']))
        if state['tick'] >= start['tick'] + 60:
            break
finally:
    rpc('owner', 'walk', value=False)
    command('gamemode creative FFMOwner')
advance = trace[-1]['zombie'][2] - start_zombie['position'][2]
player_advance = trace[-1]['player'][2] - player(start, 'FFMOwner')['position'][2]
(TEST_ROOT / 'zombie-chase-0.1.2-trace.json').write_text(json.dumps(trace, indent=2))
assert all(abs(row['player'][1] - 64) < 0.1 for row in trace), 'The walking player must stay on the flat test floor'
assert player_advance > 6, ('Client movement keys must produce real walking packets', player_advance)
assert advance > 3, ('Zombie must advance while the real player walks away, including the approved 80% speed variants', advance)
assert all(row['target'] == owner_id for row in trace), 'Zombie must retain the valid moving player'
idle = longest_idle = 0
for previous, current in zip(trace, trace[1:]):
    movement = sum((current['zombie'][i] - previous['zombie'][i]) ** 2 for i in [0, 2])
    idle = idle + current['tick'] - previous['tick'] if movement < 0.00001 else 0
    longest_idle = max(longest_idle, idle)
assert longest_idle < 10, ('Flat-ground pursuit must not stall between routes', longest_idle)
record('Zombie chases an actually walking client on flat ground without Mercenaries',
       dict(ticks=trace[-1]['tick'] - trace[0]['tick'], zombieAdvance=advance, playerAdvance=player_advance, longestIdleTicks=longest_idle, trace=trace))

rpc('server', 'clear')
teleport('FFMOwner', 0, 0)
teleport('FFMGuest', 2, 0, yaw=15)
spider = rpc('server', 'spawn', type='minecraft:spider', x=0, z=4)
bee = rpc('server', 'spawn', type='minecraft:bee', x=0, z=7)
rpc('server', 'health', entity=spider['id'], health=3)
rpc('server', 'health', entity=bee['id'], health=1)
rpc('server', 'target', entity=spider['id'], target=bee['id'])
wait_for(lambda: entity(spider['id'])['health'] == spider['maxHealth'])
for side in ['owner', 'guest']:
    wait_for(lambda: next(e for e in snapshot(side)['entities'] if e['id'] == spider['id'])['health'] == spider['maxHealth'])
record('Actual wild spider AI kills a bee and its full healing reaches both clients', dict(health=entity(spider['id'])['health']))

# The normal-world fixture can have terrain above the original seven-block-high
# test room. Clear the fall volume so climbing/collisions cannot shorten a drop.
command('fill -4 64 0 14 80 14 air')
for drop in [8.5, 9.5]:
    rpc('server', 'clear')
    target = rpc('server', 'spawn', type='minecraft:iron_golem', x=4, z=12)
    outcomes = []
    for i, (kind, tamed) in enumerate([('spider', False), ('spider', True), ('cave_spider', False), ('cave_spider', True)]):
        options = dict(type='minecraft:' + kind, x=i * 3, y=64 + drop, z=4)
        if tamed:
            options['owner'] = 'FFMOwner'
        mob = rpc('server', 'spawn', **options)
        rpc('server', 'target', entity=mob['id'], target=target['id'])
        wait_for(lambda: entity(mob['id'])['position'][1] <= 64.05, timeout=10)
        landed = entity(mob['id'])
        assert (landed['health'] == mob['maxHealth']) if drop < 9 else (0 < landed['health'] < mob['maxHealth'])
        assert landed['target'] == target['id'], 'Surviving the fall must retain the target'
        outcomes.append(dict(type=kind, tamed=tamed, health=landed['health'], maxHealth=mob['maxHealth'], target=landed['target']))
    record(str(drop) + '-block actual falls preserve target and apply the correct damage threshold', outcomes)
command('gamemode creative @a')
