package com.rhinomcd.rooftoplens;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
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
import net.runelite.api.events.StatChanged;
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
    private static final Type OFFSETS_TYPE = new TypeToken<List<List<double[]>>>() { }.getType();
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
    private int agilityXp = -1;

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
                agilityXp = -1;
                obstacles.clear();
                course = null;
                break;
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
        if (course != null && obstacles.isEmpty() && client.getGameState() == GameState.LOGGED_IN)
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
        LapRecorder.Click click = clickPoint(event.getId());
        if (click == null)
        {
            return;
        }
        CameraPose clicked = CameraPose.read(client);
        CameraPose start = recorder.inProgress() ? recorder.startView() : null;
        if (recorder.obstacleClicked(step, click, clicked) == LapRecorder.Outcome.CAMERA_MOVED)
        {
            lapDropped(start, clicked);
        }
    }

    /** An Agility XP drop means the obstacle in progress completed. */
    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() != Skill.AGILITY)
        {
            return;
        }
        boolean gained = agilityXp >= 0 && event.getXp() > agilityXp;
        agilityXp = event.getXp();
        if (gained && course != null && recorder.agilityXpGained() == LapRecorder.Outcome.COMPLETED)
        {
            book.record(recorder.lastLap());
            configManager.setConfiguration(RooftopLensConfig.GROUP, viewsKey(course), gson.toJson(book.views));
            configManager.setConfiguration(RooftopLensConfig.GROUP, spotsKey(course), gson.toJson(book.focalOffsets));
            log.info("Lap learned on {} in view {}", course, recorder.lastLap().camera.viewKey());
            showNotice("Lap learned", false);
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
            obstacles.clear();
            course = owner;
            recorder = new LapRecorder(owner.stepCount());
            book = loadBook(owner);
        }
        obstacles.put(object, owner.stepOf(object.getId()));
    }

    /**
     * The center of the clicked obstacle's clickbox, or the mouse if no clickbox is on screen. The center, not the
     * mouse, so the learned spot marks where the obstacle sits when it is clicked rather than which edge was hit.
     */
    private LapRecorder.Click clickPoint(int objectId)
    {
        Point mouse = client.getMouseCanvasPosition();
        List<TileObject> candidates = obstacles.keySet().stream()
            .filter(object -> object.getId() == objectId && object.getClickbox() != null)
            .collect(Collectors.toList());
        TileObject clicked = candidates.stream()
            .filter(object -> mouse != null && object.getClickbox().contains(mouse.getX(), mouse.getY()))
            .findFirst()
            .orElse(candidates.isEmpty() ? null : candidates.get(0));
        if (clicked == null)
        {
            if (mouse == null)
            {
                return null;
            }
            Point2D.Double lens = Lens.fromScreen(client, mouse.getX(), mouse.getY());
            return lens == null ? null : new LapRecorder.Click(lens, null);
        }
        Rectangle bounds = clicked.getClickbox().getBounds();
        Point2D.Double lens = Lens.fromScreen(client, bounds.getCenterX(), bounds.getCenterY());
        LocalPoint at = clicked.getLocalLocation();
        double[] focalOffset = { client.getCameraFocalPointX() - at.getX(), client.getCameraFocalPointY() - at.getY(),
            client.getCameraFocalPointZ() - clicked.getZ() };
        return lens == null ? null : new LapRecorder.Click(lens, focalOffset);
    }

    private ViewBook loadBook(Course course)
    {
        ViewBook loaded = new ViewBook(course.stepCount());
        loadFocalOffsets(loaded, course);
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
                if (view != null && reachableViewKey(key) && validSteps(view.clicks, course.stepCount(), 2))
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

    private void loadFocalOffsets(ViewBook book, Course course)
    {
        String json = configManager.getConfiguration(RooftopLensConfig.GROUP, spotsKey(course));
        if (json == null || json.isEmpty())
        {
            return;
        }
        try
        {
            List<List<double[]>> offsets = gson.fromJson(json, OFFSETS_TYPE);
            if (validSteps(offsets, course.stepCount(), 3))
            {
                book.focalOffsets = offsets;
            }
        }
        catch (JsonSyntaxException ignored)
        {
            // An unreadable record is discarded; the next clean lap starts a new one.
        }
    }

    private static boolean reachableViewKey(String key)
    {
        String[] parts = key.split(":");
        return parts.length == 2 && Arrays.stream(parts).allMatch(part -> part.matches("-?\\d+"))
            && CameraPose.fromViewKey(key).withinStandardPitch();
    }

    private static boolean validSteps(List<List<double[]>> steps, int count, int axes)
    {
        return steps != null && steps.size() == count && steps.stream().allMatch(step -> step != null
            && step.stream().allMatch(click -> click != null && click.length == axes));
    }

    private void lapDropped(CameraPose start, CameraPose now)
    {
        log.info("Lap dropped on {}: started {}/{} now {}/{}", course, start.yaw, start.pitch, now.yaw, now.pitch);
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

    private static String spotsKey(Course course)
    {
        return "spots." + course.name();
    }
}
