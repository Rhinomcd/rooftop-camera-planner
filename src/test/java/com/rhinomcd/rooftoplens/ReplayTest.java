package com.rhinomcd.rooftoplens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

/** Replays every recording in src/test/resources/recordings and checks the expect line at its top. */
public class ReplayTest
{
    private static final Gson GSON = new Gson();
    @Test
    public void everyRecordingMeetsItsExpectations() throws IOException, URISyntaxException
    {
        Path dir = Paths.get(ReplayTest.class.getResource("/recordings").toURI());
        List<Path> recordings;
        try (Stream<Path> files = Files.list(dir))
        {
            recordings = files.filter(file -> file.toString().endsWith(".jsonl")).sorted().collect(Collectors.toList());
        }
        assertFalse(recordings.isEmpty());
        for (Path recording : recordings)
        {
            JsonObject expect = GSON.fromJson(Files.readAllLines(recording).get(0), JsonObject.class);
            Replay replay = new Replay().run(recording, null);
            String name = recording.getFileName().toString();
            assertEquals(name + " laps learned", expect.get("laps").getAsInt(), replay.lapsLearned());
            assertEquals(name + " laps dropped", expect.get("drops").getAsLong(), replay.noticesOf("Camera moved, lap dropped"));
        }
    }
}
