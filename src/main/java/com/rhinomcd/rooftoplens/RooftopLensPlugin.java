package com.rhinomcd.rooftoplens;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
    name = "Rooftop Lens",
    description = "Learns which camera view keeps your rooftop Agility clicks closest together",
    tags = {"agility", "rooftop", "camera", "marks", "route"}
)
public class RooftopLensPlugin extends Plugin
{
    private static final Type VIEWS_TYPE = new TypeToken<LinkedHashMap<String, ViewBook.View>>() { }.getType();
    private static final int NOTICE_TICKS = 10;

    @Inject private Client client;
    @Inject private ConfigManager configManager;
    @Inject private Gson gson;
    @Inject private OverlayManager overlayManager;
    @Inject private ObstacleOverlay obstacleOverlay;
    @Inject private LensPanel lensPanel;

    private final Map<TileObject, Integer> obstacles = new HashMap<>();
    private Course course;
    private LapRecorder recorder;
    private ViewBook book;
    private CameraPose camera;
    private String notice;
    private boolean noticeWarning;
    private int noticeTicks;

    @Provides
    RooftopLensConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RooftopLensConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(obstacleOverlay);
        overlayManager.add(lensPanel);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(obstacleOverlay);
        overlayManager.remove(lensPanel);
        obstacles.clear();
        course = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN)
        {
            obstacles.clear();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (course != null && obstacles.isEmpty() && !recorder.inProgress())
        {
            course = null;
        }
        if (noticeTicks > 0 && --noticeTicks == 0)
        {
            notice = null;
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (course == null)
        {
            return;
        }
        camera = CameraPose.read(client);
        recorder.cameraSampled(camera);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        int step = course == null ? -1 : course.stepOf(event.getId());
        if (step < 0)
        {
            return;
        }
        Point2D.Double click = clickPoint(event.getId());
        if (click == null)
        {
            return;
        }
        switch (recorder.obstacleClicked(step, click, CameraPose.read(client)))
        {
            case COMPLETED:
                book.record(recorder.lastLap(), client.getCanvasWidth(), client.getCanvasHeight());
                configManager.setConfiguration(RooftopLensConfig.GROUP, viewsKey(course), gson.toJson(book.views));
                showNotice("Lap learned", false);
                break;
            case CAMERA_MOVED:
                showNotice("Camera moved, lap not counted", true);
                break;
            default:
                break;
        }
    }

    @Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { track(e.getGameObject()); }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { obstacles.remove(e.getGameObject()); }
    @Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { track(e.getWallObject()); }
    @Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { obstacles.remove(e.getWallObject()); }
    @Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { track(e.getDecorativeObject()); }
    @Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { obstacles.remove(e.getDecorativeObject()); }
    @Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { track(e.getGroundObject()); }
    @Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { obstacles.remove(e.getGroundObject()); }

    private void track(TileObject object)
    {
        Course owner = course != null && course.stepOf(object.getId()) >= 0 ? course : Course.withObstacle(object.getId());
        if (owner == null)
        {
            return;
        }
        if (owner != course)
        {
            course = owner;
            recorder = new LapRecorder(owner.stepCount());
            book = loadBook(owner);
        }
        obstacles.put(object, owner.stepOf(object.getId()));
    }

    /** The click as canvas fractions: the mouse when it is on the obstacle, else the obstacle's center. */
    private Point2D.Double clickPoint(int objectId)
    {
        Point mouse = client.getMouseCanvasPosition();
        Shape clickbox = obstacles.keySet().stream()
            .filter(object -> object.getId() == objectId)
            .map(TileObject::getClickbox)
            .filter(shape -> shape != null)
            .findFirst()
            .orElse(null);
        double x;
        double y;
        if (clickbox != null && (mouse == null || !clickbox.contains(mouse.getX(), mouse.getY())))
        {
            Rectangle bounds = clickbox.getBounds();
            x = bounds.getCenterX();
            y = bounds.getCenterY();
        }
        else if (mouse != null)
        {
            x = mouse.getX();
            y = mouse.getY();
        }
        else
        {
            return null;
        }
        return new Point2D.Double(x / client.getCanvasWidth(), y / client.getCanvasHeight());
    }

    private ViewBook loadBook(Course course)
    {
        ViewBook loaded = new ViewBook(course.stepCount());
        String json = configManager.getConfiguration(RooftopLensConfig.GROUP, viewsKey(course));
        if (json == null || json.isEmpty())
        {
            return loaded;
        }
        try
        {
            Map<String, ViewBook.View> views = gson.fromJson(json, VIEWS_TYPE);
            views.forEach((key, view) ->
            {
                if (view.clicks.size() == course.stepCount())
                {
                    loaded.views.put(key, view);
                }
            });
        }
        catch (JsonSyntaxException | IllegalArgumentException ignored)
        {
            // An unreadable record is discarded; the next clean lap starts a new one.
        }
        return loaded;
    }

    private void showNotice(String text, boolean warning)
    {
        notice = text;
        noticeWarning = warning;
        noticeTicks = NOTICE_TICKS;
    }

    Course getCourse() { return course; }
    LapRecorder getRecorder() { return recorder; }
    ViewBook getBook() { return book; }
    CameraPose getCamera() { return camera; }
    Map<TileObject, Integer> getObstacles() { return obstacles; }
    String getNotice() { return notice; }
    boolean isNoticeWarning() { return noticeWarning; }

    private static String viewsKey(Course course)
    {
        return "views." + course.name();
    }
}
