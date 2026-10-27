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
3. Run a lap from the first obstacle to the last without touching the camera. The card shows
   **Lap learned**. If the camera moved, it shows **Camera moved, lap not counted** and nothing is saved.
4. In any view you've lapped, the overlay draws your usual click spots joined in lap order, with a ring on the
   next one.

The next obstacle is filled amber. Other obstacles are outlined in teal with their step number.

## Settings

| Setting | Default | What it does |
| --- | --- | --- |
| Show learned route | on | Draw your click spots and route in views you've lapped |
| Outline every obstacle | on | Outline all visible obstacles, not only the next one |
| Next obstacle | amber | Color of the obstacle to click next |
| Route | teal | Color of other obstacles and the learned route |

## Courses

Draynor Village, Al Kharid, Varrock, Canifis, Falador, Seers' Village, Pollnivneach, Rellekka, Ardougne.

## Development

```
./gradlew test        # unit tests
./gradlew run         # RuneLite dev client with the plugin loaded
```
