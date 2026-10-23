package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class BulletinCarouselTest {
    @Test void holdsForSixSecondsThenCrossfadesAndLoopsAllThreePages() {
        var clock=new AtomicLong();var c=new BulletinCarousel(3,clock::get);
        clock.set(BulletinCarousel.HOLD_NANOS-1);c.tick(false);
        assertEquals(0,c.current());assertEquals(0,c.progress());
        clock.incrementAndGet();c.tick(false);assertEquals(1,c.target());assertEquals(0,c.progress());
        clock.addAndGet(BulletinCarousel.FADE_NANOS/2);c.tick(false);assertEquals(.5f,c.progress(),.0001f);
        clock.addAndGet(BulletinCarousel.FADE_NANOS/2);c.tick(false);assertEquals(1,c.current());assertEquals(0,c.progress());
        for(int expected:new int[]{2,0,1}) {
            clock.addAndGet(BulletinCarousel.HOLD_NANOS+BulletinCarousel.FADE_NANOS);c.tick(false);assertEquals(expected,c.current());
        }
    }
    @Test void hoverPausesAutomaticCyclingButManualNavigationStillAnimates() {
        var clock=new AtomicLong();var c=new BulletinCarousel(3,clock::get);
        clock.set(100_000_000_000L);c.tick(true);assertEquals(0,c.current());
        c.step(-1);assertEquals(2,c.target());assertEquals(0,c.progress());
        clock.addAndGet(BulletinCarousel.FADE_NANOS/2);c.tick(true);assertEquals(.5f,c.progress(),.0001f);
        clock.addAndGet(BulletinCarousel.FADE_NANOS);c.tick(true);assertEquals(2,c.current());
        clock.addAndGet(100_000_000_000L);c.tick(true);assertEquals(2,c.current());
        c.step(1);assertEquals(0,c.target());
    }
    @Test void resizingOrReturningFromAnotherScreenDoesNotSkipBulletins() {
        var clock=new AtomicLong();var c=new BulletinCarousel(3,clock::get);
        clock.set(3_000_000_000L);c.tick(false);
        clock.set(500_000_000_000L);c.synchronizeClock();c.tick(false);
        assertEquals(0,c.selected());
        clock.addAndGet(3_000_000_000L);c.tick(false);assertEquals(1,c.target());
    }
    @Test void timingIsIndependentOfRenderFrameRate() {
        var aClock=new AtomicLong();var bClock=new AtomicLong();
        var a=new BulletinCarousel(3,aClock::get);var b=new BulletinCarousel(3,bClock::get);
        for(int i=0;i<241;i++){aClock.addAndGet(100_000_000L);a.tick(false);}
        bClock.set(aClock.get());b.tick(false);
        assertEquals(a.current(),b.current());assertEquals(a.target(),b.target());assertEquals(a.progress(),b.progress(),.0001f);
    }
    @Test void singleImageNeverCyclesOrFades() {
        var clock=new AtomicLong();var c=new BulletinCarousel(1,clock::get);
        clock.set(1_000_000_000_000L);c.tick(false);c.step(1);
        assertEquals(0,c.current());assertEquals(0,c.target());assertEquals(0,c.progress());
    }
}
