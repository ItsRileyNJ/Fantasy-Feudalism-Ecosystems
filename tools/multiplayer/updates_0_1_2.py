"""Two-client checks for 0.1.2 in a disposable Mobs-only test world."""
from control import *
import math

REPORT = TEST_ROOT / 'updates-0.1.2-results.json'
checks = []
def record(name, detail=None):
    checks.append(dict(name=name, status='passed', detail=detail))
    REPORT.write_text(json.dumps(checks, indent=2))
    print('PASS', name, flush=True)
def entity(state, identity):
    return next(e for e in state['entities'] if e['id'] == identity)
def command(text):
    return rpc('server', 'command', command=text)
def tracked(identity):
    wait_for(lambda: all(any(e['id'] == identity for e in snapshot(side).get('entities', [])) for side in ['owner', 'guest']))
def teleport(name, x, z, yaw=0, pitch=22, y=64):
    rpc('server', 'teleport', player=name, x=x, y=y, z=z, yaw=yaw, pitch=pitch)

for side in ['owner', 'guest']:
    if not rpc(side, 'snapshot', timeout=90)['connected']:
        rpc(side, 'connect')
wait_for(lambda: len(snapshot('server')['players']) == 2, timeout=60)
command('gamemode creative @a')
command('effect give @a minecraft:night_vision infinite 0 true')
rpc('server', 'clear')
for side, name in [('owner', 'FFMOwner'), ('guest', 'FFMGuest')]:
    rpc('server', 'inventory', player=name, slots=[])
    rpc(side, 'guiScale', scale=2)
    rpc(side, 'inventory')
    for _ in range(3):
        state = snapshot(side)
        tab = next((t for t in state.get('creativeTabs', []) if t['title'] == 'Fantasy Feudal Mobs'), None)
        if tab:
            break
        rpc(side, 'mouse', x=407, y=72)
    assert tab, 'Mobs creative tab missing'
    assert [i['item'] for i in tab['items']] == ['fantasy_feudalism_mobs:bee_bottle']
    rpc(side, 'mouse', x=222 + 27 * tab['column'] + 13, y=96 if tab['top'] else 264)
    assert snapshot(side)['creativeSlots'][0]['item'] == 'fantasy_feudalism_mobs:bee_bottle'
    rpc(side, 'screenshot', file='updates-creative-' + side + '.png')
    rpc(side, 'mouse', x=239, y=137)  # Pick the item from the actual creative screen.
    rpc(side, 'mouse', x=239, y=233)  # Place it in the first hotbar slot.
    wait_for(lambda: count(player(snapshot('server'), name), 'fantasy_feudalism_mobs:bee_bottle') == 1)
    wait_for(lambda: snapshot(side)['player']['inventory'] == player(snapshot('server'), name)['inventory'])
    assert not snapshot(side)['beeBottleModelMissing']
    rpc(side, 'close')
record('Creative tab and Bee Bottle transfer agree with the server on both clients')

teleport('FFMOwner', 0, 0)
teleport('FFMGuest', 2, 0, yaw=20)
spiders = []
for index, (kind, tamed) in enumerate([('spider', False), ('spider', True), ('cave_spider', False), ('cave_spider', True)]):
    options = dict(type='minecraft:' + kind, x=-3 + index * 2, z=4)
    if tamed:
        options['owner'] = 'FFMOwner'
    mob = rpc('server', 'spawn', **options)
    tracked(mob['id'])
    spiders.append((mob, kind, tamed))
# The fixture spawns companions following; order them to sit through real interactions.
rpc('server', 'inventory', player='FFMOwner', slots=[])
for mob, kind, tamed in spiders:
    if tamed:
        teleport('FFMOwner', mob['position'][0], 2)
        rpc('owner', 'interact', entity=mob['id'])
        wait_for(lambda: entity(snapshot('server'), mob['id'])['sitting'])
teleport('FFMOwner', 0, 0)
def correct_spider_poses():
    for side in ['owner', 'guest']:
        state = snapshot(side)
        for mob, kind, tamed in spiders:
            actual = entity(state, mob['id'])
            texture = ('fantasy_feudalism_mobs:textures/entity/spider/tamed_' if tamed else 'minecraft:textures/entity/spider/') + kind + '.png'
            if actual['texture'] != texture or not actual['texturePresent']:
                return False
            pose = state['spiderPoses'].get(str(mob['id']))
            if not pose or pose['sitting'] != actual['sitting']:
                return False
            if pose['abdomenY'] != pose['restY'] + (2 if actual['sitting'] else 0):
                return False
    return True
wait_for(correct_spider_poses)
rpc('owner', 'screenshot', file='updates-spider-sitting.png')
for mob, kind, tamed in spiders:
    if not tamed:
        continue
    rpc('server', 'inventory', player='FFMOwner', slots=[])
    teleport('FFMOwner', mob['position'][0], 2)
    for expected in [False, True, False, True]:
        rpc('owner', 'interact', entity=mob['id'])
        wait_for(lambda: entity(snapshot('server'), mob['id'])['sitting'] == expected)
        wait_for(correct_spider_poses)
teleport('FFMOwner', 0, 0)
rpc('owner', 'screenshot', file='updates-spider-textures.png')
record('Both clients render wild/tamed textures and reversible sitting poses without model-state leakage')
for side in ['owner', 'guest']:
    rpc(side, 'reload', timeout=60)
wait_for(correct_spider_poses)
record('Resource reload preserves both clients\' spider textures and poses')

rpc('server', 'clear')
teleport('FFMOwner', 0, -2, pitch=12)
teleport('FFMGuest', 2, -2, yaw=15, pitch=12)
skeletons = []
for index, kind in enumerate(['skeleton', 'stray', 'bogged', 'wither_skeleton']):
    mob = rpc('server', 'spawn', type='minecraft:' + kind, x=-3 + index * 2, z=4, weapon='minecraft:crossbow', invulnerable=True)
    command('tp ' + mob['uuid'] + ' ' + str(-3 + index * 2) + ' 64 4 180 0')
    tracked(mob['id'])
    skeletons.append(mob)
def holds():
    for side in ['owner', 'guest']:
        poses = snapshot(side)['crossbowPoses']
        for mob in skeletons:
            pose = poses.get(str(mob['id']))
            if not pose or pose['rightX'] >= -0.7 or pose['leftX'] >= -0.7:
                return False
    return True
wait_for(holds)
rpc('owner', 'screenshot', file='updates-crossbow-idle.png')
record('Idle crossbow skeleton, stray, bogged and wither skeleton use two-handed holds on both clients')
target = rpc('server', 'spawn', type='minecraft:iron_golem', x=-3, z=12)
rpc('server', 'target', entity=skeletons[0]['id'], target=target['id'])
wait_for(lambda: all(snapshot(side)['crossbowPoses'].get(str(skeletons[0]['id']), {}).get('usingItem') for side in ['owner', 'guest']))
record('Crossbow charge animation reaches both clients')

rpc('server', 'clear')
teleport('FFMOwner', 0, 0)
teleport('FFMGuest', 2, 0)
command('effect give FFMOwner minecraft:resistance infinite 4 true')
owner_id = player(snapshot('server'), 'FFMOwner')['id']
for weapon in ['bow', 'crossbow']:
    for kind in ['skeleton', 'stray', 'bogged', 'wither_skeleton']:
        mob = rpc('server', 'spawn', type='minecraft:' + kind, x=0, z=8, weapon='minecraft:' + weapon, invulnerable=True)
        for mode in ['creative', 'spectator']:
            command('gamemode survival FFMOwner')
            rpc('server', 'target', entity=mob['id'], target=owner_id)
            wait_for(lambda: entity(snapshot('server'), mob['id'])['target'] == owner_id)
            command('gamemode ' + mode + ' FFMOwner')
            wait_for(lambda: entity(snapshot('server'), mob['id'])['target'] == -1)
        rpc('server', 'remove', entity=mob['id'])
    record(weapon + ' skeleton family releases real players switching to Creative and Spectator')
command('gamemode creative @a')
for side in ['owner', 'guest']:
    assert not snapshot(side)['renderErrors']
record('Both clients remain connected with finite rendered poses and no missing Bee Bottle model')
