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
 * where it will be at click time, still on screen for as long as yaw, pitch and zoom hold. The camera's orbit
 * around the player depends on its pitch, which a view snaps and caps, so each offset keeps the pitch it was learned
 * at and only offsets near the current pitch are used.
 */
final class ClickSpot
{
    /**
     * Camera minus obstacle {x, y, z} in local units, clickbox center minus obstacle base {x, y} in {@link Lens}
     * units, then the camera's uncapped pitch in JAU11.
     */
    static final int OFFSET_AXES = 6;
    static final int PITCH_AXIS = 5;
    /** About 1.4 degrees; further than this the orbit moves the spot by more than a click's worth. */
    static final int PITCH_TOLERANCE = 8;

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
            (clickbox.getCenterX() - base.getX()) / scale, (clickbox.getCenterY() - base.getY()) / scale, pitch(client) };
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

    static int pitch(Client client)
    {
        return client.getCameraPitchTarget() >> 3;
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
