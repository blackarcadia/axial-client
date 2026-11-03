package com.axial.cosmetics.client;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiMotionTest {
    @Test void easingIsIndependentOfFrameRateAndNeverOvershoots() {
        var clock=new AtomicLong();var many=new UiMotion(0,22,clock::get);
        double previous=0;
        for(int i=1;i<=10;i++) {
            clock.set(i*10_000_000L);double next=many.toward(100);
            assertTrue(next>=previous&&next<=100);previous=next;
        }
        var otherClock=new AtomicLong();var once=new UiMotion(0,22,otherClock::get);
        otherClock.set(100_000_000L);assertEquals(previous,once.toward(100),1e-9);
        clock.addAndGet(16_000_000);double reversed=many.toward(0);
        assertTrue(reversed<previous&&reversed>0);
        clock.addAndGet(5_000_000_000L);assertEquals(0,many.toward(0));
    }
    @Test void wheelBurstsAccumulateAndDirectionCanReverseBeforeSettling() {
        var clock=new AtomicLong();var scroll=new SmoothScroll(clock::get);
        scroll.to(30,500);scroll.to(scroll.target()+30,500);scroll.to(scroll.target()+30,500);
        assertEquals(90,scroll.target());assertEquals(0,scroll.value(500));
        clock.addAndGet(50_000_000);int halfway=scroll.value(500);
        assertTrue(halfway>0&&halfway<90);
        scroll.to(0,500);clock.addAndGet(20_000_000);
        assertTrue(scroll.value(500)<halfway);
    }
    @Test void draggingIsImmediateAndShrinkingContentClampsBothDestinationAndDisplay() {
        var clock=new AtomicLong();var scroll=new SmoothScroll(clock::get);
        scroll.jump(480,500);assertEquals(480,scroll.value(500));
        assertEquals(60,scroll.value(60));assertEquals(60,scroll.target());
        scroll.jump(-20,60);assertEquals(0,scroll.value(60));
        scroll.to(1000,60);assertEquals(60,scroll.target());
        assertEquals(0,scroll.value(0));assertEquals(0,scroll.target());
    }
}
