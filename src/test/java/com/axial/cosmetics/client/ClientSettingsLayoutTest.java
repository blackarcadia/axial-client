package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientSettingsLayoutTest {
    @Test void navigationFooterAndScrollingControlsFitSmallWindows() {
        for(int width:new int[]{260,320,480,640,960})for(int height:new int[]{165,210,240,360,540}) {
            var l=new ClientSettingsLayout(width,height);
            assertTrue(l.panel.x()>=0&&l.panel.y()>=0);
            assertTrue(l.panel.right()<=width&&l.panel.bottom()<=height);
            for(Rect r:new Rect[]{l.controls,l.back,l.settings,l.editHud,l.close,l.reset,l.done,l.scrollbar}) {
                assertTrue(r.x()>=l.panel.x()&&r.right()<=l.panel.right(),r.toString());
                assertTrue(r.y()>=l.panel.y()&&r.bottom()<=l.panel.bottom(),r.toString());
            }
            assertTrue(l.back.right()<=l.settings.x());
            assertTrue(l.settings.right()<=l.editHud.x());
            assertTrue(l.editHud.right()<l.close.x());
            assertTrue(l.reset.right()<l.done.x());
            assertTrue(l.controls.bottom()<l.reset.y());
            assertTrue(l.headerBottom<l.controls.y());
            // Each real control can be fully exposed by scrolling, even at the smallest supported height.
            for(int[] row:new int[][]{{16,38},{58,38},{130,38},{172,36},{214,38},{256,38},{298,38},{340,38},{382,38},{456,36},{492,36}}) {
                int scroll=Math.clamp(row[0],0,l.maxScroll());
                assertTrue(l.fullyVisible(new Rect(l.controls.x(),l.controls.y()+row[0]-scroll,l.controls.width(),row[1])));
            }
            assertFalse(l.fullyVisible(new Rect(l.controls.x(),l.controls.y()-1,20,38)));
        }
    }
    @Test void normalSizeFitsTheApprovedSingleColumnDesign() {
        var l=new ClientSettingsLayout(960,540);
        assertEquals(520,l.panel.width());assertEquals(342,l.panel.height());
        assertTrue(l.maxScroll()>0);assertEquals(480,l.controls.width());
        assertFalse(l.compact);
        var small=new ClientSettingsLayout(480,210);
        assertTrue(small.compact);assertTrue(small.maxScroll()>0);
    }
}
