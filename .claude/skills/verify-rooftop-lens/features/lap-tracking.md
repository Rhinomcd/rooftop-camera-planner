# Lap tracking

A lap starts when the player clicks the course's first obstacle and advances through each next obstacle in order. It completes on the last obstacle and is saved under the camera view it started in. If the camera leaves that view, the lap drops.

## Sub-features

- `lap-start`: clicking obstacle 1 starts a lap. The card shows "Lap 1/N".
- `lap-advance`: each expected obstacle advances the lap. Misclicks and re-clicks are ignored.
- `lap-complete`: the last obstacle saves the lap and shows "Lap learned".
- `lap-drop`: leaving the starting view shows "Camera moved, lap dropped". The next lap starts at obstacle 1.
- `pitch-snap`: expanded pitch the game pulls back to 383 by itself doesn't drop the lap.

## How to get to it (user POV)

- Run a rooftop course from the first obstacle while the plugin is enabled.

## Driving it with lens.sh

- **Replay:** run the tests. `pitch-snap-rellekka.jsonl` must learn 2 laps with 0 drops, and `camera-turn-drops-lap.jsonl` must learn 0 laps with 1 drop.
- **Live:** ask the human for 2 laps without touching the camera, then 1 lap where they rotate the camera mid-lap. `lens.sh laps` must show 2 `Lap learned` lines, then 1 `Lap dropped` line where the yaw changed.
- **Proof:** the test result, the `lens.sh laps` output, and a screenshot of the card right after "Lap learned".

## Gotchas

- The game moves the camera's pitch on its own when expanded pitch is in use. A drop line where only the pitch changed to 383 means the cap regressed.
- Tabbing out of the window doesn't move the camera. A drop that happens at the moment of tab-out is worth a recording.
