package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LapRecorderTest
{
    private static final CameraPose VIEW = new CameraPose(512, 384);
    private static final LapRecorder.Click CLICK = click(0.5);

    private static LapRecorder.Click click(double x)
    {
        return new LapRecorder.Click(new Point2D.Double(x, x), null);
    }

    /** Clicks the step and lets its obstacle finish. */
    private static LapRecorder.Outcome cross(LapRecorder recorder, int step, LapRecorder.Click click, CameraPose camera)
    {
        recorder.obstacleClicked(step, click, camera);
        return recorder.agilityXpGained();
    }

    @Test
    public void completesOnTheLastObstaclesXp()
    {
        LapRecorder recorder = new LapRecorder(3);
        cross(recorder, 0, CLICK, VIEW);
        cross(recorder, 1, CLICK, VIEW);
        assertEquals(2, recorder.nextStep());
        assertEquals(LapRecorder.Outcome.NONE, recorder.obstacleClicked(2, click(0.9), VIEW));
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.agilityXpGained());
        assertEquals(0.9, recorder.lastLap().clicks[2].lens.x, 0);
        assertFalse(recorder.inProgress());
    }

    @Test
    public void aClickBeforeTheXpDoesNotAdvance()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        assertFalse(recorder.ready());
        recorder.obstacleClicked(1, click(0.1), VIEW);
        assertEquals(1, recorder.nextStep());
        recorder.agilityXpGained();
        assertTrue(recorder.ready());
        assertEquals(1, recorder.nextStep());
        recorder.obstacleClicked(1, click(0.7), VIEW);
        assertEquals(2, recorder.nextStep());
        recorder.agilityXpGained();
        recorder.obstacleClicked(2, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.agilityXpGained());
        assertEquals(0.7, recorder.lastLap().clicks[1].lens.x, 0);
    }

    @Test
    public void anEarlyClickTheGameTookIsConfirmedByTheNextXp()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        recorder.obstacleClicked(1, click(0.2), VIEW);
        recorder.agilityXpGained();
        recorder.agilityXpGained();
        assertEquals(2, recorder.nextStep());
        recorder.obstacleClicked(2, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.agilityXpGained());
        assertEquals(0.2, recorder.lastLap().clicks[1].lens.x, 0);
    }

    @Test
    public void ignoresMisclicksAndRepeatClicks()
    {
        LapRecorder recorder = new LapRecorder(3);
        cross(recorder, 0, CLICK, VIEW);
        recorder.obstacleClicked(0, click(0.1), VIEW);
        recorder.obstacleClicked(2, CLICK, VIEW);
        cross(recorder, 1, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.COMPLETED, cross(recorder, 2, CLICK, VIEW));
        assertEquals(0.5, recorder.lastLap().clicks[0].lens.x, 0);
    }

    @Test
    public void movingTheCameraDropsTheLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        cross(recorder, 0, CLICK, VIEW);
        cross(recorder, 1, CLICK, VIEW);
        assertTrue(recorder.cameraSampled(new CameraPose(900, 384)));
        assertFalse(recorder.inProgress());
        assertFalse(recorder.cameraSampled(VIEW));
        assertEquals(LapRecorder.Outcome.NONE, cross(recorder, 2, CLICK, VIEW));
        recorder.obstacleClicked(0, CLICK, VIEW);
        assertTrue(recorder.inProgress());
    }

    @Test
    public void movingTheCameraAfterTheLastClickKeepsTheLap()
    {
        LapRecorder recorder = new LapRecorder(2);
        cross(recorder, 0, CLICK, VIEW);
        recorder.obstacleClicked(1, CLICK, VIEW);
        assertFalse(recorder.cameraSampled(new CameraPose(900, 384)));
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.agilityXpGained());
    }

    @Test
    public void clickingFromAnotherViewDropsTheLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        cross(recorder, 0, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.CAMERA_MOVED, recorder.obstacleClicked(1, CLICK, new CameraPose(900, 384)));
        assertFalse(recorder.inProgress());
    }

    @Test
    public void firstObstacleRestartsAnAbandonedLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        cross(recorder, 0, CLICK, VIEW);
        cross(recorder, 1, CLICK, VIEW);
        CameraPose other = new CameraPose(0, 300);
        cross(recorder, 0, CLICK, other);
        assertEquals(1, recorder.nextStep());
        cross(recorder, 1, CLICK, other);
        assertEquals(LapRecorder.Outcome.COMPLETED, cross(recorder, 2, CLICK, other));
        assertEquals("0:304", recorder.lastLap().camera.viewKey());
    }

    @Test
    public void driftingIntoTheNextViewIsNotCounted()
    {
        LapRecorder recorder = new LapRecorder(2);
        cross(recorder, 0, CLICK, new CameraPose(31, 384));
        assertTrue(recorder.cameraSampled(new CameraPose(55, 384)));
    }
}
