# 0.1.18 — Field Mouse sounds

Uses the user's uploaded `Mouse Sounds.zip`: **five ambient calls** and **three hurt clips**. The original mono, 44.1 kHz Ogg Vorbis recordings are packaged byte-for-byte unchanged. All eight decoded successfully with FFmpeg. The two sound banks choose among their variants with equal default weights.

Field Mice use vanilla Mob's random ambient scheduler with the **120-tick minimum interval used by Animal**, instead of PathfinderMob's default 80 ticks. This is ordinary randomized animal timing, not a fixed call every six seconds. Normal damage plays the hurt bank; fatal damage reuses the same bank because no separate death clips were supplied. Native volume/pitch, positional sound, Friendly Creatures category and `Silent` handling remain in use. Subtitles identify mouse calls and hurt sounds.

## Validation

- **176/176 standalone required GameTests passed**, with a successful release build.
- **189/189 combined Ecosystems/integration tests passed** with the unchanged Mercenaries 1.15.7 JAR.
- The new native server test observes outgoing sound packets to a nearby player for randomized ambient scheduling, nonfatal damage and fatal damage; checks cooldown parity with a vanilla cow; and verifies that `Silent` suppresses both banks.
- Both packaged sound banks resolve to their eight files. Packaged audio equals the uploaded audio byte-for-byte. No development tests or diagnostic harness classes are bundled.
- Physical listening was not performed: this cloud environment lacks a working audio device. File decoding and server sound dispatch were verified; this change does not claim a new graphical-client listening test.

The combined count covers Ecosystems and its integration cases, not Mercenaries' separate native test suite. Earlier independent Mercenaries runner findings remain documented in the previous audits.

Release: `build/libs/fantasy_feudalism_ecosystems-0.1.18.jar`, **313,216 bytes**. SHA-256: `8613dd5bd22d2a79cc605244fc699a021f0e4966f1d65710a3230f895f1b70e5`.

[Source audio checksums and recorded test results](mouse-sounds-0.1.18-evidence.txt).

## Reproduce

```sh
python /workspace/toolchains/gradle.py runGameTestServer build -PgameTestDirectory=run/new-mouse-sounds
python tools/check_compatibility.py /absolute/path/fantasyfeudalism-mercenaries-1.15.7.jar --suite ecosystems
```

Use a fresh GameTest directory. Outside the cloud workspace, use the repository Gradle wrapper with Java 21.
