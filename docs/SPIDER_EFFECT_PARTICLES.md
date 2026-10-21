# Companion effect particle verification

The released Mobs **0.1.3** already enables vanilla particles on companion-applied effects. `MobRules.afterHit` uses vanilla `MobEffectInstance` constructors with particle visibility enabled. No gameplay change or replacement JAR was necessary.

The same packaged 0.1.3 release ran on a dedicated server and two graphical clients with Mercenaries 1.15.7 enabled. Release SHA-256: `688054213e2d2ab85d31fb1947dac3a3b686fa4fd405d09257159ea26186fd5e`.

All **six focused checks passed**, three each for regular and cave companions:

- A successful vanilla melee hit applied exactly the approved visible, nonambient level I effects for 120 ticks. The matching vanilla `minecraft:entity_effect` particle options synchronized to both clients.
- A read-only observer found actual visible `SpellParticle` instances of every applied effect color around the affected enemy in both clients' particle engines. Screenshots were also inspected.
- After expiry, both clients cleared the effect particle metadata. Existing particles were allowed to finish their vanilla lifetime; subsequent observation recorded no further particles around the enemy.

Regular companions showed Slowness and Weakness particles; cave companions additionally showed Poison particles. The client observer only reads particle queues and never creates, recolors, or suppresses particles. The server fixture invokes the spider's real `doHurtTarget` method against a pillager and verifies that the hit succeeds; it does not apply effects with commands.

Evidence: [all results and particle counts](effect-particle-evidence/results.json), [regular spider](effect-particle-evidence/effect-particles-spider-owner.png), and [cave spider](effect-particle-evidence/effect-particles-cave_spider-owner.png).

The development-only diagnostics build with `multiplayerTestJar`; `tools/multiplayer/spider_effect_particles.py` runs against the disposable local fixture described in [the validation report](VALIDATION.md). The diagnostics are excluded from the released mod. Normal vanilla particle settings and rendering rules remain respected. These checks used loopback networking and Linux Mesa software rendering; they do not establish behavior for every graphics driver, resource pack, or additional mod.
