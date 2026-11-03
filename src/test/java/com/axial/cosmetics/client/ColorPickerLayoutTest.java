package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColorPickerLayoutTest {
    @Test void chromaHexAndFooterFitWithoutOverlapOnSmallWindows() {
        for(int w:new int[]{260,320,640,960})for(int h:new int[]{165,180,210,360,540}) {
            var l=new ColorPickerLayout(w,h);
            for(var r:java.util.List.of(l.shade,l.hue,l.hex,l.chroma,l.back,l.reset,l.done)) {
                assertTrue(r.x()>=l.panel.x()&&r.right()<=l.panel.right());
                assertTrue(r.y()>=l.panel.y()&&r.bottom()<=l.panel.bottom());
            }
            assertTrue(l.back.bottom()<l.shade.y());assertTrue(l.shade.bottom()<l.hex.y());
            assertTrue(l.hex.bottom()<l.chroma.y());assertTrue(l.chroma.bottom()<l.reset.y());
            assertTrue(l.reset.right()<l.done.x());
        }
    }
}
