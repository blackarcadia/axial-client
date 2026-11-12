package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PollHudLayoutTest {
    @Test void bothResultBarsFitInTheShorterHudWithoutCoveringTheFooter(){
        assertTrue(PollHudLayout.HEIGHT<108);
        for(int screen:new int[]{160,260,320,427,640,960}) {
            var box=new Rect(5,48,PollHudLayout.width(screen),PollHudLayout.HEIGHT);
            assertTrue(box.right()<=screen);
            var first=PollHudLayout.option(box,0);var second=PollHudLayout.option(box,1);
            assertTrue(first.bottom()<=second.y());
            for(var row:java.util.List.of(first,second)) {
                var bar=PollHudLayout.bar(row);
                assertTrue(bar.x()>box.x()&&bar.right()<box.right());
                assertTrue(bar.y()>=row.y()+9&&bar.bottom()<=row.bottom());
                assertTrue(bar.bottom()<box.bottom()-12);
            }
        }
    }
}
