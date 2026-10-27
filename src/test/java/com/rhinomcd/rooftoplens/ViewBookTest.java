package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ViewBookTest
{
    private static LapRecorder.Lap lap(CameraPose camera, double... xs)
    {
        Point2D.Double[] clicks = new Point2D.Double[xs.length];
        for (int i = 0; i < xs.length; i++)
        {
            clicks[i] = new Point2D.Double(xs[i], 0.5);
        }
        return new LapRecorder.Lap(camera, clicks);
    }

    @Test
    public void routeIsTheCycleThroughMedianClickSpots()
    {
        ViewBook book = new ViewBook(2);
        CameraPose view = new CameraPose(0, 384, 256);
        book.record(lap(view, 0.1, 0.3), 1000, 800);
        book.record(lap(view, 0.1, 0.5), 1000, 800);
        book.record(lap(view, 0.1, 0.9), 1000, 800);
        ViewBook.View learned = book.view(view);
        assertEquals(3, learned.laps);
        assertEquals(0.5, learned.spot(1).x, 1e-9);
        assertEquals(800, learned.routePixels(), 1e-6);
    }

    @Test
    public void bestIsTheShortestRoute()
    {
        ViewBook book = new ViewBook(2);
        assertNull(book.bestKey());
        book.record(lap(new CameraPose(0, 384, 256), 0.1, 0.9), 1000, 800);
        book.record(lap(new CameraPose(512, 384, 256), 0.4, 0.6), 1000, 800);
        assertEquals("512:384:256", book.bestKey());
    }

    @Test
    public void suggestsUntriedQuarterTurnsThenTheBestView()
    {
        ViewBook book = new ViewBook(2);
        CameraPose camera = new CameraPose(100, 380, 250);
        assertEquals("0:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(0, 384, 256), 0.1, 0.9), 1000, 800);
        assertEquals("512:384:256", book.suggestion(camera).viewKey());
        book.record(lap(new CameraPose(512, 384, 256), 0.4, 0.6), 1000, 800);
        book.record(lap(new CameraPose(1024, 384, 256), 0.2, 0.8), 1000, 800);
        book.record(lap(new CameraPose(1536, 384, 256), 0.2, 0.8), 1000, 800);
        assertEquals("512:384:256", book.suggestion(camera).viewKey());
    }
}
