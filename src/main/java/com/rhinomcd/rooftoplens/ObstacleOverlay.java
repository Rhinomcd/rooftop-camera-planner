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
import net.runelite.api.TileObject;
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
        }
        if (view != null)
        {
            drawSpots(g, view, next);
        }
        return null;
    }

    /**
     * Where you usually click each obstacle in this view, fixed on screen rather than on the obstacle, so the mouse
     * can be waiting there before the obstacle arrives. The next spot is ringed and linked to the one after it.
     */
    private void drawSpots(Graphics2D g, ViewBook.View view, int next)
    {
        int steps = plugin.getCourse().stepCount();
        Point2D.Double nextSpot = screenSpot(view, next);
        Point2D.Double afterSpot = screenSpot(view, (next + 1) % steps);
        if (nextSpot != null && afterSpot != null)
        {
            g.setColor(LensPalette.alpha(config.routeColor(), 160));
            g.setStroke(ROUTE_LINE);
            g.draw(new Line2D.Double(nextSpot, afterSpot));
        }
        for (int step = 0; step < steps; step++)
        {
            Point2D.Double spot = step == next ? nextSpot : screenSpot(view, step);
            if (spot == null || (step != next && step != (next + 1) % steps && !config.showAll()))
            {
                continue;
            }
            Color color = step == next ? config.nextColor() : config.routeColor();
            g.setColor(step == next ? color : LensPalette.alpha(color, 170));
            g.fill(new Ellipse2D.Double(spot.x - 2.5, spot.y - 2.5, 5, 5));
            if (step == next)
            {
                g.setStroke(NEXT_OUTLINE);
                g.draw(new Ellipse2D.Double(spot.x - 9, spot.y - 9, 18, 18));
                badge(g, step + 1, spot.x + 16, spot.y - 12, color, true);
            }
        }
    }

    private Point2D.Double screenSpot(ViewBook.View view, int step)
    {
        Point2D.Double spot = view.spot(step);
        return spot == null ? null : Lens.toScreen(client, spot);
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
