# View scoring

Each saved view keeps the last 8 clicks per obstacle. Its score is the cyclic pixel distance through the median click spots. The card suggests untried quarter turns, then the best view, and views persist in the RuneLite config.

## Sub-features

- `persist`: views are saved to `rooftopLens.views.<COURSE>` and reload after a restart.
- `score`: "This view" and "Best" show route pixels.
- `suggest`: untried quarter turns come first, then the best view.

## How to get to it (user POV)

- Lap in several camera views, then restart the client.

## Driving it with lens.sh

- **Replay:** `ViewBookTest` covers scoring and suggestions. `lens.sh replay` prints the saved views.
- **Live:** after the human's laps, read `rooftopLens.views.RELLEKKA` in `~/.runelite/profiles2/*.properties`. Then `lens.sh stop` and `lens.sh launch`, and the card's "Best" value must match.
- **Proof:** the config value before and after the restart, plus the card screenshot.

## Gotchas

- Pitch above 383 is saved as 383. Views saved at a higher pitch are skipped when loading.
