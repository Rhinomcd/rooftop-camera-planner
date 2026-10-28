# Lap tracking

A lap starts when the player clicks the course's first obstacle. A click on the next obstacle counts once the previous obstacle's Agility XP has landed; a click before that is held, then confirmed by the next XP drop if the game took it, or replaced by the next click if it didn't. The lap completes on the last obstacle's XP and is saved under the camera view it started in. If the camera leaves that view before the last click, the lap drops.

## Sub-features

- `lap-start`: clicking obstacle 1 starts a lap. The card shows "Lap 1/N".
- `lap-advance`: each expected obstacle advances the lap once the previous obstacle's XP lands. Misclicks and re-clicks are ignored.
- `early-click`: a click on the next obstacle during the previous one's animation doesn't advance the card.
- `lap-complete`: the last obstacle's XP saves the lap and shows "Lap learned". Turning the camera after the last click is fine.
- `lap-drop`: leaving the starting view shows "Camera moved, lap dropped". The next lap starts at obstacle 1.
- `pitch-snap`: expanded pitch the game pulls back to 383 by itself doesn't drop the lap.

## How to get to it (user POV)

- Run a rooftop course from the first obstacle while the plugin is enabled.

## Driving it with lens.sh

- **Replay:** run the tests. Recordings with `xp` lines replay XP as recorded; older ones replay as if every click succeeded. `pitch-snap-rellekka.jsonl` must learn 2 laps with 0 drops, and `camera-turn-drops-lap.jsonl` must learn 0 laps with 1 drop.
- **Live:** ask the human for 2 laps without touching the camera, then 1 lap where they rotate the camera mid-lap. `lens.sh laps` must show 2 `Lap learned` lines, then 1 `Lap dropped` line where the yaw changed.
- **Proof:** the test result, the `lens.sh laps` output, and a screenshot of the card right after "Lap learned".

## Gotchas

- The game moves the camera's pitch on its own when expanded pitch is in use. A drop line where only the pitch changed to 383 means the cap regressed.
- Tabbing out of the window doesn't move the camera. A drop that happens at the moment of tab-out is worth a recording.
