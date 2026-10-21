"""Observe actual vanilla effect particles from successful companion hits on two clients."""
from control import *

report = TEST_ROOT / 'spider-effect-particles-results.json'
checks = []

def record(name, detail):
    checks.append(dict(name=name, status='passed', detail=detail))
    report.write_text(json.dumps(checks, indent=2))
    print('PASS', name, flush=True)

def entity(side, identity):
    return next(e for e in snapshot(side)['entities'] if e['id'] == identity)

rpc('server', 'snapshot', timeout=600)
for side in ['owner', 'guest']:
    if not rpc(side, 'snapshot', timeout=600)['connected']:
        rpc(side, 'connect')
wait_for(lambda: len(snapshot('server')['players']) == 2, timeout=90)
rpc('server', 'setup', timeout=60)
rpc('server', 'command', command='gamemode creative @a')
for name, x in [('FFMOwner', -1), ('FFMGuest', 1)]:
    rpc('server', 'teleport', player=name, x=x, y=64, z=0, yaw=0, pitch=12)
    rpc('server', 'inventory', player=name, slots=[])
rpc('server', 'clear')

for kind in ['spider', 'cave_spider']:
    pet = rpc('server', 'spawn', type='minecraft:' + kind, x=-2, y=64, z=5, owner='FFMOwner')
    victim = rpc('server', 'spawn', type='minecraft:pillager', x=0, y=64, z=5)
    for side in ['owner', 'guest']:
        wait_for(lambda: any(e['id'] == victim['id'] for e in snapshot(side)['entities']))
        rpc(side, 'watchEffectParticles', entity=victim['id'])
    hit = rpc('server', 'hit', entity=pet['id'], target=victim['id'])
    assert hit['successful'], hit
    effects = hit['target']['effectDetails']
    expected = {'minecraft:slowness', 'minecraft:weakness'}
    if kind == 'cave_spider':
        expected.add('minecraft:poison')
    assert {e['effect'] for e in effects} == expected, effects
    assert all(e['duration'] == 120 and e['amplifier'] == 0 and e['visible']
               and not e['ambient'] for e in effects), effects
    colors = {e['particle']['rgb'] for e in effects}
    assert len(colors) == len(expected), effects
    assert all(e['particle']['type'] == 'minecraft:entity_effect' and e['particle']['alpha'] > 0
               for e in effects), effects

    def synced():
        for side in ['server', 'owner', 'guest']:
            particles = entity(side, victim['id'])['effectParticles']
            if {p.get('rgb') for p in particles} != colors:
                return False
        return True
    wait_for(synced)
    record(kind + ': visible level I six-second effects and correct vanilla particle colors sync to both clients', effects)

    def emitted():
        for side in ['owner', 'guest']:
            observed = rpc(side, 'effectParticleProbe')
            assert not observed['error'], observed
            if not colors.issubset({int(c) for c in observed['colors']}):
                return False
        return True
    wait_for(emitted, timeout=30)
    observations = {side: rpc(side, 'effectParticleProbe') for side in ['owner', 'guest']}
    record(kind + ': both clients actually create visible particles of every applied effect color at the enemy', observations)
    for side in ['owner', 'guest']:
        rpc(side, 'screenshot', file='effect-particles-' + kind + '-' + side + '.png')

    wait_for(lambda: all(not entity(side, victim['id'])['effectParticles']
                         and not entity(side, victim['id'])['effects']
                         for side in ['server', 'owner', 'guest']), timeout=30)
    # Let existing particles finish their vanilla lifetime; expired effects must not keep emitting.
    for side in ['owner', 'guest']:
        rpc(side, 'watchEffectParticles', entity=victim['id'])
    wait_for(lambda: all(rpc(side, 'effectParticleProbe')['ticks'] >= 45 for side in ['owner', 'guest']))
    for side in ['owner', 'guest']:
        rpc(side, 'watchEffectParticles', entity=victim['id'])
    wait_for(lambda: all(rpc(side, 'effectParticleProbe')['ticks'] >= 25 for side in ['owner', 'guest']))
    quiet = {side: rpc(side, 'effectParticleProbe') for side in ['owner', 'guest']}
    assert all(not p['error'] and not p['colors'] for p in quiet.values()), quiet
    record(kind + ': effect metadata clears and particle emission stops after expiry on both clients', quiet)
    rpc('server', 'remove', entity=pet['id'])
    rpc('server', 'remove', entity=victim['id'])
