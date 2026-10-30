package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IslandXpAnimationTest {
    @Test void repeatedGainsKeepPoseContinuousAndAccumulateTheActualXp() {
        var animation=new IslandXpAnimation();animation.show(100,0);
        var before=animation.frame(180);
        animation.show(250,180);
        var after=animation.frame(180);
        assertEquals(before.alpha(),after.alpha(),1e-6);
        assertEquals(before.lift(),after.lift(),1e-6);
        assertEquals(before.scale(),after.scale(),1e-6);
        assertEquals(before.amount(),after.amount(),1e-6);
        assertEquals(350,animation.frame(1000).amount(),.01);
        assertTrue(animation.frame(1200).active());
    }
    @Test void motionIsEquivalentAtDifferentFrameRatesIncludingTheFadeBoundary() {
        var sparse=new IslandXpAnimation();var frequent=new IslandXpAnimation();
        sparse.show(1250,0);frequent.show(1250,0);
        for(int time=10;time<=1400;time+=10)frequent.frame(time);
        var a=sparse.frame(1400);var b=frequent.frame(1400);
        assertEquals(a.alpha(),b.alpha(),1e-6);assertEquals(a.lift(),b.lift(),1e-5);
        assertEquals(a.scale(),b.scale(),1e-6);assertEquals(a.amount(),b.amount(),.011);
    }
    @Test void gainsDuringFadeQueueForANewPopupWithoutRevivingTheOldOne() {
        var animation=new IslandXpAnimation();animation.show(50,0);
        var before=animation.frame(1300);assertTrue(before.alpha()<.5);
        animation.show(75,1300);var after=animation.frame(1300);
        assertEquals(before.alpha(),after.alpha(),1e-6);assertEquals(before.lift(),after.lift(),1e-6);
        assertTrue(animation.frame(1500).alpha()<before.alpha());
        assertEquals(50,animation.frame(1600).amount(),.01);
        assertFalse(animation.frame(1800).active());
        assertFalse(animation.frame(1900).active());
        assertTrue(animation.frame(2000).active());
        assertEquals(75,animation.frame(2800).amount(),.01);
    }
    @Test void uninterruptedXpStillFadesOutAndStartsFreshBoundedTotals() {
        var animation=new IslandXpAnimation();
        int gaps=0;boolean wasActive=false;
        double maximum=0;
        for(int time=0;time<20_000;time+=10) {
            if(time%100==0)animation.show(10,time);
            var frame=animation.frame(time);
            if(wasActive&&!frame.active())gaps++;
            wasActive=frame.active();
            maximum=Math.max(maximum,frame.amount());
        }
        assertTrue(gaps>=5,"continuous gains must not stop the popup disappearing");
        assertTrue(maximum<=240,"the amount must reset between batches, not accumulate indefinitely");
    }
    @Test void cooldownGainsArePreservedAndDisconnectClearsTheQueue() {
        var animation=new IslandXpAnimation();animation.show(10,0);
        assertFalse(animation.frame(1800).active());
        animation.show(20,1850);animation.show(30,1900);
        assertFalse(animation.frame(1950).active());
        assertTrue(animation.frame(2000).active());
        assertEquals(50,animation.frame(2800).amount(),.01);
        animation.show(5,3200);animation.clear();
        assertFalse(animation.frame(5000).active());
    }
    @Test void completedBurstsResetAndInvalidValuesDoNotCreateNotifications() {
        var animation=new IslandXpAnimation();
        for(double bad:new double[]{0,-10,Double.NaN,Double.POSITIVE_INFINITY})animation.show(bad,0);
        assertFalse(animation.frame(100).active());
        animation.show(100,100);assertFalse(animation.frame(2100).active());
        animation.show(25,2200);assertEquals(25,animation.frame(3000).amount(),.01);
        animation.clear();assertFalse(animation.frame(3100).active());
    }
    @Test void parsesExistingServerFormatsAndRejectsNonFiniteXp() {
        assertEquals(1250,IslandXpPopup.parseAmount("1,250"));
        assertEquals(1250,IslandXpPopup.parseAmount("1.25K"));
        assertEquals(2500000,IslandXpPopup.parseAmount("2.5m"));
        for(String invalid:new String[]{"NaN","Infinity","-25","bad",null})assertEquals(0,IslandXpPopup.parseAmount(invalid));
        assertEquals("1,250",IslandXpPopup.formatAmount(1250));
    }
}
