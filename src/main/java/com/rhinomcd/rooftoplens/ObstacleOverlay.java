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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class ObstacleOverlay extends Overlay
{
    private static final Stroke OUTLINE = new BasicStroke(1.5f);
    private static final Stroke NEXT_OUTLINE = new BasicStroke(2.5f);
    private static final Stroke ROUTE_LINE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
        new float[] { 4f, 4f }, 0f);
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
        int next = plugin.getRecorder().nextStep() % plugin.getCourse().stepCount();
        int plane = client.getTopLevelWorldView().getPlane();
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
        }
        if (config.showRoute())
        {
            drawSpots(g, next);
        }
        return null;
    }

    /**
     * Where each obstacle will be on screen when you usually click it, fixed on screen while the camera follows you,
     * so the mouse can be waiting there before the obstacle arrives. The next spot is ringed and linked to the one
     * after it; the ring turns to the ready color once the previous obstacle's XP lands and the next click will count.
     */
    private void drawSpots(Graphics2D g, int next)
    {
        int steps = plugin.getCourse().stepCount();
        int after = (next + 1) % steps;
        Point2D.Double nextSpot = screenSpot(next);
        Point2D.Double afterSpot = screenSpot(after);
        if (nextSpot != null && afterSpot != null)
        {
            g.setColor(LensPalette.alpha(config.routeColor(), 160));
            g.setStroke(ROUTE_LINE);
            g.draw(new Line2D.Double(nextSpot, afterSpot));
        }
        for (int step = 0; step < steps; step++)
        {
            Point2D.Double spot = step == next ? nextSpot : step == after ? afterSpot : config.showAll() ? screenSpot(step) : null;
            if (spot == null)
            {
                continue;
            }
            Color color = step != next ? config.routeColor()
                : plugin.getRecorder().ready() ? config.readyColor() : config.nextColor();
            g.setColor(step == next ? color : LensPalette.alpha(color, 170));
            g.fill(new Ellipse2D.Double(spot.x - 2.5, spot.y - 2.5, 5, 5));
            if (step == next)
            {
                g.setStroke(NEXT_OUTLINE);
                g.draw(new Ellipse2D.Double(spot.x - 9, spot.y - 9, 18, 18));
            }
        }
    }

    /**
     * Projects the obstacle as the camera would see it from its usual focal point at click time: moving the camera
     * by some offset moves the scene by minus that offset, so this projects the current focal point minus the learned
     * offset, then shifts by the gap between the obstacle's base point and its clickbox center.
     */
    private Point2D.Double screenSpot(int step)
    {
        double[] offset = plugin.getBook().focalOffset(step);
        TileObject obstacle = obstacleAt(step);
        if (offset == null || obstacle == null)
        {
            return null;
        }
        Point spot = Perspective.localToCanvas(client, (int) Math.round(client.getCameraFocalPointX() - offset[0]),
            (int) Math.round(client.getCameraFocalPointY() - offset[1]), (int) Math.round(client.getCameraFocalPointZ() - offset[2]));
        if (spot == null)
        {
            return null;
        }
        LocalPoint at = obstacle.getLocalLocation();
        Point base = Perspective.localToCanvas(client, at.getX(), at.getY(), obstacle.getZ());
        Shape clickbox = obstacle.getClickbox();
        if (base == null || clickbox == null)
        {
            return new Point2D.Double(spot.getX(), spot.getY());
        }
        Rectangle bounds = clickbox.getBounds();
        return new Point2D.Double(spot.getX() + bounds.getCenterX() - base.getX(), spot.getY() + bounds.getCenterY() - base.getY());
    }

    private TileObject obstacleAt(int step)
    {
        int plane = client.getTopLevelWorldView().getPlane();
        return plugin.getObstacles().entrySet().stream()
            .filter(entry -> entry.getValue() == step && entry.getKey().getPlane() == plane)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
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
