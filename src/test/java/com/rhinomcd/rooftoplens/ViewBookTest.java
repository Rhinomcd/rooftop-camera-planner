package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ViewBookTest
{
    private static LapRecorder.Lap lap(CameraPose camera, double... xs)
    {
        LapRecorder.Click[] clicks = new LapRecorder.Click[xs.length];
        for (int i = 0; i < xs.length; i++)
        {
            clicks[i] = new LapRecorder.Click(new Point2D.Double(xs[i], 0.5), i == 0 ? null : new double[] { xs[i] * 100, 0, -50, 0, 0, 423 });
        }
        return new LapRecorder.Lap(camera, clicks);
    }

    private static void learnQuarterTurns(ViewBook book, int pitch)
    {
        book.record(lap(new CameraPose(0, pitch), 0.1, 0.9));
        book.record(lap(new CameraPose(512, pitch), 0.4, 0.6));
        book.record(lap(new CameraPose(1024, pitch), 0.2, 0.8));
        book.record(lap(new CameraPose(1536, pitch), 0.2, 0.8));
    }

    @Test
    public void routeIsTheMedianLapsCyclicTravel()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384);
        book.record(lap(view, 0.1, 0.3));
        book.record(lap(view, 0.1, 0.5));
        book.record(lap(view, 0.1, 0.9));
        ViewBook.View learned = book.view(view);
        assertEquals(3, learned.keptLaps());
        assertEquals(0.8, learned.route(), 1e-9);
        assertNull(learned.cameraOffset(0, 423));
        assertEquals(50, learned.cameraOffset(1, 423)[0], 1e-9);
        assertEquals(-50, learned.cameraOffset(1, 430)[2], 1e-9);
        assertNull(learned.cameraOffset(1, 480));
    }

    @Test
    public void routeFollowsEachLapNotTheAverageSpot()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384);
        book.record(lap(view, 0.1, 0.9));
        book.record(lap(view, 0.5, 0.5));
        book.record(lap(view, 0.9, 0.1));
        assertEquals(1.6, book.view(view).route(), 1e-9);
    }

    @Test
    public void bestIsTheShortestRoute()
    {
        ViewBook book = new ViewBook(2);
        assertNull(book.bestKey(new CameraPose(0, 384)));
        book.record(lap(new CameraPose(0, 384), 0.1, 0.9));
        book.record(lap(new CameraPose(512, 384), 0.4, 0.6));
        assertEquals("512:384", book.bestKey(new CameraPose(0, 384)));
    }

    @Test
    public void suggestsQuarterTurnsThenRefinesAroundTheBestThenSettles()
    {
        ViewBook book = new ViewBook(2);
        CameraPose camera = new CameraPose(100, 380);
        assertEquals("0:384", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(0, 384), 0.1, 0.9));
        assertEquals("512:384", book.suggestion(camera).viewKey());
        learnQuarterTurns(book, 384);
        assertEquals("256:384", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(256, 384), 0.3, 0.7));
        book.record(lap(new CameraPose(768, 384), 0.45, 0.55));
        assertEquals("640:384", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(640, 384), 0.3, 0.7));
        assertFalse(book.calibrated(camera));
        book.record(lap(new CameraPose(896, 384), 0.3, 0.7));
        assertEquals("768:384", book.suggestion(camera).viewKey());
        assertTrue(book.searched(camera));
        assertFalse(book.calibrated(camera));
        book.record(lap(new CameraPose(768, 384), 0.45, 0.55));
        book.record(lap(new CameraPose(768, 384), 0.45, 0.55));
        assertTrue(book.calibrated(camera));
    }

    @Test
    public void aLuckyLapAfterTheSearchDoesNotSendItWandering()
    {
        ViewBook book = new ViewBook(2);
        CameraPose camera = new CameraPose(100, 384);
        learnQuarterTurns(book, 384);
        book.record(lap(new CameraPose(256, 384), 0.3, 0.7));
        book.record(lap(new CameraPose(768, 384), 0.45, 0.55));
        book.record(lap(new CameraPose(640, 384), 0.3, 0.7));
        book.record(lap(new CameraPose(896, 384), 0.3, 0.7));
        book.record(lap(new CameraPose(896, 384), 0.5, 0.5));
        book.record(lap(new CameraPose(896, 384), 0.5, 0.5));
        assertEquals("896:384", book.bestKey(camera));
        assertEquals("896:384", book.suggestion(camera).viewKey());
    }

    @Test
    public void viewsAreComparedAtTheLatestLapsPitch()
    {
        ViewBook book = new ViewBook(2);
        book.record(lap(new CameraPose(512, 304), 0.49, 0.51));
        learnQuarterTurns(book, 384);
        CameraPose drifted = new CameraPose(512, 304);
        assertEquals(384, book.calibrationPitch(drifted));
        assertEquals("512:384", book.bestKey(drifted));
        assertEquals("256:384", book.suggestion(drifted).viewKey());
        assertEquals("Rotate left 45°", drifted.directionsTo(book.suggestion(drifted)));
        assertEquals("Tilt camera higher", drifted.directionsTo(new CameraPose(512, 384)));
    }
}
