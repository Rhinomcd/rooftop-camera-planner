package com.rhinomcd.rooftoplens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class ObstacleOverlay extends Overlay
{
    private static final Stroke OUTLINE = new BasicStroke(1.5f);
    private static final Stroke NEXT_OUTLINE = new BasicStroke(2.5f);
    private static final Font BADGE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final Client client;
    private final RooftopLensPlugin plugin;
    private final RooftopLensConfig config;

    @Inject
    ObstacleOverlay(Client client, RooftopLensPlugin plugin, RooftopLensConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (plugin.getCourse() == null)
        {
            return null;
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int next = plugin.getRecorder().nextStep();
        int plane = client.getTopLevelWorldView().getPlane();
        ViewBook.View view = config.showRoute() && plugin.getCamera() != null ? plugin.getBook().view(plugin.getCamera()) : null;
        for (Map.Entry<TileObject, Integer> entry : plugin.getObstacles().entrySet())
        {
            int step = entry.getValue();
            boolean isNext = step == next;
            Shape clickbox = entry.getKey().getPlane() == plane ? entry.getKey().getClickbox() : null;
            if (clickbox == null || (!isNext && !config.showAll()))
            {
                continue;
            }
            Color color = isNext ? config.nextColor() : config.routeColor();
            if (isNext)
            {
                g.setColor(LensPalette.alpha(color, 60));
                g.fill(clickbox);
            }
            g.setColor(isNext ? color : LensPalette.alpha(color, 150));
            g.setStroke(isNext ? NEXT_OUTLINE : OUTLINE);
            g.draw(clickbox);
            Rectangle bounds = clickbox.getBounds();
            badge(g, step + 1, bounds.getCenterX(), bounds.getMinY() - 10, color, isNext);
            Point2D.Double spot = view == null ? null : view.boxSpot(step);
            if (spot != null)
            {
                drawSpot(g, bounds.x + spot.x * bounds.width, bounds.y + spot.y * bounds.height, color, isNext);
            }
        }
        return null;
    }

    /** Where you usually click this obstacle in this view. */
    private static void drawSpot(Graphics2D g, double x, double y, Color color, boolean isNext)
    {
        g.setColor(color);
        if (isNext)
        {
            g.setStroke(NEXT_OUTLINE);
            g.draw(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
        }
        g.fill(new Ellipse2D.Double(x - 2.5, y - 2.5, 5, 5));
    }

    private static void badge(Graphics2D g, int number, double centerX, double centerY, Color color, boolean filled)
    {
        g.setFont(BADGE_FONT);
        FontMetrics metrics = g.getFontMetrics();
        String text = String.valueOf(number);
        double radius = 8;
        Ellipse2D.Double circle = new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2, radius * 2);
        g.setColor(filled ? color : LensPalette.INK);
        g.fill(circle);
        g.setStroke(OUTLINE);
        g.setColor(color);
        g.draw(circle);
        g.setColor(filled ? LensPalette.INK : LensPalette.TEXT);
        g.drawString(text, (float) (centerX - metrics.stringWidth(text) / 2.0),
            (float) (centerY + metrics.getAscent() / 2.0 - 1));
    }
}
