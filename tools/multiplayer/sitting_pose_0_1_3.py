"""Verify sitting abdomen pitch on two clients in the disposable local fixture."""
from control import *
import math

checks = []
report = TEST_ROOT / 'sitting-pose-0.1.3-results.json'

def entity(state, identity):
    return next(e for e in state['entities'] if e['id'] == identity)

def teleport(x, z):
    rpc('server', 'teleport', player='FFMOwner', x=x, y=64, z=z, yaw=0, pitch=18)

def record(name):
    checks.append(dict(name=name, status='passed'))
    report.write_text(json.dumps(checks, indent=2))
    print('PASS', name, flush=True)

rpc('server', 'snapshot', timeout=240)
for side in ['owner', 'guest']:
    if not rpc(side, 'snapshot', timeout=240)['connected']:
        rpc(side, 'connect')
wait_for(lambda: len(snapshot('server')['players']) == 2, timeout=60)
rpc('server', 'setup', timeout=60)
rpc('server', 'command', command='gamemode creative @a')
rpc('server', 'command', command='effect give @a minecraft:night_vision infinite 0 true')
teleport(0, 0)
rpc('server', 'teleport', player='FFMGuest', x=2, y=64, z=0, yaw=20, pitch=18)
rpc('server', 'clear')
rpc('server', 'inventory', player='FFMOwner', slots=[])
spiders = []
for index, (kind, tamed) in enumerate([
        ('spider', False), ('spider', True), ('spider', True),
        ('cave_spider', False), ('cave_spider', True), ('cave_spider', True)]):
    options = dict(type='minecraft:' + kind, x=-5 + index * 2, y=64, z=5)
    if tamed:
        options['owner'] = 'FFMOwner'
    mob = rpc('server', 'spawn', **options)
    wait_for(lambda: all(any(e['id'] == mob['id'] for e in snapshot(side)['entities'])
                         for side in ['owner', 'guest']))
    rpc('server', 'command', command=f"tp {mob['uuid']} {options['x']} 64 5 90 0")
    spiders.append((mob, tamed))

def correct_poses():
    server = snapshot('server')
    for side in ['owner', 'guest']:
        state = snapshot(side)
        if state['renderErrors']:
            raise AssertionError(state['renderErrors'])
        for mob, tamed in spiders:
            sitting = entity(server, mob['id'])['sitting']
            if entity(state, mob['id'])['sitting'] != sitting:
                return False
            pose = state['spiderPoses'].get(str(mob['id']))
            if not pose or pose['sitting'] != sitting:
                return False
            if not math.isclose(pose['abdomenY'], pose['restY'], abs_tol=1e-6):
                return False
            expected = pose['restXRot'] - (0.18 if tamed and sitting else 0)
            if not math.isclose(pose['abdomenXRot'], expected, abs_tol=1e-6):
                return False
    return True

wait_for(correct_poses)
# Keep one following and one sitting companion per type in the same rendered scene.
for index in [2, 5]:
    mob, _ = spiders[index]
    teleport(mob['position'][0], 3)
    rpc('owner', 'interact', entity=mob['id'])
    wait_for(lambda: entity(snapshot('server'), mob['id'])['sitting'])
teleport(0, 0)
wait_for(correct_poses)
record('Both clients render sitting regular/cave abdomens at -0.18 radians without a vertical offset; wild/following models remain neutral')
rpc('owner', 'screenshot', file='sitting-pose-0.1.3-owner.png')
rpc('guest', 'screenshot', file='sitting-pose-0.1.3-guest.png')

for index in [2, 5]:
    mob, _ = spiders[index]
    teleport(mob['position'][0], 3)
    for expected in [False, True, False, True]:
        rpc('owner', 'interact', entity=mob['id'])
        wait_for(lambda: entity(snapshot('server'), mob['id'])['sitting'] == expected)
        wait_for(correct_poses)
teleport(0, 0)
record('Repeated real sit/follow interactions restore the normal angle without accumulated rotation or shared-model leakage')
for side in ['owner', 'guest']:
    rpc(side, 'reload', timeout=60)
wait_for(correct_poses)
record('Resource reload preserves correct poses on both connected clients')
for side in ['owner', 'guest']:
    (TEST_ROOT / f'sitting-pose-0.1.3-{side}-snapshot.json').write_text(json.dumps(snapshot(side), indent=2))
