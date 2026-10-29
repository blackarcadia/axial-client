package com.axial.cosmetics.client;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresenceDisplayOffsetTest {
    @Test void offsetRemainsBoundedAndOnlyChangesGraduallyOverSeveralDays() {
        var display=new PresenceDisplayOffset(new Random(1234),0);
        int previous=display.value(0);
        boolean increased=false,decreased=false;
        long lastChange=Long.MIN_VALUE;
        for(int second=1;second<=3*24*60*60;second++) {
            int current=display.value(TimeUnit.SECONDS.toNanos(second));
            assertTrue(current>=20&&current<=70);
            assertTrue(Math.abs(current-previous)<=2);
            if(current!=previous) {
                if(lastChange!=Long.MIN_VALUE)assertTrue(second-lastChange>=25);
                lastChange=second;
                increased|=current>previous;decreased|=current<previous;
            }
            previous=current;
        }
        assertTrue(increased&&decreased);
    }
    @Test void realPlayersAreAddedExactlyWithoutFabricatingAServiceResult() {
        var display=new PresenceDisplayOffset(new Random(52),0);
        int offset=display.value(0);
        assertEquals(offset,display.displayedCount(0,0));
        assertEquals(offset+12,display.displayedCount(12,0));
        assertEquals(-1,display.displayedCount(-1,0));
        assertEquals((long)Integer.MAX_VALUE+offset,display.displayedCount(Integer.MAX_VALUE,0));
    }
    @Test void repeatedRendersStayStableAndReturningAfterHoursDoesNotJump() {
        var display=new PresenceDisplayOffset(new Random(82),0);
        int initial=display.value(0);
        for(int frame=0;frame<1000;frame++)assertEquals(initial,display.value(frame*1_000_000L));
        int resumed=display.value(TimeUnit.HOURS.toNanos(8));
        assertTrue(Math.abs(resumed-initial)<=2);
        assertEquals(resumed,display.value(TimeUnit.HOURS.toNanos(8)+1));
    }
}
