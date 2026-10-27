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
 * its recent laps, of the cyclic distance through that lap's clicks: the mouse travel one lap needs, in
 * projection-scale units so it survives window resizes and zoom. Views are compared at one pitch, the calibration
 * pitch: the pitch of the latest lap.
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

    void record(LapRecorder.Lap lap)
    {
        View view = views.computeIfAbsent(lap.camera.viewKey(), key -> new View(steps));
        view.laps++;
        view.learned = views.values().stream().mapToLong(other -> other.learned).max().orElse(0) + 1;
        for (int step = 0; step < steps; step++)
        {
            LapRecorder.Click click = lap.clicks[step];
            keep(view.clicks.get(step), click.lens);
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

    /** The snapped pitch views are compared at: the latest lap's, else the camera's own. */
    int calibrationPitch(CameraPose camera)
    {
        String latest = views.entrySet().stream()
            .max(Comparator.comparingLong(entry -> entry.getValue().learned))
            .map(Map.Entry::getKey)
            .orElse(camera.viewKey());
        return CameraPose.fromViewKey(latest).pitch;
    }

    /** The shortest-route view at the calibration pitch, or null if none has a lap. */
    String bestKey(CameraPose camera)
    {
        int pitch = calibrationPitch(camera);
        return views.entrySet().stream()
            .filter(entry -> entry.getValue().keptLaps() > 0 && CameraPose.fromViewKey(entry.getKey()).pitch == pitch)
            .min(Comparator.comparingDouble(entry -> entry.getValue().route()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * The view to try next, at the calibration pitch: an untried quarter turn nearest the current yaw, then untried
     * yaws 45 and then 22.5 degrees either side of the best view, then the best view itself.
     */
    CameraPose suggestion(CameraPose current)
    {
        CameraPose home = new CameraPose(current.yaw, calibrationPitch(current));
        CameraPose untried = nearestUntried(home, QUARTER_TURNS);
        if (untried != null)
        {
            return untried;
        }
        CameraPose best = CameraPose.fromViewKey(bestKey(current));
        for (int spread : REFINE_SPREADS)
        {
            untried = nearestUntried(home, new int[] { best.yaw - spread, best.yaw + spread });
            if (untried != null)
            {
                return untried;
            }
        }
        return best;
    }

    /** True once every view the search wants has a lap, so the suggestion is the best view for good. */
    boolean calibrated(CameraPose current)
    {
        String best = bestKey(current);
        return best != null && suggestion(current).viewKey().equals(best);
    }

    private CameraPose nearestUntried(CameraPose home, int[] yaws)
    {
        CameraPose nearest = null;
        for (int yaw : yaws)
        {
            CameraPose probe = CameraPose.fromViewKey(new CameraPose(yaw, home.pitch).viewKey());
            View tried = views.get(probe.viewKey());
            if ((tried == null || tried.keptLaps() == 0)
                && (nearest == null || Math.abs(home.yawDelta(probe.yaw)) < Math.abs(home.yawDelta(nearest.yaw))))
            {
                nearest = probe;
            }
        }
        return nearest;
    }

    static final class View
    {
        int laps;
        /** Order of this view's latest lap among all views; higher is more recent. */
        long learned;
        /** Per step, the most recent clean-lap clicks in lens units {x, y} (see {@link LapRecorder.Click#lens}). */
        final List<List<double[]>> clicks;
        /** Per step, the same clicks as fractions of the obstacle's clickbox bounds. Placed on the live obstacle. */
        final List<List<double[]>> boxClicks;

        View(int steps)
        {
            clicks = emptySteps(steps);
            boxClicks = emptySteps(steps);
        }

        /** Median click per step inside the obstacle's clickbox bounds, or null if none was recorded. */
        Point2D.Double boxSpot(int step)
        {
            List<double[]> kept = boxClicks.get(step);
            return kept.isEmpty() ? null : median(kept);
        }

        /** Laps whose clicks still count toward the route. */
        int keptLaps()
        {
            return clicks.stream().mapToInt(List::size).min().orElse(0);
        }

        /** Median over recent laps of one lap's cyclic click-to-click distance, in lens units. */
        double route()
        {
            int laps = keptLaps();
            double[] routes = new double[laps];
            for (int lap = 0; lap < laps; lap++)
            {
                for (int step = 0; step < clicks.size(); step++)
                {
                    List<double[]> from = clicks.get(step);
                    List<double[]> to = clicks.get((step + 1) % clicks.size());
                    double[] a = from.get(from.size() - laps + lap);
                    double[] b = to.get(to.size() - laps + lap);
                    routes[lap] += Math.hypot(b[0] - a[0], b[1] - a[1]);
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
