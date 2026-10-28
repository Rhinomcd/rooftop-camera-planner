# Rooftop Lens verification map

The maintained recipes for proving Rooftop Lens behavior. Read [../SKILL.md](../SKILL.md) first for the two loops and the no-input rule.

## Baseline preconditions

- Replay: the repo builds, and `~/.claude/skills/test-box/box.sh run sh ./gradlew test` passes.
- Live: `lens.sh doctor` passes, the human is logged in on the dev client, and they're standing at a supported course (Rellekka is the one proven so far).

## Features

- [lap-tracking.md](lap-tracking.md): laps start, advance, complete, and drop.
- [calibrator-card.md](calibrator-card.md): the HUD card's viewfinder, status text, and stats.
- [click-spots.md](click-spots.md): obstacle badges and learned click spots fixed on screen.
- [view-scoring.md](view-scoring.md): views are saved, scored, and suggested.

## Feature entry contract

Each feature file has a title and one paragraph, then `Sub-features`, `How to get to it (user POV)`, `Driving it with lens.sh`, and `Gotchas`.
