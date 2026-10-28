package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;

/**
 * Follows one lap through obstacle clicks and Agility XP drops. A click on the expected obstacle counts once the
 * previous obstacle's XP has landed; a click before that is held, because the game may or may not take it. If the
 * game took it, the next XP drop confirms it; if not, the player's next click replaces it. A lap starts on the
 * course's first obstacle and completes on the last obstacle's XP. Clicks on other obstacles are ignored; clicking
 * the first obstacle again starts over. Moving the camera out of the starting view before the last click drops the
 * lap.
 */
final class LapRecorder
{
    enum Outcome { NONE, COMPLETED, CAMERA_MOVED }

    private final int steps;
    private Click[] clicks;
    private CameraPose start;
    private int next = -1;
    /** The last counted click's obstacle hasn't given XP yet. */
    private boolean awaitingXp;
    /** A click on the next obstacle made while {@link #awaitingXp}. */
    private Click early;
    private Lap lastLap;

    LapRecorder(int steps)
    {
        this.steps = steps;
    }

    Outcome obstacleClicked(int step, Click click, CameraPose camera)
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
        if (awaitingXp)
        {
            early = click;
            return Outcome.NONE;
        }
        count(click);
        awaitingXp = true;
        return Outcome.NONE;
    }

    Outcome agilityXpGained()
    {
        if (next < 0)
        {
            return Outcome.NONE;
        }
        if (awaitingXp)
        {
            awaitingXp = false;
        }
        else if (early != null)
        {
            count(early);
        }
        else
        {
            return Outcome.NONE;
        }
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
        if (next > 0 && next < steps && !camera.viewKey().equals(start.viewKey()))
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

    /** Index of the step expected next, or 0 when no lap is running; equals the step count while the last obstacle runs. */
    int nextStep()
    {
        return next < 0 ? 0 : next;
    }

    boolean inProgress()
    {
        return next > 0;
    }

    /** Whether the next obstacle can be clicked now: no counted click is still waiting for its XP. */
    boolean ready()
    {
        return next < 0 || !awaitingXp;
    }

    /** The view the lap in progress started in. */
    CameraPose startView()
    {
        return start;
    }

    private void begin(Click click, CameraPose camera)
    {
        clicks = new Click[steps];
        start = camera;
        next = 0;
        count(click);
        awaitingXp = true;
    }

    private void count(Click click)
    {
        clicks[next++] = click;
        early = null;
    }

    static final class Click
    {
        /** Where the clicked obstacle sat on screen, in {@link Lens} units. Scores the view. */
        final Point2D.Double lens;
        /**
         * The camera's focal point minus the obstacle's position, {x, y, z} in local units: where the camera was,
         * relative to the obstacle, when it was clicked. Null when the obstacle wasn't on screen.
         */
        final double[] focalOffset;

        Click(Point2D.Double lens, double[] focalOffset)
        {
            this.lens = lens;
            this.focalOffset = focalOffset;
        }
    }

    static final class Lap
    {
        final CameraPose camera;
        final Click[] clicks;

        Lap(CameraPose camera, Click[] clicks)
        {
            this.camera = camera;
            this.clicks = clicks;
        }
    }
}
