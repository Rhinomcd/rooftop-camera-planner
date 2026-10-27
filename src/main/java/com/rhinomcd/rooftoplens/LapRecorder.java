package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;

/**
 * Follows obstacle clicks through one lap. A lap starts on the course's first obstacle, advances only on the
 * expected next obstacle, and completes on the last one. Clicks on other obstacles (misclicks, re-clicks) are
 * ignored; clicking the first obstacle again starts over. Moving the camera out of the starting view drops the lap.
 */
final class LapRecorder
{
    enum Outcome { NONE, COMPLETED, CAMERA_MOVED }

    private final int steps;
    private Point2D.Double[] clicks;
    private CameraPose start;
    private int next = -1;
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
        if (cameraSampled(camera))
        {
            return Outcome.CAMERA_MOVED;
        }
        clicks[step] = click;
        next++;
        if (next < steps)
        {
            return Outcome.NONE;
        }
        next = -1;
        lastLap = new Lap(start, clicks.clone());
        return Outcome.COMPLETED;
    }

    /** @return true when this sample dropped the lap in progress */
    boolean cameraSampled(CameraPose camera)
    {
        if (next > 0 && !camera.viewKey().equals(start.viewKey()))
        {
            next = -1;
            return true;
        }
        return false;
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

    /** The view the lap in progress started in. */
    CameraPose startView()
    {
        return start;
    }

    private void begin(Point2D.Double click, CameraPose camera)
    {
        clicks = new Point2D.Double[steps];
        clicks[0] = click;
        start = camera;
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
