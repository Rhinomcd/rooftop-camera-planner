package com.rhinomcd.rooftoplens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every camera view the player has completed a clean lap in, for one course. A view's score is the median, over
 * its recent laps, of the cyclic distance through that lap's clicks: the mouse travel one lap needs, in
 * projection-scale units so it survives window resizes and zoom. Views are compared at one pitch, the calibration
 * pitch: the pitch of the latest lap.
 */
final class ViewBook
{
    private static final int CLICKS_KEPT = 8;
    /** Laps the best view needs before its score is trusted; one lucky lap shouldn't settle the search. */
    static final int CONFIRM_LAPS = 3;
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
        view.learned = views.values().stream().mapToLong(other -> other.learned).max().orElse(0) + 1;
        for (int step = 0; step < steps; step++)
        {
            LapRecorder.Click click = lap.clicks[step];
            keep(view.clicks.get(step), new double[] { click.lens.x, click.lens.y });
            if (click.cameraOffset != null)
            {
                keep(view.cameraOffsets.get(step), click.cameraOffset);
            }
        }
    }

    private static void keep(List<double[]> kept, double[] point)
    {
        kept.add(point);
        if (kept.size() > CLICKS_KEPT)
        {
            kept.remove(0);
        }
    }

    static List<List<double[]>> emptySteps(int steps)
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
     * yaws 45 degrees either side of the best quarter turn, then 22.5 degrees either side of the best so far, then
     * the best view itself. Each refinement centers on views already lapped, so a noisy lap can't send the search
     * wandering: it tries at most eight views.
     */
    CameraPose suggestion(CameraPose current)
    {
        CameraPose home = new CameraPose(current.yaw, calibrationPitch(current));
        List<Integer> searched = new ArrayList<>();
        for (int yaw : QUARTER_TURNS)
        {
            searched.add(yaw);
        }
        CameraPose untried = nearestUntried(home, searched);
        for (int spread : REFINE_SPREADS)
        {
            if (untried != null)
            {
                return untried;
            }
            int center = bestAmong(home.pitch, searched);
            List<Integer> refine = Arrays.asList(center - spread, center + spread);
            untried = nearestUntried(home, refine);
            searched.addAll(refine);
        }
        return untried != null ? untried : CameraPose.fromViewKey(bestKey(current));
    }

    private int bestAmong(int pitch, List<Integer> yaws)
    {
        return yaws.stream()
            .map(yaw -> new CameraPose(yaw, pitch).viewKey())
            .filter(key -> views.containsKey(key) && views.get(key).keptLaps() > 0)
            .min(Comparator.comparingDouble(key -> views.get(key).route()))
            .map(key -> CameraPose.fromViewKey(key).yaw)
            .orElseThrow(IllegalStateException::new);
    }

    /** True once every view the search wants has a lap and the best view has been confirmed over several. */
    boolean calibrated(CameraPose current)
    {
        return searched(current) && views.get(bestKey(current)).keptLaps() >= CONFIRM_LAPS;
    }

    /** True once every view the search wants has a lap, so the suggestion is the best view. */
    boolean searched(CameraPose current)
    {
        String best = bestKey(current);
        return best != null && suggestion(current).viewKey().equals(best);
    }

    private CameraPose nearestUntried(CameraPose home, List<Integer> yaws)
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
        /** Order of this view's latest lap among all views; higher is more recent. */
        long learned;
        /** Per step, the most recent clean-lap clicks in {@link Lens} units {x, y}. */
        final List<List<double[]>> clicks;

        /**
         * Per step, recent {@link LapRecorder.Click#cameraOffset}s. The camera orbits the player differently in each
         * view, so these only hold in the view they were learned in.
         */
        List<List<double[]>> cameraOffsets;

        View(int steps)
        {
            clicks = emptySteps(steps);
            cameraOffsets = emptySteps(steps);
        }

        /** The usual {@link LapRecorder.Click#cameraOffset} for a step at this camera pitch, or null if none was recorded. */
        double[] cameraOffset(int step, int pitch)
        {
            List<double[]> kept = cameraOffsets.get(step).stream()
                .filter(offset -> Math.abs(offset[ClickSpot.PITCH_AXIS] - pitch) <= ClickSpot.PITCH_TOLERANCE)
                .collect(Collectors.toList());
            if (kept.isEmpty())
            {
                return null;
            }
            double[] median = new double[kept.get(0).length];
            for (int axis = 0; axis < median.length; axis++)
            {
                int a = axis;
                double[] sorted = kept.stream().mapToDouble(value -> value[a]).sorted().toArray();
                int mid = sorted.length / 2;
                median[axis] = sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
            }
            return median;
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
    }
}
