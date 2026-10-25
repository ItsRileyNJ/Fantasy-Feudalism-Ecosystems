# Ecosystems 0.1.24 — occupied Bat Net artwork

The occupied Bat Net now uses a separate flat sprite based on the supplied reference: a wooden hoop and handle, dense ivory/gray mesh, and a brown bat visible inside the hanging bag. The original empty-net artwork is preserved byte-for-byte.

The existing `fantasy_feudalism_mobs:occupied` item predicate selects the occupied model whenever captured-bat data is present. A successful transfer into an enclosure clears that data and restores the original empty sprite. This reuses existing capture and transfer mechanics, with no extra polling or custom 3D item model.

The new reference-based artwork is a transparent 64×64 PNG. Both models use the native handheld sprite parent; no custom cuboids are added.

## Validation

- `build multiplayerTestJar` passed; Python live-test compilation and `git diff --check` passed.
- A dedicated server and graphical client ran the release with Mercenaries 1.15.7. Native client interactions captured a real bat and transferred the same UUID into an enclosure.
- Measured the native client item model sequence: original empty sprite → new occupied sprite after capture → original empty sprite after transfer. All three used normal sprite rendering.
- The occupied sprite persisted through resource reload and disconnect/reconnect. No renderer errors occurred, and the enclosure held exactly the original captured bat afterward.
- Verified the original empty PNG is unchanged, the occupied PNG has transparency, the release contains the correct model and texture, and test/probe classes and structures are excluded.

Evidence: [native client model observations](occupied-net-0.1.24-live-evidence.json), [occupied screenshot](occupied-net-0.1.24-occupied.png), [empty-again screenshot](occupied-net-0.1.24-empty.png). Reproduce in the disposable test environment with `tools/multiplayer/occupied_net_0_1_24.py`.

Follow-up multiplayer validation reran **211/211 Ecosystems/integration GameTests with Mercenaries 1.15.7**, plus **7/7 bat/net two-client scenarios** and **14/14 Mercenaries two-client scenarios**, including two full dedicated-server restarts. Capture races, observer sprites, full-enclosure failures, item identity conservation, guard inventories/orders, persistence, resource reloads, and horse mounting all passed. See the [server stability report](SERVER_STABILITY_0.1.24.md) for evidence and scope. The separate native Mercenaries-suite limitations remain documented there.

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.24.jar` (345398 bytes), SHA-256 `0c46fc3f9df893623ff56d96db7f60bb2ce44ef9deaf977b997cc22efa45b054`.
