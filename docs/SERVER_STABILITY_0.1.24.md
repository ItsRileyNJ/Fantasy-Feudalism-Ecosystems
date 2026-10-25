# Ecosystems 0.1.24 — dedicated server and Mercenaries validation

Validated the exact release JAR on Minecraft 1.21.1 / NeoForge 21.1.262 / Java 21, with the supplied Mercenaries 1.15.7 JAR, a dedicated server, and two graphical clients. Both mods were installed on the server and clients.

## Results

- **211/211 required Ecosystems and integration GameTests passed** in a fresh world with Mercenaries installed. Build and the separate multiplayer harness also passed.
- **7/7 bat/net multiplayer scenarios passed**: competing captures conserve one bat; both owner and observer select the correct sprite; transfer clears the sprite on both clients; server restart preserves carried and housed bat identities; repeated full-enclosure insertions preserve the occupied net; both clients reload resources and render three roof-attached bats; all four original bat identities are conserved across two enclosures.
- **14/14 Mercenaries client scenarios passed**, covering competing contract redemption, spider alliances, simultaneous GuardScreens and shared storage, ammunition slot protection, synchronized orders, GUI scales 1–3, rejected items/potions without loss, private highlight packets, resource reloads, cursor items across logout, guard/player/spider persistence through another full server restart, harvest cooldown persistence, and real GUI horse mounting/dismounting on both clients.
- Both clients remained usable through the tested actions and restarts. Their inventory/model/guard state assertions agreed with the server. No renderer errors were recorded. The final release JAR is byte-identical to the previously delivered 0.1.24 artifact and excludes all test/probe classes and structures.

## Evidence

- [Bat/net two-client observations](server-stability-0.1.24-live-evidence.json)
- [Mercenaries two-client observations](server-stability-0.1.24-mercenaries-evidence.json)
- [Owner view](server-stability-0.1.24-owner.png) and [guest view](server-stability-0.1.24-guest.png)
- [Automated suite and artifact summary](server-stability-0.1.24-evidence.txt)

Reproduction: `tools/check_compatibility.py <Mercenaries.jar> --suite ecosystems`; `tools/multiplayer/server_stability_0_1_24.py prepare`, normal dedicated-server restart, then `verify`; and the existing `mercenaries.py` / `mercenaries_restart.py` live scripts.

The separate Mercenaries-owned native GameTest suite was not rerun. Its previously documented mounted-retreat failure and pre-start chunk-readiness stall remain outside these passing Ecosystems and live-client checks. Test identities used the existing disposable offline server because Minecraft authentication services were unavailable.

Release SHA-256: `0c46fc3f9df893623ff56d96db7f60bb2ce44ef9deaf977b997cc22efa45b054`.
Mercenaries SHA-256: `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.
