package com.rhinomcd.rooftoplens;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import org.junit.Test;

public class ClickSpotTest
{
    private final Client client = mock(Client.class);
    private float cameraX;
    private float cameraY;

    public ClickSpotTest()
    {
        when(client.isGpu()).thenReturn(true);
        when(client.getCameraFpX()).thenAnswer(call -> cameraX);
        when(client.getCameraFpY()).thenAnswer(call -> cameraY);
        when(client.getCameraFpZ()).thenReturn(-1200f);
        when(client.getCameraFpPitch()).thenReturn(1.2f);
        when(client.getCameraFpYaw()).thenReturn(5.5f);
        when(client.getScale()).thenReturn(512);
        when(client.getViewportWidth()).thenReturn(1000);
        when(client.getViewportHeight()).thenReturn(700);
    }

    @Test
    public void theSpotStaysStillWhileTheCameraFollowsThePlayer()
    {
        TileObject obstacle = mock(TileObject.class);
        when(obstacle.getLocalLocation()).thenReturn(new LocalPoint(6400, 6400, 0));
        when(obstacle.getZ()).thenReturn(-300);
        cameraX = 5600;
        cameraY = 6000;
        double[] offset = ClickSpot.offset(client, obstacle, new Rectangle(480, 300, 40, 60));
        Point2D.Double atClick = ClickSpot.onScreen(client, offset);

        cameraX = 4200;
        cameraY = 5100;
        Point2D.Double later = ClickSpot.onScreen(client, offset);
        assertEquals(atClick.x, later.x, 1);
        assertEquals(atClick.y, later.y, 1);
    }
}
