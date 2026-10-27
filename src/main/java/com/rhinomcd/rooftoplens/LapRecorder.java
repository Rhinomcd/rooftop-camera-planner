package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;

/**
 * Follows obstacle clicks through one lap. A lap starts on the course's first obstacle, advances only on the
 * expected next obstacle, and completes on the last one. Clicks on other obstacles (misclicks, re-clicks) are
 * ignored; clicking the first obstacle again starts over.
 */
final class LapRecorder
{
    enum Outcome { NONE, COMPLETED, CAMERA_MOVED }

    private final int steps;
    private Point2D.Double[] clicks;
    private CameraPose start;
    private int next = -1;
    private boolean cameraHeld;
    private Lap lastLap;

    LapRecorder(int steps)
    {
        this.steps = steps;
    }

    /** @param click canvas position as a fraction of canvas width and height */
    Outcome obstacleClicked(int step, Point2D.Double click, CameraPose camera)
    {
        if (step == 0 && next != 1)
        {
            begin(click, camera);
            return Outcome.NONE;
        }
        if (step != next)
        {
            return Outcome.NONE;
        }
        clicks[step] = click;
        cameraHeld &= camera.holds(start);
        next++;
        if (next < steps)
        {
            return Outcome.NONE;
        }
        next = -1;
        if (!cameraHeld)
        {
            return Outcome.CAMERA_MOVED;
        }
        lastLap = new Lap(start, clicks.clone());
        return Outcome.COMPLETED;
    }

    void cameraSampled(CameraPose camera)
    {
        if (next > 0)
        {
            cameraHeld &= camera.holds(start);
        }
    }

    Lap lastLap()
    {
        return lastLap;
    }

    /** Index of the step expected next, or 0 when no lap is running. */
    int nextStep()
    {
        return next < 0 ? 0 : next;
    }

    boolean inProgress()
    {
        return next > 0;
    }

    boolean cameraHeld()
    {
        return next <= 0 || cameraHeld;
    }

    void reset()
    {
        next = -1;
    }

    private void begin(Point2D.Double click, CameraPose camera)
    {
        clicks = new Point2D.Double[steps];
        clicks[0] = click;
        start = camera;
        cameraHeld = true;
        next = 1;
    }

    static final class Lap
    {
        final CameraPose camera;
        final Point2D.Double[] clicks;

        Lap(CameraPose camera, Point2D.Double[] clicks)
        {
            this.camera = camera;
            this.clicks = clicks;
        }
    }
}
