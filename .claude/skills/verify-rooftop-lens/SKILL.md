---
name: verify-rooftop-lens
description: Verify Rooftop Lens, the RuneLite rooftop Agility plugin in this repo. Use to prove lap tracking, the calibrator card, or obstacle click spots work. Two loops - offline replay of recorded sessions through the real plugin (agent alone), and a live dev client a human plays while the agent observes. Never sends game input.
---

# Verify Rooftop Lens

Rooftop Lens runs inside RuneLite, and Jagex bans automated game input. So an agent never clicks, types, or moves the camera in the game client, not even through `ydotool`, `wtype`, or computer use. Only a human plays. Verification runs in two loops:

- **Replay (agent alone):** a recording of real play is fed through the real `RooftopLensPlugin`, with a mocked RuneLite `Client` standing in for the game (`src/test/java/com/rhinomcd/rooftoplens/Replay.java`). This covers lap logic, view scoring, and rendering of both overlays. Use it for every change.
- **Live (human plays, agent observes):** the dev client runs the plugin plus the dev-only `LensRecorderPlugin`. The human runs laps, and the agent reads the log and takes window screenshots. This is the only proof of in-game behavior, and each session leaves a recording that can become a replay fixture.

The helper `.claude/skills/verify-rooftop-lens/lens.sh` wraps every step below. Run it from anywhere. It keeps state in `/tmp/rooftop-lens-verify` (override with `LENS_RUN_DIR`).

## Launch

**Replay:** no launch needed. Run the tests through the test box:

```
~/.claude/skills/test-box/box.sh run sh ./gradlew test
```

`ReplayTest` replays every `src/test/resources/recordings/*.jsonl` file and checks the `expect` line at its top: laps learned, and the number of "Camera moved, lap dropped" notices.

**Live:** this needs a Jagex-account login saved by the launcher.

1. Check that `~/.runelite/credentials.properties` exists. If it doesn't, the human starts RuneLite once from the Jagex Launcher with the client argument `--insecure-write-credentials`. The launcher keeps its settings in `settings.json` in its working directory; on this machine that's under `/tmp/appimage_extracted_*`.
2. Run `.claude/skills/verify-rooftop-lens/lens.sh launch`. It runs `./gradlew run` detached and waits for `RooftopLensPlugin is now running` in the log. Readiness means the plugin is running, not that a window exists. The human still has to press Play to log in.
3. The dev client logs into the same account as the human's normal client. Ask the human to close their normal client on that account first, or one of the two gets kicked.

## Doctor

`lens.sh doctor` is read-only. It confirms the client PID this skill started is alive, the plugin is running, and the recorder file path. It also prints any `rooftoplens` errors. Run it whenever anything looks off.

## Drive

**Replay:**

- Run a recording and render a PNG frame after every obstacle click: `lens.sh replay [file.jsonl]`. The default is the newest recording. It prints `course= lapsLearned= views= best= next= calibrated= notices= frames=<dir>`, and the frames land in `build/replay/<name>/NNN.png`.
- Turn a live session into a regression fixture: `lens.sh promote <recording> <name> <expected laps> <expected drops> "<note>"`. Write the expectation from what the human saw happen, then run the tests.

**Live:** ask the human to run laps. Say exactly what to do, for example "2 laps at Rellekka without touching the camera". Then observe:

- `lens.sh laps` lists `Lap learned on RELLEKKA in view Y:P:Z` and `Lap dropped on RELLEKKA: started y/p/z now y/p/z` lines. A drop line shows exactly which camera axis moved.
- `lens.sh screenshot` asks the dev-only recorder for the next rendered game frame through RuneLite's `DrawManager`, then copies it into evidence. Overlapping windows can't spoil it, but it only fires on a game tick, so the human must be logged in.
- The human can hand you screenshots directly. Treat what they report as evidence, and get the log line that matches it.

## Evidence

- **Replay:** the test result, plus the `lens.sh replay` summary line and the frames in `build/replay/<name>/`.
- **Live:** `lens.sh laps` output and screenshots in `/tmp/rooftop-lens-verify/evidence/`. Saved views are in `~/.runelite/profiles2/*.properties` under `rooftopLens.views.<COURSE>`.
- **Standard:** a behavior change is proven when a replay fixture covers it and a live session shows it once. If you couldn't get a live session, say so; replay alone doesn't prove the in-game camera and clickbox values.

## Cleanup

- `lens.sh stop` kills only the client PID this skill started. Never `pkill -f` a pattern: the pattern also matches the shell running the command, and it kills that shell.
- Evidence and recordings stay. Recordings live in `~/.runelite/rooftop-lens/recordings/`.
- Tell the human to delete `~/.runelite/credentials.properties` when they're done. It holds their session in plain text.

## Gotchas

- **Old Gson:** RuneLite bundles an old Gson, so test code uses `new Gson().fromJson(text, JsonObject.class)`, not `JsonParser.parseString`.
- **Two copies:** the Plugin Hub copy of the original plugin, `rooftop-camera-planner`, is a different plugin and doesn't conflict. A Plugin Hub copy of Rooftop Lens itself is skipped automatically while it's loaded as a builtin.
- **Gradle lock:** a running dev client holds Gradle's cache lock. Run tests from a copy (`rsync` the repo to `/tmp`), with `GRADLE_USER_HOME=/tmp/gh-rooftop`, or stop the client first.
- **Unrelated errors:** other Plugin Hub plugins may fail to load in the dev client because it's a different RuneLite version. Those errors aren't Rooftop Lens's.

Feature recipes are in [features/README.md](features/README.md).
