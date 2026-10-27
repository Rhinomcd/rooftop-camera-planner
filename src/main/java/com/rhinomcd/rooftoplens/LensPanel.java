package com.rhinomcd.rooftoplens;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/** The HUD card: course, lap progress, and which way to turn the camera next. */
final class LensPanel extends Overlay
{
    private static final int WIDTH = 232;
    private static final int HEIGHT = 132;
    private static final int PAD = 12;
    private static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    private static final Font LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 9);

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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(LensPalette.INK);
        g.fill(new RoundRectangle2D.Double(0, 0, WIDTH, HEIGHT, 14, 14));
        g.setColor(LensPalette.alpha(LensPalette.TEAL, 90));
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, WIDTH - 1, HEIGHT - 1, 14, 14));

        g.setFont(TITLE);
        g.setColor(LensPalette.TEXT);
        g.drawString(course.displayName, PAD, PAD + 11);
        drawPips(g, course.stepCount(), plugin.getRecorder().nextStep(), plugin.getRecorder().inProgress());

        g.setColor(LensPalette.RULE);
        g.draw(new Line2D.Double(PAD, 44, WIDTH - PAD, 44));

        ViewBook book = plugin.getBook();
        boolean lapRunning = plugin.getRecorder().inProgress();
        CameraPose goal = lapRunning ? plugin.getRecorder().startView() : book.suggestion(camera);
        boolean onGoal = camera.holds(goal);
        drawCompass(g, PAD + 26, 82, camera, goal, onGoal);

        int textX = PAD + 64;
        boolean isBest = goal.viewKey().equals(book.bestKey());
        g.setFont(LABEL);
        g.setColor(onGoal ? LensPalette.TEAL : LensPalette.MUTED);
        g.drawString(lapRunning ? "LAP IN PROGRESS"
            : onGoal ? (isBest ? "ON BEST VIEW" : "ON TEST VIEW") : (isBest ? "BEST VIEW" : "TRY THIS VIEW"), textX, 62);
        g.setFont(BODY);
        g.setColor(LensPalette.TEXT);
        g.drawString(onGoal ? "Hold the camera for a lap" : camera.directionsTo(goal), textX, 78);

        ViewBook.View here = book.view(camera);
        g.setColor(LensPalette.MUTED);
        g.drawString(here == null ? "This view: not learned" : "This view: " + here.laps + (here.laps == 1 ? " lap" : " laps")
            + " · " + Math.round(here.routePixels()) + " px", textX, 94);
        String bestKey = book.bestKey();
        if (bestKey != null)
        {
            g.drawString("Best: " + Math.round(book.views.get(bestKey).routePixels()) + " px", textX, 108);
        }

        String notice = plugin.getNotice();
        if (notice != null)
        {
            g.setFont(LABEL);
            g.setColor(plugin.isNoticeWarning() ? LensPalette.CORAL : LensPalette.TEAL);
            g.drawString(notice.toUpperCase(), PAD, HEIGHT - 9);
        }
        return new Dimension(WIDTH, HEIGHT);
    }

    private static void drawPips(Graphics2D g, int steps, int next, boolean inProgress)
    {
        double size = 7;
        double gap = 5;
        double x = WIDTH - PAD - steps * size - (steps - 1) * gap;
        for (int step = 0; step < steps; step++)
        {
            Ellipse2D.Double pip = new Ellipse2D.Double(x + step * (size + gap), PAD + 4, size, size);
            if (inProgress && step < next)
            {
                g.setColor(LensPalette.TEAL);
                g.fill(pip);
            }
            else
            {
                g.setColor(step == next ? LensPalette.AMBER : LensPalette.alpha(LensPalette.MUTED, 110));
                g.draw(pip);
            }
        }
    }

    /** A dial seen from above: the needle points where the camera should face relative to now. */
    private static void drawCompass(Graphics2D g, double cx, double cy, CameraPose camera, CameraPose goal, boolean onGoal)
    {
        double r = 24;
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(LensPalette.alpha(LensPalette.TEAL, onGoal ? 255 : 80));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(LensPalette.alpha(LensPalette.TEXT, 70));
        g.draw(new Line2D.Double(cx, cy - r + 3, cx, cy - r + 8));
        if (onGoal)
        {
            g.setColor(LensPalette.TEAL);
            g.fill(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));
            return;
        }
        double angle = camera.yawDelta(goal.yaw) * 2 * Math.PI / CameraPose.YAW_UNITS;
        double tipX = cx + Math.sin(angle) * (r - 5);
        double tipY = cy - Math.cos(angle) * (r - 5);
        g.setColor(LensPalette.AMBER);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx, cy, tipX, tipY));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(tipX, tipY);
        head.lineTo(tipX - Math.sin(angle - 0.5) * 7, tipY + Math.cos(angle - 0.5) * 7);
        head.lineTo(tipX - Math.sin(angle + 0.5) * 7, tipY + Math.cos(angle + 0.5) * 7);
        head.closePath();
        g.fill(head);
        g.fill(new Ellipse2D.Double(cx - 3, cy - 3, 6, 6));
    }
}
