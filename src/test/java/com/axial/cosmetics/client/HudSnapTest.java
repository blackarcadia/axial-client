package com.axial.cosmetics.client;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.axial.cosmetics.client.HudSnap.*;

class HudSnapTest {
    @Test void placesElementsBesideAndBelowEachOtherWithConsistentGap() {
        var other=new Box(100,80,60,40);
        var beside=place(new Box(167,82,30,20),List.of(other),640,360,false);
        assertEquals(164,beside.x());assertEquals(80,beside.y());
        var below=place(new Box(101,127,30,20),List.of(other),640,360,false);
        assertEquals(100,below.x());assertEquals(124,below.y());
    }
    @Test void alignsRightEdgesAndCentresForDifferentSizedHuds() {
        var other=new Box(100,80,60,40);
        var right=place(new Box(129,126,30,20),List.of(other),640,360,false);
        assertEquals(130,right.x());
        var centre=place(new Box(117,126,30,20),List.of(other),640,360,false);
        assertEquals(115,centre.x());
    }
    @Test void overlapDoesNotTriggerCollisionAvoidance() {
        var box=new Box(120,95,20,10);
        var result=place(box,List.of(new Box(100,80,100,70)),640,360,false);
        assertEquals(box.x(),result.x());assertEquals(box.y(),result.y());
    }
    @Test void freePlacementBypassesEveryMagnetAndAllowsExactOverlap() {
        var box=new Box(103,82,60,40);
        var result=place(box,List.of(new Box(100,80,60,40)),640,360,true);
        assertEquals(box.x(),result.x());assertEquals(box.y(),result.y());
        assertNull(result.guideX());assertNull(result.guideY());
    }
    @Test void onlyNearbyElementsAttractAndNearestCandidateWins() {
        var distant=place(new Box(167,250,30,20),List.of(new Box(100,80,60,40)),640,360,false);
        assertEquals(167,distant.x());
        var result=place(new Box(167,82,30,20),List.of(new Box(100,80,60,40),new Box(168,80,80,40)),640,360,false);
        assertEquals(168,result.x());
    }
    @Test void screenEdgesAndCentreRemainAvailable() {
        assertEquals(0,place(new Box(4,100,30,20),List.of(),640,360,false).x());
        assertEquals(305,place(new Box(308,100,30,20),List.of(),640,360,false).x());
        assertEquals(610,place(new Box(607,100,30,20),List.of(),640,360,false).x());
        assertEquals(170,place(new Box(100,173,30,20),List.of(),640,360,false).y());
    }
    @Test void clampsAtScreenBoundsEvenForOversizedElements() {
        var result=place(new Box(-10,1000,700,400),List.of(),640,360,true);
        assertEquals(0,result.x());assertEquals(0,result.y());
    }
    @Test void outsideThresholdLeavesCoordinatesUntouched() {
        var result=place(new Box(172,90,30,20),List.of(new Box(100,80,60,40)),640,360,false);
        assertEquals(172,result.x());
    }
}
