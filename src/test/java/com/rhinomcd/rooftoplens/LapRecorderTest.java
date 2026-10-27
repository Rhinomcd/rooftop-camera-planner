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

    @Test
    public void completesAfterEveryStepInOrder()
    {
        LapRecorder recorder = new LapRecorder(3);
        assertEquals(LapRecorder.Outcome.NONE, recorder.obstacleClicked(0, CLICK, VIEW));
        assertEquals(LapRecorder.Outcome.NONE, recorder.obstacleClicked(1, CLICK, VIEW));
        assertEquals(2, recorder.nextStep());
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.obstacleClicked(2, click(0.9), VIEW));
        assertEquals(0.9, recorder.lastLap().clicks[2].lens.x, 0);
        assertFalse(recorder.inProgress());
    }

    @Test
    public void ignoresMisclicksAndRepeatClicks()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        recorder.obstacleClicked(0, click(0.1), VIEW);
        recorder.obstacleClicked(2, CLICK, VIEW);
        recorder.obstacleClicked(1, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.obstacleClicked(2, CLICK, VIEW));
        assertEquals(0.5, recorder.lastLap().clicks[0].lens.x, 0);
    }

    @Test
    public void movingTheCameraDropsTheLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        recorder.obstacleClicked(1, CLICK, VIEW);
        assertTrue(recorder.cameraSampled(new CameraPose(900, 384)));
        assertFalse(recorder.inProgress());
        assertFalse(recorder.cameraSampled(VIEW));
        assertEquals(LapRecorder.Outcome.NONE, recorder.obstacleClicked(2, CLICK, VIEW));
        assertEquals(LapRecorder.Outcome.NONE, recorder.obstacleClicked(0, CLICK, VIEW));
        assertTrue(recorder.inProgress());
    }

    @Test
    public void clickingFromAnotherViewDropsTheLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        assertEquals(LapRecorder.Outcome.CAMERA_MOVED, recorder.obstacleClicked(1, CLICK, new CameraPose(900, 384)));
        assertFalse(recorder.inProgress());
    }

    @Test
    public void firstObstacleRestartsAnAbandonedLap()
    {
        LapRecorder recorder = new LapRecorder(3);
        recorder.obstacleClicked(0, CLICK, VIEW);
        recorder.obstacleClicked(1, CLICK, VIEW);
        recorder.obstacleClicked(0, CLICK, new CameraPose(0, 300));
        assertEquals(1, recorder.nextStep());
        recorder.obstacleClicked(1, CLICK, new CameraPose(0, 300));
        assertEquals(LapRecorder.Outcome.COMPLETED, recorder.obstacleClicked(2, CLICK, new CameraPose(0, 300)));
        assertEquals("0:304", recorder.lastLap().camera.viewKey());
    }

    @Test
    public void driftingIntoTheNextViewIsNotCounted()
    {
        LapRecorder recorder = new LapRecorder(2);
        recorder.obstacleClicked(0, CLICK, new CameraPose(31, 384));
        assertTrue(recorder.cameraSampled(new CameraPose(55, 384)));
    }
}
