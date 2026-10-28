# Click spots

Obstacles get numbered badges. The next obstacle is filled amber, and the others are outlined. In a view with learned laps, the screen shows where each obstacle will be when the player usually clicks it, fixed on screen while the camera follows, so the mouse can wait there during the obstacle animation. Each click stores the camera focal point minus the obstacle's position; the overlay projects the current focal point minus that median offset, plus the gap from the obstacle's base point to its clickbox center. Offsets are world positions, so they're learned per course (`rooftopLens.spots.<COURSE>`) and hold in every view, pitch, zoom, and window size. The next spot's ring is amber while the previous obstacle runs and green once its XP lands.

## Sub-features

- `badges`: a step-number badge above every visible obstacle.
- `next-highlight`: the next obstacle is filled.
- `spots`: a dot at each step's median learned screen spot; only the next two unless "Outline every obstacle" is on.
- `next-spot`: a ring on the next spot, and a dashed line to the spot after it.
- `ready`: the ring turns green ("Click now" color) when the previous obstacle's XP lands.

## How to get to it (user POV)

- Complete at least one lap on the course, in any view, with "Show click spots" turned on.

## Driving it with lens.sh

- **Replay:** run `lens.sh replay` on any fixture with a completed lap. Frames after the first learned lap show the ringed next spot and the dashed line.
- **Live:** ask the human for one lap, then a screenshot mid-lap. The spots must stay fixed on screen while the character moves, and the next obstacle should arrive under the ring by the time the human clicks.
- **Proof:** a replay frame, plus a live screenshot mid-animation with the ring where the next click lands.

## Gotchas

- The spot is where the human clicked, so it sits on the obstacle only at click time; while the character runs it can sit on empty ground. That's intended.
- Red boxes come from other plugins, such as TicTac7x Rooftops, not from Rooftop Lens.
