# Changelog

## 1.0.0

First release of Rooftop Lens, a rewrite of Rooftop Camera Planner 0.1.3.

- Laps are filed under the camera view you actually held, so every clean lap counts.
- Courses are detected from obstacles in the loaded scene, so laps no longer reset where a course crosses a map region.
- Camera angles are read in the units RuneLite reports (JAU14) and compared in JAU11 with one tolerance rule.
- Camera suggestions are hints: four quarter-turn views, then the best view you've lapped. Nothing is rejected or banned.
- New HUD, obstacle badges, learned-route overlay, and icon.
