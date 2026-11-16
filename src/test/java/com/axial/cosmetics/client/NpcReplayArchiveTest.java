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
    @Test void processedArchiveMissingExtraEntriesGetsItsRetimedTrackBack()throws Exception {
        var raw=dir.resolve("raw.mcpr");var output=dir.resolve("processed.mcpr");
        var t=new NpcEmoteTimeline();var clip=new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),"Dance","AQID",60);var actor=UUID.randomUUID();
        t.addClip(clip);t.addAll(List.of(new NpcEmoteTimeline.Cue(15028,actor,clip.id(),true,0)));
        try(var zip=new ZipOutputStream(Files.newOutputStream(raw))) {
            for(var entry:Map.of(NpcReplayArchive.ENTRY,t.encode(),"metaData.json","{\"duration\":30486}".getBytes(),"markers.json","[{\"realTimestamp\":28928,\"value\":{\"name\":\"_RM_START_CUT\"}},{\"realTimestamp\":28929,\"value\":{\"name\":\"_RM_SPLIT\"}}]".getBytes()).entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();
            }
        }
        try(var zip=new ZipOutputStream(Files.newOutputStream(output))){zip.putNextEntry(new ZipEntry("recording.tmcpr"));zip.write(new byte[]{4,5,6});zip.closeEntry();}
        var snapshot=NpcReplayProcessing.capture(raw);assertNotNull(snapshot);
        try(var loader=new URLClassLoader(new java.net.URL[]{Path.of("src/main/resources/replaymod-1.21.11-2.6.28.jar").toUri().toURL()},getClass().getClassLoader())) {
            var studio=loader.loadClass("com.replaymod.replaystudio.studio.ReplayStudio").getConstructor().newInstance();
            var constructor=loader.loadClass("com.replaymod.replaystudio.replay.ZipReplayFile").getConstructor(loader.loadClass("com.replaymod.replaystudio.Studio"),java.io.File.class);
            var pair=loader.loadClass("com.replaymod.lib.org.apache.commons.lang3.tuple.Pair").getMethod("of",Object.class,Object.class).invoke(null,output,null);
            snapshot.finish(List.of(pair),path->{try{return constructor.newInstance(studio,path.toFile());}catch(Exception ex){throw new RuntimeException(ex);}});
            var file=constructor.newInstance(studio,output.toFile());
            try {assertEquals(20,NpcEmoteTimeline.decode(NpcReplayArchive.read(file)).pose(actor,16028).ticks());}
            finally{NpcReplayBridge.fileCall(file,"close");}
        }
        try(var zip=new ZipFile(output.toFile())){assertArrayEquals(new byte[]{4,5,6},zip.getInputStream(zip.getEntry("recording.tmcpr")).readAllBytes());}
    }
    @Test void rejectedSaveQueueReportsFailure(){var writer=new NpcReplayArchive.Writer(r->{throw new RejectedExecutionException();},new Object());assertThrows(RejectedExecutionException.class,()->writer.submit(new byte[]{1}));assertNotNull(writer.failure());}
}
