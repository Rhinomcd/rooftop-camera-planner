package com.rhinomcd.rooftoplens;

import java.nio.file.Path;
import java.nio.file.Paths;

/** {@code ./gradlew replay -Precording=<file>}: replays a recording, renders a frame per obstacle click, prints a summary. */
public class ReplayMain
{
    public static void main(String[] args) throws Exception
    {
        if (args.length < 2 || args[0].isEmpty())
        {
            throw new IllegalArgumentException("Pass -Precording=<file.jsonl>");
        }
        Path recording = Paths.get(args[0]);
        String name = recording.getFileName().toString().replaceFirst("\\.jsonl$", "");
        Path frames = Paths.get(args[1]).resolve(name);
        Replay replay = new Replay().run(recording, frames);
        System.out.println("course=" + replay.plugin.getCourse() + " lapsLearned=" + replay.lapsLearned()
            + " views=" + (replay.plugin.getBook() == null ? 0 : replay.plugin.getBook().views.keySet())
            + " notices=" + replay.notices + " frames=" + frames);
    }
}
