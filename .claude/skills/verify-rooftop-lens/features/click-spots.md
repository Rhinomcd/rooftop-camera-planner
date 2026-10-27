# Click spots

Obstacles get numbered badges. The next obstacle is filled amber, and the others are outlined. In a view with learned laps, each obstacle shows a dot where the player usually clicks it, placed inside the live clickbox, with a ring on the next one.

## Sub-features

- `badges`: a step-number badge above every visible obstacle.
- `next-highlight`: the next obstacle is filled.
- `spots`: a dot on each obstacle at its median learned click position, and a ring on the next one.

## How to get to it (user POV)

- Complete at least one lap in the current view, with "Show click spots" turned on.

## Driving it with lens.sh

- **Replay:** run `lens.sh replay` on any fixture with a completed lap. Frames after the first learned lap show dots inside the obstacle boxes.
- **Live:** ask the human for one lap, then a screenshot mid-lap. The dots must sit on the obstacles, not at fixed screen points.
- **Proof:** a replay frame, plus a live screenshot with the dots on the obstacles.

## Gotchas

- Views learned before 1.0.0's switch to clickbox-relative spots have no spot data until they get a new lap.
- Red boxes come from other plugins, such as TicTac7x Rooftops, not from Rooftop Lens.
