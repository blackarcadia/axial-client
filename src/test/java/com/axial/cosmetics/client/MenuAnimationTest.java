package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class MenuAnimationTest {
    @Test void openingSettlesWithoutOvershootAtAnyFrameRate() {
        var clock=new AtomicLong();
        var animation=new MenuAnimation(clock::get);
        assertEquals(0,animation.progress());
        float previous=0;
        for(int millis:new int[]{1,16,33,70,90,140,180,500,10000}) {
            clock.set(millis*1_000_000L);
            float value=animation.progress();
            assertTrue(value>=previous&&value<=1);
            assertEquals((1-value)*10,animation.offset(),.00001);
            previous=value;
        }
        assertEquals(1,animation.progress());
    }

    @Test void closingAnUnfinishedOpeningIsContinuousAndFinishesOnce() {
        var clock=new AtomicLong();
        var animation=new MenuAnimation(clock::get);
        clock.set(60_000_000);
        float before=animation.progress();
        animation.close();
        assertEquals(before,animation.progress());
        assertTrue(animation.closing());
        clock.addAndGet(70_000_000);
        float halfway=animation.progress();
        assertTrue(halfway<before&&halfway>0);
        animation.close(); // Repeated Escape must not restart the timer.
        clock.addAndGet(70_000_000);
        assertTrue(animation.finished());
        assertEquals(0,animation.progress());
    }

    @Test void hoverSpeedDoesNotDependOnFrameCount() {
        float twoFrames=MenuAnimation.hover(MenuAnimation.hover(0,true,.016f),true,.016f);
        assertEquals(twoFrames,MenuAnimation.hover(0,true,.032f),.00001);
        assertTrue(MenuAnimation.hover(twoFrames,false,.016f)<twoFrames);
    }
}
