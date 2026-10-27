<p align="center"><img src="icon.png" width="96" alt="Rooftop Lens icon"></p>
<h1 align="center">Rooftop Lens</h1>
<p align="center">Find the rooftop Agility camera view that keeps your clicks closest together.</p>

Rooftop Lens watches your laps. Each lap you finish without moving the camera is filed under that camera view,
along with where you clicked each obstacle. A view's score is the distance your mouse travels through your usual
click spots for one lap. The shortest route wins.

The plugin never moves the camera, mouse, or character, and never clicks for you. Everything is stored in your
local RuneLite config.

## Using it

1. Go to a rooftop course. The HUD card appears with the course name and one dot per obstacle.
2. Follow the dial: it points the way to turn, and the text says how far. **Try this view** is a quarter turn you
   haven't lapped yet; **Best view** is your shortest route so far.
3. Run a lap from the first obstacle to the last without touching the camera. While a lap runs, the card holds
   the view you started in. A finished lap shows **Lap learned**. If the camera leaves that view, the card shows
   **Camera moved, lap dropped**; the next lap starts when you click the first obstacle again.
4. In any view you've lapped, each obstacle shows a dot where you usually click it, with a ring on the next one.
   The dot is placed on the obstacle's live clickbox, so it stays on the obstacle as the camera follows you.

The next obstacle is filled amber. Other obstacles are outlined in teal with their step number.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| Show click spots | on | Mark where you usually click each obstacle, in views you've lapped |
| Outline every obstacle | on | Outline all visible obstacles, not only the next one |
| Next obstacle | amber | Color of the obstacle to click next |
| Other obstacles | teal | Color of other obstacles and their click spots |

## Courses

Draynor Village, Al Kharid, Varrock, Canifis, Falador, Seers' Village, Pollnivneach, Rellekka, Ardougne.

## Credits

Rooftop Lens is a fork and rewrite of [Rooftop Camera Planner](https://github.com/MichaelRVP/rooftop-camera-planner)
by MVP IT Solutions ([MichaelRVP](https://github.com/MichaelRVP)). That project came up with the idea of learning
a low-travel camera view from your laps and marking the click route, and its course table is carried over here.
Course data traces back to RuneLite's Agility plugin. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## License

[BSD 2-Clause](LICENSE). The original copyright notice from Rooftop Camera Planner is kept alongside this fork's.

## Development

```
./gradlew test        # unit tests
./gradlew run         # RuneLite dev client with the plugin loaded
```
