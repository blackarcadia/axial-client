package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeatureSettingsLayoutTest {
    @Test void allControlsStayInsideCompactWindows() {
        for(int width:new int[]{260,320,380,480,640,960})for(int height:new int[]{165,210,240,360,540}) {
            var l=new FeatureSettingsLayout(width,height,true);
            assertTrue(l.panel.x()>=0&&l.panel.y()>=0);
            assertTrue(l.panel.right()<=width&&l.panel.bottom()<=height);
            assertTrue(l.panel.height()<=304);
            for(var r:new ModMenuLayout.Rect[]{l.controls,l.back,l.close,l.enabled,l.reset,l.done,l.scrollbar}) {
                assertTrue(r.x()>=l.panel.x()&&r.right()<=l.panel.right(),r.toString());
                assertTrue(r.y()>=l.panel.y()&&r.bottom()<=l.panel.bottom(),r.toString());
            }
            assertTrue(l.back.right()<l.enabled.x());
            assertTrue(l.controls.bottom()<l.reset.y());
            if(l.preview!=null)assertTrue(l.controls.right()<l.preview.x());
        }
    }
    @Test void smallWindowsGiveAllAvailableWidthToSettings() {
        assertNull(new FeatureSettingsLayout(480,300,true).preview);
        assertNotNull(new FeatureSettingsLayout(640,360,true).preview);
        assertNull(new FeatureSettingsLayout(640,360,false).preview);
    }
    @Test void scrollingReachesLastRowWithoutAllowingClippedHitTargets() {
        var l=new FeatureSettingsLayout(520,240,true);
        int contentHeight=450,scroll=l.maxScroll(contentHeight);
        var last=new ModMenuLayout.Rect(l.controls.x(),l.controls.y()+contentHeight-22-scroll,l.controls.width(),22);
        assertEquals(l.controls.bottom(),last.bottom());
        assertTrue(l.fullyVisible(last));
        assertFalse(l.fullyVisible(new ModMenuLayout.Rect(last.x(),last.y()+1,last.width(),last.height())));
    }
}
