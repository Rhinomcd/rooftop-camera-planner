package com.rhinomcd.rooftoplens;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.Tile;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Rooftop Lens",
    description = "Learns which camera view keeps your rooftop Agility clicks closest together",
    tags = {"agility", "rooftop", "camera", "marks", "route"}
)
public class RooftopLensPlugin extends Plugin
{
    private static final Type VIEWS_TYPE = new TypeToken<LinkedHashMap<String, ViewBook.View>>() { }.getType();
    private static final Logger log = LoggerFactory.getLogger(RooftopLensPlugin.class);
    private static final int NOTICE_TICKS = 10;
    private static final Set<MenuAction> OBSTACLE_ACTIONS = EnumSet.of(MenuAction.GAME_OBJECT_FIRST_OPTION,
        MenuAction.GAME_OBJECT_SECOND_OPTION, MenuAction.GAME_OBJECT_THIRD_OPTION,
        MenuAction.GAME_OBJECT_FOURTH_OPTION, MenuAction.GAME_OBJECT_FIFTH_OPTION);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
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
        clientThread.invoke(this::scanScene);
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
        switch (event.getGameState())
        {
            case LOADING:
                // Scenes reload mid-course; the lap survives and obstacles respawn.
                obstacles.clear();
                break;
            case LOGIN_SCREEN:
            case HOPPING:
                obstacles.clear();
                course = null;
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (course != null && obstacles.isEmpty())
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
        CameraPose start = recorder.inProgress() ? recorder.startView() : null;
        if (recorder.cameraSampled(camera))
        {
            lapDropped(start, camera);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        int step = course == null || !OBSTACLE_ACTIONS.contains(event.getMenuAction()) ? -1 : course.stepOf(event.getId());
        if (step < 0)
        {
            return;
        }
        Point2D.Double click = clickPoint(event.getId());
        if (click == null)
        {
            return;
        }
        CameraPose clicked = CameraPose.read(client);
        CameraPose start = recorder.inProgress() ? recorder.startView() : null;
        switch (recorder.obstacleClicked(step, click, clicked))
        {
            case COMPLETED:
                book.record(recorder.lastLap(), client.getCanvasWidth(), client.getCanvasHeight());
                configManager.setConfiguration(RooftopLensConfig.GROUP, viewsKey(course), gson.toJson(book.views));
                log.info("Lap learned on {} in view {}", course, recorder.lastLap().camera.viewKey());
                showNotice("Lap learned", false);
                break;
            case CAMERA_MOVED:
                lapDropped(start, clicked);
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

    private void scanScene()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        for (Tile[][] plane : client.getTopLevelWorldView().getScene().getTiles())
        {
            for (Tile[] row : plane)
            {
                for (Tile tile : row)
                {
                    if (tile == null)
                    {
                        continue;
                    }
                    for (GameObject object : tile.getGameObjects())
                    {
                        trackIfPresent(object);
                    }
                    trackIfPresent(tile.getWallObject());
                    trackIfPresent(tile.getDecorativeObject());
                    trackIfPresent(tile.getGroundObject());
                }
            }
        }
    }

    private void trackIfPresent(TileObject object)
    {
        if (object != null)
        {
            track(object);
        }
    }

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
            if (views == null)
            {
                return loaded;
            }
            views.forEach((key, view) ->
            {
                if (view != null && view.canvasWidth > 0 && view.canvasHeight > 0 && view.clicks != null
                    && view.clicks.size() == course.stepCount()
                    && view.clicks.stream().allMatch(step -> step != null && !step.isEmpty()
                        && step.stream().allMatch(click -> click != null && click.length == 2)))
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

    private void lapDropped(CameraPose start, CameraPose now)
    {
        log.info("Lap dropped on {}: started {}/{}/{} now {}/{}/{} focused={}", course, start.yaw, start.pitch,
            start.zoom, now.yaw, now.pitch, now.zoom, client.getCanvas().isFocusOwner());
        showNotice("Camera moved, lap dropped", true);
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
