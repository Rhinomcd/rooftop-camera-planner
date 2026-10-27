package com.rhinomcd.rooftoplens;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.mockito.Mockito;

/**
 * Feeds a {@link LensRecorderPlugin} recording through the real plugin, with a mocked RuneLite client standing in
 * for the game. Obstacles are replayed as game objects whose clickboxes are the recorded bounds.
 */
final class Replay
{
    private static final Gson GSON = new Gson();
    final RooftopLensPlugin plugin;
    final List<String> notices = new ArrayList<>();
    private final Client client = mock(Client.class);
    private final Map<String, String> config = new HashMap<>();
    private final Map<Integer, GameObject> objects = new HashMap<>();
    private final Map<Integer, Rectangle> boxes = new HashMap<>();
    private final ObstacleOverlay obstacleOverlay;
    private final LensPanel lensPanel;
    private JsonObject camera = new JsonObject();
    private int mouseX = -1;
    private int mouseY = -1;
    private GameState state = GameState.LOGGED_IN;
    private int frames;
    private String lastNotice;

    Replay()
    {
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getConfiguration(Mockito.eq(RooftopLensConfig.GROUP), Mockito.anyString()))
            .thenAnswer(call -> config.get(call.getArgument(1)));
        doAnswer(call -> config.put(call.getArgument(1), call.getArgument(2)))
            .when(configManager).setConfiguration(Mockito.eq(RooftopLensConfig.GROUP), Mockito.anyString(), Mockito.anyString());
        ClientThread clientThread = mock(ClientThread.class);
        doAnswer(call -> { ((Runnable) call.getArgument(0)).run(); return null; }).when(clientThread).invoke(Mockito.any(Runnable.class));
        RooftopLensConfig lensConfig = mock(RooftopLensConfig.class, Mockito.CALLS_REAL_METHODS);

        WorldView worldView = mock(WorldView.class);
        when(client.getTopLevelWorldView()).thenReturn(worldView);
        when(worldView.getPlane()).thenAnswer(call -> intOf("plane", 0));
        when(worldView.getScene()).thenReturn(null);
        when(client.getGameState()).thenAnswer(call -> state);
        when(client.getCameraYawTarget()).thenAnswer(call -> intOf("yaw", 0));
        when(client.getCameraPitchTarget()).thenAnswer(call -> intOf("pitch", 0));
        when(client.isResized()).thenAnswer(call -> !camera.has("resized") || camera.get("resized").getAsBoolean());
        when(client.getVarcIntValue(anyInt())).thenAnswer(call -> intOf("zoom", 0));
        when(client.getCanvasWidth()).thenAnswer(call -> intOf("cw", 765));
        when(client.getCanvasHeight()).thenAnswer(call -> intOf("ch", 503));
        when(client.getMouseCanvasPosition()).thenAnswer(call -> new Point(mouseX, mouseY));

        plugin = new RooftopLensPlugin();
        Injector injector = Guice.createInjector(binder ->
        {
            binder.bind(RooftopLensPlugin.class).toInstance(plugin);
            binder.bind(Client.class).toInstance(client);
            binder.bind(ConfigManager.class).toInstance(configManager);
            binder.bind(ClientThread.class).toInstance(clientThread);
            binder.bind(OverlayManager.class).toInstance(mock(OverlayManager.class));
            binder.bind(RooftopLensConfig.class).toInstance(lensConfig);
            binder.bind(Gson.class).toInstance(new Gson());
        });
        obstacleOverlay = injector.getInstance(ObstacleOverlay.class);
        lensPanel = injector.getInstance(LensPanel.class);
    }

    /** Replays every line; renders a frame after each obstacle click when {@code frameDir} is set. */
    Replay run(Path recording, Path frameDir) throws IOException
    {
        for (String text : Files.readAllLines(recording, StandardCharsets.UTF_8))
        {
            if (text.isBlank())
            {
                continue;
            }
            JsonObject line = GSON.fromJson(text, JsonObject.class);
            switch (line.get("t").getAsString())
            {
                case "state":
                    state = GameState.valueOf(line.get("v").getAsString());
                    GameStateChanged changed = new GameStateChanged();
                    changed.setGameState(state);
                    plugin.onGameStateChanged(changed);
                    break;
                case "cam":
                    camera = line;
                    plugin.onClientTick(new ClientTick());
                    break;
                case "tick":
                    plugin.onGameTick(new GameTick());
                    noteNotice();
                    break;
                case "spawn":
                    spawn(line);
                    break;
                case "despawn":
                    GameObjectDespawned despawned = new GameObjectDespawned();
                    despawned.setGameObject(objects.remove(line.get("k").getAsInt()));
                    plugin.onGameObjectDespawned(despawned);
                    break;
                case "boxes":
                    boxes.clear();
                    for (Map.Entry<String, JsonElement> box : line.getAsJsonObject("b").entrySet())
                    {
                        int[] b = GSON.fromJson(box.getValue(), int[].class);
                        boxes.put(Integer.parseInt(box.getKey()), new Rectangle(b[0], b[1], b[2], b[3]));
                    }
                    break;
                case "click":
                    mouseX = line.get("mx").getAsInt();
                    mouseY = line.get("my").getAsInt();
                    MenuEntry entry = mock(MenuEntry.class);
                    when(entry.getIdentifier()).thenReturn(line.get("id").getAsInt());
                    when(entry.getType()).thenReturn(MenuAction.valueOf(line.get("action").getAsString()));
                    plugin.onMenuOptionClicked(new MenuOptionClicked(entry));
                    noteNotice();
                    if (frameDir != null)
                    {
                        render(frameDir);
                    }
                    break;
                default:
                    break;
            }
        }
        return this;
    }

    int lapsLearned()
    {
        ViewBook book = plugin.getBook();
        return book == null ? 0 : book.views.values().stream().mapToInt(view -> view.laps).sum();
    }

    long noticesOf(String text)
    {
        return notices.stream().filter(text::equals).count();
    }

    private void spawn(JsonObject line)
    {
        int key = line.get("k").getAsInt();
        GameObject object = mock(GameObject.class);
        when(object.getId()).thenReturn(line.get("id").getAsInt());
        when(object.getPlane()).thenReturn(line.get("plane").getAsInt());
        when(object.getClickbox()).thenAnswer(call -> boxes.get(key));
        objects.put(key, object);
        GameObjectSpawned spawned = new GameObjectSpawned();
        spawned.setGameObject(object);
        plugin.onGameObjectSpawned(spawned);
    }

    /** Records each notice when it appears, so one shown for several ticks counts once. */
    private void noteNotice()
    {
        String notice = plugin.getNotice();
        if (notice != null && !notice.equals(lastNotice))
        {
            notices.add(notice);
        }
        lastNotice = notice;
    }

    private void render(Path frameDir) throws IOException
    {
        Files.createDirectories(frameDir);
        BufferedImage frame = new BufferedImage(client.getCanvasWidth(), client.getCanvasHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.setColor(new Color(86, 76, 52));
        g.fillRect(0, 0, frame.getWidth(), frame.getHeight());
        obstacleOverlay.render(g);
        Graphics2D card = (Graphics2D) g.create(8, 8, 300, 140);
        lensPanel.render(card);
        g.setColor(Color.WHITE);
        g.fillOval(mouseX - 3, mouseY - 3, 6, 6);
        ImageIO.write(frame, "png", frameDir.resolve(String.format("%03d.png", frames++)).toFile());
    }

    private int intOf(String key, int fallback)
    {
        return camera.has(key) ? camera.get(key).getAsInt() : fallback;
    }
}
