package com.rhinomcd.rooftoplens;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.BackgroundComponent;
import net.runelite.client.ui.overlay.components.ComponentConstants;

/**
 * The calibrator card, in RuneLite's own overlay style: a viewfinder whose ring sits where the camera should
 * point (sideways for yaw, up and down for tilt, larger or smaller for zoom) and centers when the view is held.
 */
final class LensPanel extends Overlay
{
    private static final int WIDTH = 214;
    private static final int HEIGHT = 100;
    private static final int PAD = 6;
    private static final Rectangle FINDER = new Rectangle(PAD, 24, 60, 50);
    private static final int TEXT_X = FINDER.x + FINDER.width + 10;
    private static final Color GOOD = Color.GREEN;
    private static final Color TODO = Color.ORANGE;
    private static final Color DIM = new Color(200, 200, 200);

    private final RooftopLensPlugin plugin;

    @Inject
    LensPanel(RooftopLensPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        Course course = plugin.getCourse();
        CameraPose camera = plugin.getCamera();
        if (course == null || camera == null)
        {
            return null;
        }
        new BackgroundComponent(ComponentConstants.STANDARD_BACKGROUND_COLOR, new Rectangle(0, 0, WIDTH, HEIGHT), true)
            .render(g);

        LapRecorder recorder = plugin.getRecorder();
        ViewBook book = plugin.getBook();
        boolean lapRunning = recorder.inProgress();
        CameraPose goal = lapRunning ? recorder.startView() : book.suggestion(camera);
        boolean onGoal = lapRunning || camera.holds(goal);
        String bestKey = book.bestKey(camera);
        boolean isBest = goal.viewKey().equals(bestKey);

        Font bold = FontManager.getRunescapeBoldFont();
        Font small = FontManager.getRunescapeSmallFont();
        text(g, bold, course.displayName, PAD, 15, Color.WHITE);
        String lap = "Lap " + recorder.nextStep() + "/" + course.stepCount();
        text(g, small, lap, WIDTH - PAD - g.getFontMetrics(small).stringWidth(lap), 15, lapRunning ? GOOD : DIM);

        drawFinder(g, camera, goal, onGoal);

        String status = lapRunning ? "Lap in progress"
            : onGoal ? (isBest ? "On best view" : "On test view") : (isBest ? "Best view" : "Try this view");
        text(g, bold, status, TEXT_X, 34, onGoal ? GOOD : TODO);
        text(g, small, onGoal ? "Hold the camera" : camera.directionsTo(goal), TEXT_X, 49, Color.WHITE);
        ViewBook.View here = book.view(camera);
        text(g, small, here == null ? "This view: not learned"
            : "This view: " + here.keptLaps() + (here.keptLaps() == 1 ? " lap" : " laps") + " · " + Math.round(here.routePixels()) + " px",
            TEXT_X, 63, DIM);
        if (bestKey != null)
        {
            text(g, small, "Best: " + Math.round(book.views.get(bestKey).routePixels()) + " px", TEXT_X, 76, DIM);
        }

        String notice = plugin.getNotice();
        if (notice != null)
        {
            text(g, small, notice, PAD, HEIGHT - 5, plugin.isNoticeWarning() ? Color.RED : GOOD);
        }
        return new Dimension(WIDTH, HEIGHT);
    }

    private static void drawFinder(Graphics2D g, CameraPose camera, CameraPose goal, boolean onGoal)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double cx = FINDER.getCenterX();
        double cy = FINDER.getCenterY();
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        Color frame = onGoal ? GOOD : Color.WHITE;
        double k = 9;
        corner(g, FINDER.getMinX(), FINDER.getMinY(), k, k, frame);
        corner(g, FINDER.getMaxX(), FINDER.getMinY(), -k, k, frame);
        corner(g, FINDER.getMinX(), FINDER.getMaxY(), k, -k, frame);
        corner(g, FINDER.getMaxX(), FINDER.getMaxY(), -k, -k, frame);

        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 90));
        g.draw(new Line2D.Double(cx - 4, cy, cx + 4, cy));
        g.draw(new Line2D.Double(cx, cy - 4, cx, cy + 4));

        if (onGoal)
        {
            ring(g, cx, cy, 9, GOOD);
            g.fill(new Ellipse2D.Double(cx - 2, cy - 2, 4, 4));
            return;
        }
        double ox = clamp(camera.yawDelta(goal.yaw) / 512.0 * 18, 20);
        double oy = clamp(-(goal.pitch - camera.pitch) / 6.0, 15);
        double r = 9 + clamp((goal.zoom - camera.zoom) / 16.0, 5);
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 2f, 2f }, 0f));
        g.setColor(new Color(255, 255, 255, 120));
        g.draw(new Ellipse2D.Double(cx - 9, cy - 9, 18, 18));
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(TODO);
        g.draw(new Line2D.Double(cx, cy, cx + ox, cy + oy));
        ring(g, cx + ox, cy + oy, r, TODO);
    }

    private static void ring(Graphics2D g, double x, double y, double r, Color color)
    {
        g.setStroke(new BasicStroke(3f));
        g.setColor(Color.BLACK);
        g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(color);
        g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    private static void corner(Graphics2D g, double x, double y, double dx, double dy, Color color)
    {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(x + dx, y);
        path.lineTo(x, y);
        path.lineTo(x, y + dy);
        g.setColor(Color.BLACK);
        g.translate(1, 1);
        g.draw(path);
        g.translate(-1, -1);
        g.setColor(color);
        g.draw(path);
    }

    /** RuneLite overlay text: crisp RuneScape font with a one-pixel black shadow. */
    private static void text(Graphics2D g, Font font, String value, int x, int y, Color color)
    {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString(value, x + 1, y + 1);
        g.setColor(color);
        g.drawString(value, x, y);
    }

    private static double clamp(double value, double limit)
    {
        return Math.max(-limit, Math.min(limit, value));
    }
}
