package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every camera view the player has completed a clean lap in, for one course. A view's score is the median, over
 * its recent laps, of the cyclic pixel distance through that lap's clicks: the mouse travel one lap needs.
 * Views are only compared at the same pitch and zoom, because zooming out shrinks every distance.
 */
final class ViewBook
{
    private static final int CLICKS_KEPT = 8;
    private static final int[] QUARTER_TURNS = { 0, 512, 1024, 1536 };
    private static final int[] REFINE_SPREADS = { 256, 128 };

    private final int steps;
    final Map<String, View> views = new LinkedHashMap<>();

    ViewBook(int steps)
    {
        this.steps = steps;
    }

    void record(LapRecorder.Lap lap, int canvasWidth, int canvasHeight)
    {
        View view = views.computeIfAbsent(lap.camera.viewKey(), key -> new View(steps));
        if (view.canvasWidth != canvasWidth || view.canvasHeight != canvasHeight)
        {
            view.clicks.forEach(List::clear);
        }
        view.laps++;
        view.canvasWidth = canvasWidth;
        view.canvasHeight = canvasHeight;
        if (view.boxClicks == null)
        {
            view.boxClicks = emptySteps(steps);
        }
        for (int step = 0; step < steps; step++)
        {
            LapRecorder.Click click = lap.clicks[step];
            keep(view.clicks.get(step), click.canvas);
            if (click.onBox != null)
            {
                keep(view.boxClicks.get(step), click.onBox);
            }
        }
    }

    private static void keep(List<double[]> kept, Point2D.Double point)
    {
        kept.add(new double[] { point.x, point.y });
        if (kept.size() > CLICKS_KEPT)
        {
            kept.remove(0);
        }
    }

    private static List<List<double[]>> emptySteps(int steps)
    {
        List<List<double[]>> lists = new ArrayList<>(steps);
        for (int i = 0; i < steps; i++)
        {
            lists.add(new ArrayList<>());
        }
        return lists;
    }

    View view(CameraPose camera)
    {
        return views.get(camera.viewKey());
    }

    /** The shortest-route view at the camera's pitch and zoom, or null if none has a lap. */
    String bestKey(CameraPose camera)
    {
        String level = level(camera);
        return views.entrySet().stream()
            .filter(entry -> level(CameraPose.fromViewKey(entry.getKey())).equals(level))
            .min(Comparator.comparingDouble(entry -> entry.getValue().routePixels()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * The view to try next, at the camera's pitch and zoom: an untried quarter turn nearest the current yaw, then
     * untried yaws 45 and then 22.5 degrees either side of the best view, then the best view itself.
     */
    CameraPose suggestion(CameraPose current)
    {
        CameraPose untried = nearestUntried(current, QUARTER_TURNS);
        for (int spread : REFINE_SPREADS)
        {
            if (untried != null)
            {
                return untried;
            }
            int best = CameraPose.fromViewKey(bestKey(current)).yaw;
            untried = nearestUntried(current, new int[] { best - spread, best + spread });
        }
        return untried != null ? untried : CameraPose.fromViewKey(bestKey(current));
    }

    private CameraPose nearestUntried(CameraPose current, int[] yaws)
    {
        CameraPose nearest = null;
        for (int yaw : yaws)
        {
            CameraPose probe = CameraPose.fromViewKey(new CameraPose(yaw, current.pitch, current.zoom).viewKey());
            if (!views.containsKey(probe.viewKey())
                && (nearest == null || Math.abs(current.yawDelta(probe.yaw)) < Math.abs(current.yawDelta(nearest.yaw))))
            {
                nearest = probe;
            }
        }
        return nearest;
    }

    private static String level(CameraPose camera)
    {
        String key = camera.viewKey();
        return key.substring(key.indexOf(':'));
    }

    static final class View
    {
        int laps;
        int canvasWidth;
        int canvasHeight;
        /** Per step, the most recent clean-lap clicks as canvas fractions {x, y}. Scores the view. */
        final List<List<double[]>> clicks;
        /** Per step, the same clicks as fractions of the obstacle's clickbox bounds. Placed on the live obstacle. */
        List<List<double[]>> boxClicks;

        View(int steps)
        {
            clicks = emptySteps(steps);
            boxClicks = emptySteps(steps);
        }

        /** Median click per step inside the obstacle's clickbox bounds, or null if none was recorded. */
        Point2D.Double boxSpot(int step)
        {
            List<double[]> kept = boxClicks == null ? null : boxClicks.get(step);
            return kept == null || kept.isEmpty() ? null : median(kept);
        }

        /** Median over recent laps of one lap's cyclic click-to-click distance, in pixels. */
        double routePixels()
        {
            int laps = clicks.stream().mapToInt(List::size).min().orElse(0);
            double[] routes = new double[laps];
            for (int lap = 0; lap < laps; lap++)
            {
                for (int step = 0; step < clicks.size(); step++)
                {
                    List<double[]> from = clicks.get(step);
                    List<double[]> to = clicks.get((step + 1) % clicks.size());
                    double[] a = from.get(from.size() - laps + lap);
                    double[] b = to.get(to.size() - laps + lap);
                    routes[lap] += Math.hypot((b[0] - a[0]) * canvasWidth, (b[1] - a[1]) * canvasHeight);
                }
            }
            Arrays.sort(routes);
            int mid = laps / 2;
            return laps == 0 ? Double.MAX_VALUE : laps % 2 == 1 ? routes[mid] : (routes[mid - 1] + routes[mid]) / 2;
        }

        private static Point2D.Double median(List<double[]> kept)
        {
            return new Point2D.Double(median(kept, 0), median(kept, 1));
        }

        private static double median(List<double[]> values, int axis)
        {
            double[] sorted = values.stream().mapToDouble(value -> value[axis]).sorted().toArray();
            int mid = sorted.length / 2;
            return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
        }
    }
}
