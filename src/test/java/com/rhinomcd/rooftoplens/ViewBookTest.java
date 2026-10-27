package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ViewBookTest
{
    private static LapRecorder.Lap lap(CameraPose camera, double... xs)
    {
        LapRecorder.Click[] clicks = new LapRecorder.Click[xs.length];
        for (int i = 0; i < xs.length; i++)
        {
            clicks[i] = new LapRecorder.Click(new Point2D.Double(xs[i], 0.5), i == 0 ? null : new Point2D.Double(0.25, 0.75));
        }
        return new LapRecorder.Lap(camera, clicks);
    }

    @Test
    public void routeIsTheMedianLapsCyclicTravel()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384, 256);
        book.record(lap(view, 0.1, 0.3), 1000, 800);
        book.record(lap(view, 0.1, 0.5), 1000, 800);
        book.record(lap(view, 0.1, 0.9), 1000, 800);
        ViewBook.View learned = book.view(view);
        assertEquals(3, learned.laps);
        assertEquals(800, learned.routePixels(), 1e-6);
        assertNull(learned.boxSpot(0));
        assertEquals(0.75, learned.boxSpot(1).y, 1e-9);
    }

    @Test
    public void bestIsTheShortestRoute()
    {
        ViewBook book = new ViewBook(2);
        assertNull(book.bestKey(new CameraPose(0, 384, 256)));
        book.record(lap(new CameraPose(0, 384, 256), 0.1, 0.9), 1000, 800);
        book.record(lap(new CameraPose(512, 384, 256), 0.4, 0.6), 1000, 800);
        assertEquals("512:384:256", book.bestKey(new CameraPose(0, 384, 256)));
    }

    @Test
    public void suggestsQuarterTurnsThenRefinesAroundTheBest()
    {
        ViewBook book = new ViewBook(2);
        CameraPose camera = new CameraPose(100, 380, 250);
        assertEquals("0:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(0, 384, 256), 0.1, 0.9), 1000, 800);
        assertEquals("512:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(512, 384, 256), 0.4, 0.6), 1000, 800);
        book.record(lap(new CameraPose(1024, 384, 256), 0.2, 0.8), 1000, 800);
        book.record(lap(new CameraPose(1536, 384, 256), 0.2, 0.8), 1000, 800);
        assertEquals("256:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(256, 384, 256), 0.3, 0.7), 1000, 800);
        book.record(lap(new CameraPose(768, 384, 256), 0.45, 0.55), 1000, 800);
        assertEquals("640:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(640, 384, 256), 0.3, 0.7), 1000, 800);
        book.record(lap(new CameraPose(896, 384, 256), 0.3, 0.7), 1000, 800);
        assertEquals("768:384:256", book.suggestion(camera).viewKey());
    }

    @Test
    public void viewsAtAnotherZoomAreNotCompared()
    {
        ViewBook book = new ViewBook(2);
        book.record(lap(new CameraPose(0, 384, 256), 0.4, 0.6), 1000, 800);
        book.record(lap(new CameraPose(512, 384, 896), 0.49, 0.51), 1000, 800);
        assertEquals("0:384:256", book.bestKey(new CameraPose(0, 384, 256)));
        assertEquals("512:384:896", book.bestKey(new CameraPose(0, 384, 896)));
    }

    @Test
    public void resizingTheCanvasStartsTheViewsRouteAfresh()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384, 256);
        book.record(lap(view, 0.1, 0.9), 1000, 800);
        book.record(lap(view, 0.4, 0.6), 500, 400);
        assertEquals(200, book.view(view).routePixels(), 1e-6);
        assertEquals(1, book.view(view).keptLaps());
    }

    @Test
    public void routeFollowsEachLapNotTheAverageSpot()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384, 256);
        book.record(lap(view, 0.1, 0.9), 1000, 800);
        book.record(lap(view, 0.5, 0.5), 1000, 800);
        book.record(lap(view, 0.9, 0.1), 1000, 800);
        assertEquals(1600, book.view(view).routePixels(), 1e-6);
    }

    @Test
    public void resizingTheCanvasRestartsEveryViewsRoute()
    {
        ViewBook book = new ViewBook(2);
        book.record(lap(new CameraPose(0, 384, 256), 0.45, 0.55), 1000, 800);
        book.record(lap(new CameraPose(512, 384, 256), 0.4, 0.6), 500, 400);
        assertEquals(0, book.view(new CameraPose(0, 384, 256)).keptLaps());
        assertEquals("512:384:256", book.bestKey(new CameraPose(0, 384, 256)));
        assertEquals("0:384:256", book.suggestion(new CameraPose(0, 384, 256)).viewKey());
    }
}
