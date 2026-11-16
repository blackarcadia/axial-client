package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcReplayProcessingTest {
    final UUID actor=UUID.randomUUID();
    final NpcEmoteTimeline.Clip clip=new NpcEmoteTimeline.Clip(UUID.randomUUID().toString(),"Dance","AQID",60);
    NpcEmoteTimeline source(){var t=new NpcEmoteTimeline();t.addClip(clip);t.addAll(List.of(new NpcEmoteTimeline.Cue(1000,actor,clip.id(),true,0)));return t;}
    NpcReplayProcessing.Marker marker(long t,String n){return new NpcReplayProcessing.Marker(t,"_RM_"+n);}
    @Test void recordingStopCutAndSplitPreserveThePerformance() {
        var outputs=NpcReplayProcessing.plan(List.of(marker(28929,"SPLIT"),marker(28928,"START_CUT")),30486);
        assertEquals(List.of(List.of(new NpcReplayProcessing.Span(0,28928))),outputs);
        var saved=NpcEmoteTimeline.decode(NpcReplayProcessing.retime(source(),outputs.getFirst()).encode());
        assertEquals(source().pose(actor,15028).ticks(),saved.pose(actor,15028).ticks());
    }
    @Test void trimmingTheBeginningSeedsAnEmoteAlreadyInProgress() {
        var outputs=NpcReplayProcessing.plan(List.of(marker(0,"START_CUT"),marker(2350,"END_CUT")),8000);
        var saved=NpcReplayProcessing.retime(source(),outputs.getFirst());
        assertEquals(27,saved.pose(actor,0).ticks());assertEquals(47,saved.pose(actor,1000).ticks());
    }
    @Test void middleCutsRetainOriginalPhaseAndAccountForRemovedStopCues() {
        var t=source();t.addAll(List.of(new NpcEmoteTimeline.Cue(4000,actor,"",false,0)));
        var outputs=NpcReplayProcessing.plan(List.of(marker(2500,"START_CUT"),marker(5000,"END_CUT")),8000);
        var saved=NpcReplayProcessing.retime(t,outputs.getFirst());
        assertNotNull(saved.pose(actor,2499));assertNull(saved.pose(actor,2500));
        assertNotNull(saved.pose(actor,2000)); // backwards seek across the cut
    }
    @Test void splitStartsEachOutputWithTheCorrectActorPose() {
        var outputs=NpcReplayProcessing.plan(List.of(marker(2500,"SPLIT")),8000);
        assertEquals(2,outputs.size());
        assertEquals(30,NpcReplayProcessing.retime(source(),outputs.get(1)).pose(actor,0).ticks());
        assertNull(NpcReplayProcessing.retime(source(),outputs.getFirst()).pose(actor,999));
    }
    @Test void splitInsideCutDoesNotProduceAnEmptyTrailingOutput() {
        var outputs=NpcReplayProcessing.plan(List.of(marker(2000,"START_CUT"),marker(3000,"SPLIT"),marker(5000,"END_CUT")),8000);
        assertEquals(List.of(List.of(new NpcReplayProcessing.Span(0,2000)),List.of(new NpcReplayProcessing.Span(5000,8000))),outputs);
        assertEquals(List.of(),NpcReplayProcessing.plan(List.of(marker(0,"START_CUT")),8000));
    }
}
