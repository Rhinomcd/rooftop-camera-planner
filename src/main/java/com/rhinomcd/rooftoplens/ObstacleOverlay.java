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
import java.awt.geom.Path2D;
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
    private static final Stroke ROUTE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
        1f, new float[] { 6f, 6f }, 0f);
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
        for (Map.Entry<TileObject, Integer> entry : plugin.getObstacles().entrySet())
        {
            boolean isNext = entry.getValue() == next;
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
            badge(g, entry.getValue() + 1, bounds.getCenterX(), bounds.getMinY() - 10, color, isNext);
        }
        if (config.showRoute())
        {
            drawRoute(g, next);
        }
        return null;
    }

    private void drawRoute(Graphics2D g, int next)
    {
        ViewBook.View view = plugin.getCamera() == null ? null : plugin.getBook().view(plugin.getCamera());
        if (view == null)
        {
            return;
        }
        int width = client.getCanvasWidth();
        int height = client.getCanvasHeight();
        int steps = plugin.getCourse().stepCount();
        Path2D.Double path = new Path2D.Double();
        for (int step = 0; step <= steps; step++)
        {
            Point2D.Double spot = view.spot(step % steps);
            if (step == 0)
            {
                path.moveTo(spot.x * width, spot.y * height);
            }
            else
            {
                path.lineTo(spot.x * width, spot.y * height);
            }
        }
        g.setStroke(ROUTE);
        g.setColor(LensPalette.alpha(config.routeColor(), 120));
        g.draw(path);
        Point2D.Double target = view.spot(next);
        double x = target.x * width;
        double y = target.y * height;
        g.setStroke(NEXT_OUTLINE);
        g.setColor(config.nextColor());
        g.draw(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
        g.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
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
