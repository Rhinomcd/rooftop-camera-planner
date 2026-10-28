# View scoring

A view is a snapped yaw and pitch. Each saved view keeps the last 8 clicks per obstacle, as offsets from the viewport center divided by `client.getScale()`, so window resizes and zoom don't change them. Its score is the median lap's cyclic distance through its clicks. Views are compared at the calibration pitch, the pitch of the latest lap. The card suggests untried quarter turns, then 45° either side of the best quarter turn, then 22.5° either side of the best of those (at most 8 views, so noisy laps cannot extend it), then the best view, and says "Calibrated" once none are left. Views persist in the RuneLite config.

## Sub-features

- `persist`: views are saved to `rooftopLens.views.<COURSE>` and reload after a restart.
- `score`: "This view" and "Best" show the route in pixels at the current scale.
- `suggest`: untried quarter turns come first, then the refinements around the best, then the best view.
- `confirm`: with no untried view left, the card says "Confirm best view" until the best view has 3 laps.
- `calibrated`: then it says "Calibrated" on the best view and "Back to best view" off it.

## How to get to it (user POV)

- Lap in several camera views, then restart the client.

## Driving it with lens.sh

- **Replay:** `ViewBookTest` covers scoring and suggestions. `lens.sh replay` prints the saved views.
- **Live:** after the human's laps, read `rooftopLens.views.RELLEKKA` in `~/.runelite/profiles2/*.properties`. Then `lens.sh stop` and `lens.sh launch`, and the card's "Best" value must match.
- **Proof:** the config value before and after the restart, plus the card screenshot.

## Gotchas

- Pitch above 383 is saved as 383. Views saved at a higher pitch are skipped when loading.
- Views saved before 1.0.0's `yaw:pitch` keys (`yaw:pitch:zoom`) are skipped when loading.
- Recordings from before the recorder logged `scale` and the viewport replay with the canvas height as the scale, which is only approximate when the player zoomed.
