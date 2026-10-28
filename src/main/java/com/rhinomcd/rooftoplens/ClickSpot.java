package com.rhinomcd.rooftoplens;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;

/**
 * Where an obstacle appears on screen when the player clicks it, kept as the camera's position relative to the
 * obstacle. The camera follows the player by moving without turning, and moving the camera by some offset moves the
 * scene by minus that offset, so projecting the current camera position minus the learned offset puts the obstacle
 * where it will be at click time, still on screen for as long as yaw, pitch and zoom hold.
 */
final class ClickSpot
{
    /** Camera minus obstacle {x, y, z} in local units, then clickbox center minus obstacle base {x, y} in {@link Lens} units. */
    static final int OFFSET_AXES = 5;

    private ClickSpot()
    {
    }

    /** @return null when the obstacle's base point isn't on screen */
    static double[] offset(Client client, TileObject obstacle, Rectangle clickbox)
    {
        LocalPoint at = obstacle.getLocalLocation();
        Point base = Perspective.localToCanvas(client, at.getX(), at.getY(), obstacle.getZ());
        double scale = client.getScale();
        if (base == null || scale <= 0)
        {
            return null;
        }
        return new double[] { cameraX(client) - at.getX(), cameraY(client) - at.getY(), cameraZ(client) - obstacle.getZ(),
            (clickbox.getCenterX() - base.getX()) / scale, (clickbox.getCenterY() - base.getY()) / scale };
    }

    /** @return null when the spot is off screen */
    static Point2D.Double onScreen(Client client, double[] offset)
    {
        Point base = Perspective.localToCanvas(client, (int) Math.round(cameraX(client) - offset[0]),
            (int) Math.round(cameraY(client) - offset[1]), (int) Math.round(cameraZ(client) - offset[2]));
        if (base == null)
        {
            return null;
        }
        double scale = client.getScale();
        return new Point2D.Double(base.getX() + offset[3] * scale, base.getY() + offset[4] * scale);
    }

    /** The camera position {@link Perspective} projects from. */
    private static double cameraX(Client client)
    {
        return client.isGpu() ? client.getCameraFpX() : client.getCameraX();
    }

    private static double cameraY(Client client)
    {
        return client.isGpu() ? client.getCameraFpY() : client.getCameraY();
    }

    private static double cameraZ(Client client)
    {
        return client.isGpu() ? client.getCameraFpZ() : client.getCameraZ();
    }
}
