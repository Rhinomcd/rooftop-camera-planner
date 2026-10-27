package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every camera view the player has completed a clean lap in, for one course. A view's score is the cyclic
 * pixel distance through its learned click spots: the mouse travel one lap needs in that view.
 */
final class ViewBook
{
    private static final int CLICKS_KEPT = 8;
    private static final int[] PROBE_YAWS = { 0, 512, 1024, 1536 };

    private final int steps;
    final Map<String, View> views = new LinkedHashMap<>();

    ViewBook(int steps)
    {
        this.steps = steps;
    }

    void record(LapRecorder.Lap lap, int canvasWidth, int canvasHeight)
    {
        View view = views.computeIfAbsent(lap.camera.viewKey(), key -> new View(steps));
        view.laps++;
        view.canvasWidth = canvasWidth;
        view.canvasHeight = canvasHeight;
        for (int step = 0; step < steps; step++)
        {
            List<double[]> kept = view.clicks.get(step);
            kept.add(new double[] { lap.clicks[step].x, lap.clicks[step].y });
            if (kept.size() > CLICKS_KEPT)
            {
                kept.remove(0);
            }
        }
    }

    View view(CameraPose camera)
    {
        return views.get(camera.viewKey());
    }

    String bestKey()
    {
        return views.entrySet().stream()
            .min(Comparator.comparingDouble(entry -> entry.getValue().routePixels()))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * The view to try next: an untried quarter turn at the pitch and zoom of the first learned view, nearest
     * to the current yaw. Once every quarter has a lap, the best view.
     */
    CameraPose suggestion(CameraPose current)
    {
        CameraPose anchor = views.isEmpty() ? current : CameraPose.fromViewKey(views.keySet().iterator().next());
        CameraPose nearest = null;
        for (int yaw : PROBE_YAWS)
        {
            CameraPose probe = CameraPose.fromViewKey(new CameraPose(yaw, anchor.pitch, anchor.zoom).viewKey());
            if (!views.containsKey(probe.viewKey())
                && (nearest == null || Math.abs(current.yawDelta(yaw)) < Math.abs(current.yawDelta(nearest.yaw))))
            {
                nearest = probe;
            }
        }
        if (nearest != null)
        {
            return nearest;
        }
        return CameraPose.fromViewKey(bestKey());
    }

    static final class View
    {
        int laps;
        int canvasWidth;
        int canvasHeight;
        /** Per step, the most recent clean-lap clicks as canvas fractions {x, y}. */
        final List<List<double[]>> clicks;

        View(int steps)
        {
            clicks = new ArrayList<>(steps);
            for (int i = 0; i < steps; i++)
            {
                clicks.add(new ArrayList<>());
            }
        }

        /** Median click per step, as canvas fractions. */
        Point2D.Double spot(int step)
        {
            List<double[]> kept = clicks.get(step);
            return new Point2D.Double(median(kept, 0), median(kept, 1));
        }

        double routePixels()
        {
            double total = 0;
            for (int step = 0; step < clicks.size(); step++)
            {
                Point2D.Double from = spot(step);
                Point2D.Double to = spot((step + 1) % clicks.size());
                total += Math.hypot((to.x - from.x) * canvasWidth, (to.y - from.y) * canvasHeight);
            }
            return total;
        }

        private static double median(List<double[]> values, int axis)
        {
            double[] sorted = values.stream().mapToDouble(value -> value[axis]).sorted().toArray();
            int mid = sorted.length / 2;
            return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
        }
    }
}
