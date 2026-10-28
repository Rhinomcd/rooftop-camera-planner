package com.rhinomcd.rooftoplens;

import java.awt.geom.Point2D;
import net.runelite.api.Client;

/**
 * Screen positions as offsets from the viewport center divided by the projection scale. Resizing the window or
 * zooming scales the scene about that center, so a lens position stays put for a fixed yaw and pitch.
 */
final class Lens
{
    private Lens()
    {
    }

    /** @return null while the client has no projection scale */
    static Point2D.Double fromScreen(Client client, double x, double y)
    {
        double scale = client.getScale();
        if (scale <= 0)
        {
            return null;
        }
        return new Point2D.Double((x - centerX(client)) / scale, (y - centerY(client)) / scale);
    }

    static Point2D.Double toScreen(Client client, Point2D.Double lens)
    {
        double scale = client.getScale();
        return new Point2D.Double(centerX(client) + lens.x * scale, centerY(client) + lens.y * scale);
    }

    private static double centerX(Client client)
    {
        return client.getViewportXOffset() + client.getViewportWidth() / 2.0;
    }

    private static double centerY(Client client)
    {
        return client.getViewportYOffset() + client.getViewportHeight() / 2.0;
    }
}
