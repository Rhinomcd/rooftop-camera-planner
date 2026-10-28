package com.rhinomcd.rooftoplens;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Skill;
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
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.gameval.VarClientID;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development-only recorder, loaded by {@code ./gradlew run} and never shipped. It observes what Rooftop Lens
 * observes while you play by hand, and writes it as JSON lines that {@link Replay} feeds back through the plugin.
 * It sends no input to the game.
 */
@PluginDescriptor(name = "Rooftop Lens Recorder", description = "Records Rooftop Lens inputs for replay tests")
public class LensRecorderPlugin extends Plugin
{
    static final Path ROOT = RuneLite.RUNELITE_DIR.toPath().resolve("rooftop-lens");
    static final Path DIR = ROOT.resolve("recordings");
    /** Touch this file and the recorder saves the next rendered game frame to {@link #SNAPS}. */
    static final Path SNAP_REQUEST = ROOT.resolve("snap-request");
    static final Path SNAPS = ROOT.resolve("snaps");
    private static final Logger log = LoggerFactory.getLogger(LensRecorderPlugin.class);

    @Inject private Client client;
    @Inject private Gson gson;
    @Inject private DrawManager drawManager;

    private final Map<TileObject, Integer> keys = new IdentityHashMap<>();
    private BufferedWriter out;
    private String lastCamera;
    private int nextKey;

    @Override
    protected void startUp() throws IOException
    {
        Files.createDirectories(DIR);
        Path file = DIR.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".jsonl");
        out = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        log.info("Rooftop Lens recorder writing {}", file);
    }

    @Override
    protected void shutDown() throws IOException
    {
        out.close();
        keys.clear();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        JsonObject line = line("state");
        line.addProperty("v", event.getGameState().name());
        write(line);
        flush();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        JsonObject line = line("cam");
        line.addProperty("yaw", client.getCameraYawTarget());
        line.addProperty("pitch", client.getCameraPitchTarget());
        line.addProperty("resized", client.isResized());
        line.addProperty("zoom", client.getVarcIntValue(client.isResized() ? VarClientID.CAMERA_ZOOM_BIG : VarClientID.CAMERA_ZOOM_SMALL));
        line.addProperty("scale", client.getScale());
        line.addProperty("vx", client.getViewportXOffset());
        line.addProperty("vy", client.getViewportYOffset());
        line.addProperty("vw", client.getViewportWidth());
        line.addProperty("vh", client.getViewportHeight());
        line.addProperty("cw", client.getCanvasWidth());
        line.addProperty("ch", client.getCanvasHeight());
        line.addProperty("plane", client.getTopLevelWorldView() == null ? 0 : client.getTopLevelWorldView().getPlane());
        String camera = line.toString();
        if (!camera.equals(lastCamera))
        {
            lastCamera = camera;
            write(line);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        writeBoxes();
        write(line("tick"));
        flush();
        if (Files.exists(SNAP_REQUEST))
        {
            snap();
        }
    }

    private void snap()
    {
        try
        {
            Files.delete(SNAP_REQUEST);
            Files.createDirectories(SNAPS);
        }
        catch (IOException e)
        {
            log.warn("Rooftop Lens snapshot request failed", e);
            return;
        }
        Path file = SNAPS.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png");
        drawManager.requestNextFrameListener(image ->
        {
            BufferedImage frame = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            frame.getGraphics().drawImage(image, 0, 0, null);
            try
            {
                ImageIO.write(frame, "png", file.toFile());
                log.info("Rooftop Lens snapshot saved {}", file);
            }
            catch (IOException e)
            {
                log.warn("Rooftop Lens snapshot write failed", e);
            }
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        if (Course.withObstacle(event.getId()) == null)
        {
            return;
        }
        writeBoxes();
        Point mouse = client.getMouseCanvasPosition();
        JsonObject line = line("click");
        line.addProperty("action", event.getMenuAction().name());
        line.addProperty("id", event.getId());
        line.addProperty("mx", mouse == null ? -1 : mouse.getX());
        line.addProperty("my", mouse == null ? -1 : mouse.getY());
        line.addProperty("fx", client.getCameraFocalPointX());
        line.addProperty("fy", client.getCameraFocalPointY());
        line.addProperty("fz", client.getCameraFocalPointZ());
        write(line);
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (event.getSkill() == Skill.AGILITY)
        {
            JsonObject line = line("xp");
            line.addProperty("v", event.getXp());
            write(line);
        }
    }

    @Subscribe public void onGameObjectSpawned(GameObjectSpawned e) { spawned(e.getGameObject()); }
    @Subscribe public void onGameObjectDespawned(GameObjectDespawned e) { despawned(e.getGameObject()); }
    @Subscribe public void onWallObjectSpawned(WallObjectSpawned e) { spawned(e.getWallObject()); }
    @Subscribe public void onWallObjectDespawned(WallObjectDespawned e) { despawned(e.getWallObject()); }
    @Subscribe public void onDecorativeObjectSpawned(DecorativeObjectSpawned e) { spawned(e.getDecorativeObject()); }
    @Subscribe public void onDecorativeObjectDespawned(DecorativeObjectDespawned e) { despawned(e.getDecorativeObject()); }
    @Subscribe public void onGroundObjectSpawned(GroundObjectSpawned e) { spawned(e.getGroundObject()); }
    @Subscribe public void onGroundObjectDespawned(GroundObjectDespawned e) { despawned(e.getGroundObject()); }

    private void spawned(TileObject object)
    {
        if (Course.withObstacle(object.getId()) == null)
        {
            return;
        }
        int key = nextKey++;
        keys.put(object, key);
        JsonObject line = line("spawn");
        line.addProperty("k", key);
        line.addProperty("id", object.getId());
        line.addProperty("plane", object.getPlane());
        line.addProperty("lx", object.getLocalLocation().getX());
        line.addProperty("ly", object.getLocalLocation().getY());
        line.addProperty("z", object.getZ());
        write(line);
    }

    private void despawned(TileObject object)
    {
        Integer key = keys.remove(object);
        if (key != null)
        {
            JsonObject line = line("despawn");
            line.addProperty("k", key);
            write(line);
        }
    }

    /** Clickbox bounds of every tracked obstacle on screen right now; off-screen obstacles are omitted. */
    private void writeBoxes()
    {
        JsonObject boxes = new JsonObject();
        keys.forEach((object, key) ->
        {
            Shape clickbox = object.getClickbox();
            if (clickbox != null)
            {
                Rectangle b = clickbox.getBounds();
                boxes.add(String.valueOf(key), gson.toJsonTree(new int[] { b.x, b.y, b.width, b.height }));
            }
        });
        JsonObject line = line("boxes");
        line.add("b", boxes);
        write(line);
    }

    private static JsonObject line(String type)
    {
        JsonObject line = new JsonObject();
        line.addProperty("t", type);
        return line;
    }

    private void write(JsonObject line)
    {
        try
        {
            out.write(line.toString());
            out.newLine();
        }
        catch (IOException e)
        {
            log.warn("Rooftop Lens recorder write failed", e);
        }
    }

    private void flush()
    {
        try
        {
            out.flush();
        }
        catch (IOException e)
        {
            log.warn("Rooftop Lens recorder flush failed", e);
        }
    }
}
