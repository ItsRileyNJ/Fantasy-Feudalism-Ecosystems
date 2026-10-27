# Version 0.1.28 — Less sheep sway

Reduced cold sheep's sideways head/body motion and roll by 75%. The fast head yaw, leg tremble and native freezing shake retain their previous strength and speed. This keeps the shivering while reducing the rocking motion.

The change only adjusts client-side animation amplitudes. Cold detection, the 30-second warm-up, movement speed, grazing, and wool regrowth retain their existing behavior. Shared model offsets still reset before every sheep render.

## Validation

Release and optional multiplayer test-harness builds passed. All six native two-client checks passed with Mercenaries 1.15.7 installed, verifying reduced head/body motion within the new bounds, cold/rain exposure, warm model reset, recovery and resource reload behavior. Results are recorded in [the live evidence](sheep-sway-0.1.28-live-evidence.json). The full server GameTest suite was not rerun for this animation-only adjustment; 232 tests passed for the preceding shivering fix.

Artifact: `build/libs/fantasy_feudalism_ecosystems-0.1.28.jar`

SHA-256: `57d0557be13fb5ec40b66797de4582ce57ee1b96a8790ffe9823b991537632d5`
