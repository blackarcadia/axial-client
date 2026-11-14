package com.axial.cosmetics.client;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuraSpacingTest {
    private void clearBody(List<HexwindGeometry.Quad> mesh,double clearance){
        for(var q:mesh)for(var p:List.of(q.a(),q.b(),q.c(),q.d()))assertTrue(Math.hypot(p.x(),p.z())>clearance,"Aura geometry crowds the torso: "+p);
    }
    @Test void orbitingEffectsLeaveSpaceAroundTheTorso(){
        for(float t=0;t<120;t+=3){
            clearBody(HexwindGeometry.frame(t,true),1.0);
            clearBody(GoldSparkGeometry.frame(t,true),.70);
            clearBody(PixelSnowflakesGeometry.frame(t,true),.70);
        }
    }
    @Test void starBirthsStartOutsideTheBodyWithoutChangingTheirWorldSpaceTrail(){
        var e=new PixelStarsEmitter(new UUID(20,21));e.tick(0,0,0,0);e.tick(1,.1,0,0);
        assertFalse(e.stars().isEmpty());
        for(var s:e.stars())assertTrue(Math.hypot(s.x()-.05,s.z())>.75);
        var first=e.stars().getFirst();e.tick(2,.2,0,0);assertEquals(first,e.stars().getFirst());
    }
}
