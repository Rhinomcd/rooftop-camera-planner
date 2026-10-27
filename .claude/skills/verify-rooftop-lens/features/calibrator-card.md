# Calibrator card

The card at the top left shows the course, lap progress, and a viewfinder. The orange ring sits where the camera should point: left or right for rotation, up or down for tilt, and larger or smaller for zoom. On the view, the ring centers and turns green, and it stays green for the whole lap.

## Sub-features

- `card-align`: "Try this view" or "Best view", with directions such as "Rotate right 90°".
- `card-hold`: "Lap in progress" with a green, centered ring for the whole lap.
- `card-stats`: "This view: N laps · X px" and "Best: X px".
- `card-notice`: "Lap learned" in green, "Camera moved, lap dropped" in red.

## How to get to it (user POV)

- Stand on a supported course with the plugin enabled.

## Driving it with lens.sh

- **Replay:** run `lens.sh replay src/test/resources/recordings/pitch-snap-rellekka.jsonl` and open the frames. Frame 0 shows "Lap 1/7" with a green viewfinder, and the last frame shows "Lap learned".
- **Live:** ask the human to stand at the course start. Run `lens.sh screenshot` to see the card. Ask them to turn the camera until the ring centers, and check that the direction text agrees with the way the ring is offset.
- **Proof:** a frame or screenshot for each state.

## Gotchas

- Whether "Rotate right" matches the in-game direction was unverified as of 1.0.0. Confirm it with the human.
