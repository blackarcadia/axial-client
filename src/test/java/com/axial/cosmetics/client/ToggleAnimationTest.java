package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ToggleAnimationTest {
    @Test void startsAtSavedStateAndMovesSmoothlyToTheNewState() {
        var clock=new AtomicLong();
        var animation=new ToggleAnimation(false,clock::get);
        assertEquals(0,animation.value(false));
        assertEquals(0,animation.value(true));
        float previous=0;
        for(int millis:new int[]{16,33,70,100,160,240,400}) {
            clock.set(millis*1_000_000L);
            float value=animation.value(true);
            assertTrue(value>previous&&value<1);
            previous=value;
        }
        assertEquals(1,previous,.001f);
        assertEquals(1,new ToggleAnimation(true,clock::get).value(true));
    }

    @Test void frameRateDoesNotChangeMotion() {
        var clock=new AtomicLong();
        var frequent=new ToggleAnimation(false,clock::get);
        var sparse=new ToggleAnimation(false,clock::get);
        frequent.value(true);sparse.value(true);
        for(int millis=1;millis<=200;millis++) {
            clock.set(millis*1_000_000L);
            frequent.value(true);
        }
        assertEquals(frequent.value(true),sparse.value(true),.000001f);
    }

    @Test void rapidReversalsPreservePositionAndVelocityAndSettle() {
        var clock=new AtomicLong();
        var animation=new ToggleAnimation(false,clock::get);
        animation.value(true);
        clock.set(70_000_000L);
        float before=animation.value(true);
        assertEquals(before,animation.value(false));
        clock.addAndGet(1_000_000L);
        // Momentum is preserved through the reversal rather than snapping direction.
        assertTrue(animation.value(false)>before);
        for(int i=0;i<20;i++) {
            clock.addAndGet(12_000_000L);
            float value=animation.value(i%2==0);
            assertTrue(value>=0&&value<=1);
        }
        animation.value(false);
        clock.addAndGet(1_000_000_000L);
        assertEquals(0,animation.value(false),.000001f);
    }

    @Test void unchangedTargetDoesNotRestartMotionAndLongPausesSettle() {
        var clock=new AtomicLong();
        var animation=new ToggleAnimation(true,clock::get);
        animation.value(false);
        clock.set(100_000_000L);
        float before=animation.value(false);
        for(int i=0;i<100;i++)assertEquals(before,animation.value(false));
        clock.set(60_000_000_000L);
        assertEquals(0,animation.value(false));
    }
}
