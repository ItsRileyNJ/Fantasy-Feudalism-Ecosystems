# Ecosystems with Mercenaries 1.15.7 after the enclosure display fix

This report records the first comparison after the enclosure fix. See the [follow-up deep audit](DEEP_AUDIT_CURRENT.md) for the latest targeting fixes, expanded tests, performance measurements and retained runner blockage. The subsequent [current multiplayer audit](MULTIPLAYER_AUDIT_CURRENT.md) records dedicated-server and two-client testing.

The supplied Mercenaries JAR was tested unchanged with the current Ecosystems sources, including the bat display fix. Minecraft 1.21.1, NeoForge 21.1.262, Java 21.0.13 and Gradle 9.2.1 were used. No new interaction failure was identified, but the compatibility check is **not fully passing**: one original mounted-navigation test also fails with Mercenaries alone.

## Binaries

- Mercenaries: SHA-256 `32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65`.
- Rebuilt Ecosystems: SHA-256 `b5b9b0dca63481fd6c85c33b477e798ab85faddd1188689c1ce64f4f3aaef827`.
- No overlapping asset/data file paths between the release JARs.
- No Mercenaries implementation or development test classes bundled in the Ecosystems release JAR. The separate multiplayer diagnostic JAR builds successfully.

## Executed checks

| Configuration | Required tests passed | Outcome |
| --- | --- | --- |
| Ecosystems alone | 156 / 156 | Build successful |
| Ecosystems namespace with Mercenaries | 168 / 168 | Build successful; also passed on the repeat through the new compatibility command |
| Mercenaries namespace with Ecosystems | 337 / 338 | One original mounted-retreat test failed; Gradle failed |
| Mercenaries alone, independent reference project | 335 / 336 | The same original mounted-retreat test failed; Gradle failed |

Each combined namespace used a separate fresh world. The reference project used only Mercenaries, NeoForge and Minecraft, with no Ecosystems classes, mixins or test replacements. Original assertions were not changed, suppressed or retried until passing.

[Recorded runner summaries, failure diagnostics and reference-project configuration](compatibility-current-evidence.txt) preserve the comparison. The full current-run logs remain under `run/compatibility-qz9y5xu1/`; the reference log is `/tmp/mercenaries-reference.log`.

Coverage includes companion alliances/friendly fire, skeleton weapon classification, projectiles, guard menu corrections, wildlife, bat occupancy/flight, recruitment, inventory safety, contracts, supplies, combat, orders, persistence and mounting. The Mercenaries-namespace total includes its 336 original cases plus the two existing supplemental mounted bow/crossbow cases; both supplemental cases passed.

## Reproduced original failure

`mountedcrossbowretreatrestartsacrossgrassystep` requires the horse's X coordinate to be strictly greater than the arena's X + 24 while mounted, unharmed and outside walls. Inspection of the supplied test's bytecode confirms the coordinate assertion. In the combined failure, the horse stopped at relative X approximately 23.977 with safe footing; in the Mercenaries-only failure it stopped at approximately 23.926. A safe retreat can end diagonally without satisfying this endpoint. This agrees with the previously recorded [mounted-retreat findings](RELEASE_0.1.16.md#findings-retained-and-limits).

This check still fails the compatibility command. The failure must not be reported as a pass or used to justify a test-specific navigation override. Correcting the original test requires a separate Mercenaries source change and a newly audited binary.

## Repeat before releasing changes

From the checkout root:

```sh
python tools/check_compatibility.py /absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar
```

The command verifies the exact supported Mercenaries SHA-256, selects the cloud Gradle helper when available (otherwise the repository wrapper), and runs both namespaces in fresh directories under ignored `run/compatibility-*`. It retains logs and worlds, propagates failed builds, requires the actual passing-test summaries with at least 174 and 338 required tests respectively (the expanded Ecosystems suite adds six audit cases), and rejects incomplete runs. A suite has a fifteen-minute timeout. `--suite ecosystems` or `--suite mercenaries` can isolate a diagnosis; release verification requires both suites. The wrong-JAR rejection path was verified with a deliberately invalid temporary file.

This command is a manual release check; ordinary GitHub CI currently tests Ecosystems alone. Keep the separately supplied Mercenaries binary outside source control. Do not update the accepted hash merely to allow an unreviewed version through.

## Limits

These new runs were headless GameTests. Graphical rendering (including visual confirmation of enclosure stability), two real networked clients, GUI interaction, server restart across a real client session, and online authentication were not revalidated. Earlier multiplayer evidence remains historical evidence for the earlier release binary. Authentication public-key lookup was unavailable in the cloud; it did not stop the headless suites. No Mixin application/injection failure or resource collision was found. These results do not establish perfect compatibility in every world or with other mods.
