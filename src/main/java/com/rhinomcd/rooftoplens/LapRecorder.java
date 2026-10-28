package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;

/**
 * Follows one lap through obstacle clicks and Agility XP drops. A click on the expected obstacle counts once the
 * previous obstacle's XP has landed; a click before that is held, because the game may or may not take it. If the
 * game took it, the next XP drop or a click on the obstacle after it confirms it; if not, the player's next click
 * replaces it. A lap starts on the course's first obstacle and completes on the last obstacle's XP; clicking the first
 * obstacle while the last one runs is held for the next lap. Clicks on other obstacles are ignored, and clicking the
 * first obstacle mid-lap starts over. Moving the camera out of the starting view before the last click drops the lap.
 */
final class LapRecorder
{
    enum Outcome { NONE, COMPLETED, CAMERA_MOVED }

    private final int steps;
    private Click[] clicks;
    private CameraPose start;
    /** The step expected next: -1 with no lap, {@code steps} while the last obstacle's XP is pending. */
    private int next = -1;
    /** The last counted click's obstacle hasn't given XP yet. */
    private boolean awaitingXp;
    /** A click on the next obstacle made while {@link #awaitingXp}, and the camera it was made from. */
    private Click early;
    private CameraPose earlyCamera;
    private Lap lastLap;

    LapRecorder(int steps)
    {
        this.steps = steps;
    }

    Outcome obstacleClicked(int step, Click click, CameraPose camera)
    {
        boolean takenEarly = next >= 0 && next < steps && early != null && !awaitingXp && step == (next + 1) % steps;
        if (step == 0 && next == steps)
        {
            hold(click, camera);
            return Outcome.NONE;
        }
        if (step == 0 && next != 0 && next != 1 && !takenEarly)
        {
            clicks = new Click[steps];
            next = 0;
            count(click, camera);
            awaitingXp = true;
            return Outcome.NONE;
        }
        if (next < 0 || (step != next && !takenEarly))
        {
            return Outcome.NONE;
        }
        if (cameraSampled(camera))
        {
            return Outcome.CAMERA_MOVED;
        }
        if (takenEarly)
        {
            // The held click's obstacle is underway, so this click is early for the one after it.
            count(early, earlyCamera);
            awaitingXp = true;
            hold(click, camera);
        }
        else if (awaitingXp)
        {
            hold(click, camera);
        }
        else
        {
            count(click, camera);
            awaitingXp = true;
        }
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
            count(early, earlyCamera);
        }
        else
        {
            return Outcome.NONE;
        }
        if (next < steps)
        {
            return Outcome.NONE;
        }
        lastLap = new Lap(start, clicks.clone());
        next = early == null ? -1 : 0;
        clicks = new Click[steps];
        return Outcome.COMPLETED;
    }

    /** @return true when this sample dropped the lap in progress */
    boolean cameraSampled(CameraPose camera)
    {
        if (next > 0 && next < steps && !camera.viewKey().equals(start.viewKey()))
        {
            next = -1;
            early = null;
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

    private void hold(Click click, CameraPose camera)
    {
        early = click;
        earlyCamera = camera;
    }

    private void count(Click click, CameraPose camera)
    {
        if (next == 0)
        {
            start = camera;
        }
        clicks[next++] = click;
        early = null;
    }

    static final class Click
    {
        /** Where the clicked obstacle sat on screen, in {@link Lens} units. Scores the view. */
        final Point2D.Double lens;
        /** Where the camera stood relative to the obstacle; see {@link ClickSpot#offset}. Null when the obstacle wasn't on screen. */
        final double[] cameraOffset;

        Click(Point2D.Double lens, double[] cameraOffset)
        {
            this.lens = lens;
            this.cameraOffset = cameraOffset;
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
