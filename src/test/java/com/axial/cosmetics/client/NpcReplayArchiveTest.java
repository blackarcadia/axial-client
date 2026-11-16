package com.axial.cosmetics.client;

import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NpcReplayArchiveTest {
    @TempDir Path dir;
    @Test void nativeReplayArchiveRetainsActorTrackAndRecordingAcrossSaveAndReopen()throws Exception {
        try(var loader=new URLClassLoader(new java.net.URL[]{Path.of("src/main/resources/replaymod-1.21.11-2.6.28.jar").toUri().toURL()},getClass().getClassLoader())) {
            var studio=loader.loadClass("com.replaymod.replaystudio.studio.ReplayStudio").getConstructor().newInstance();
            var type=loader.loadClass("com.replaymod.replaystudio.replay.ZipReplayFile");
            var constructor=type.getConstructor(loader.loadClass("com.replaymod.replaystudio.Studio"),java.io.File.class);
            var path=dir.resolve("filming.mcpr");
            try(var zip=new ZipOutputStream(Files.newOutputStream(path))){zip.putNextEntry(new ZipEntry("recording.tmcpr"));zip.write(new byte[]{1,2,3});zip.closeEntry();}
            var file=constructor.newInstance(studio,path.toFile());assertEquals(0,NpcReplayArchive.read(file).length);
            var queue=new ArrayList<Runnable>();var writer=new NpcReplayArchive.Writer(queue::add,file);
            var t=new NpcEmoteTimeline();var clip=new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),"Dance","AQID",40);var actor=UUID.randomUUID();
            t.addClip(clip);t.addAll(List.of(new NpcEmoteTimeline.Cue(500,actor,clip.id(),true,0)));
            writer.submit(t.encode());t.addAll(List.of(new NpcEmoteTimeline.Cue(1500,actor,"",false,0)));var expected=t.encode();writer.submit(expected);
            assertEquals(1,queue.size());queue.removeFirst().run();assertNull(writer.failure());
            assertArrayEquals(expected,NpcReplayArchive.read(file));
            type.getMethod("save").invoke(file);type.getMethod("close").invoke(file);
            var reopened=constructor.newInstance(studio,path.toFile());
            try {var restored=NpcEmoteTimeline.decode(NpcReplayArchive.read(reopened));assertEquals(10,restored.pose(actor,1000).ticks());assertNull(restored.pose(actor,1500));}
            finally{type.getMethod("close").invoke(reopened);}
            try(var zip=new ZipFile(path.toFile())){assertArrayEquals(new byte[]{1,2,3},zip.getInputStream(zip.getEntry("recording.tmcpr")).readAllBytes());}
        }
    }
    @Test void rejectedSaveQueueReportsFailure(){var writer=new NpcReplayArchive.Writer(r->{throw new RejectedExecutionException();},new Object());assertThrows(RejectedExecutionException.class,()->writer.submit(new byte[]{1}));assertNotNull(writer.failure());}
}
