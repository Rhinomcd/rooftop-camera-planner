# Changelog

## 1.0.0

First release of Rooftop Lens, a rewrite of Rooftop Camera Planner 0.1.3.

- Laps are filed under the camera view you actually held, so every clean lap counts.
- Courses are detected from obstacles in the loaded scene, so laps no longer reset where a course crosses a map region.
- Camera angles are read in the units RuneLite reports (JAU14) and compared in JAU11 with one tolerance rule.
- Camera suggestions are hints: four quarter-turn views, then 45° and 22.5° either side of the best, then the best view, marked Calibrated. Nothing is rejected or banned.
- Routes are measured in projection-scale units, so resizing the window or zooming keeps every learned view.
- New HUD, obstacle badges, learned-route overlay, and icon.
