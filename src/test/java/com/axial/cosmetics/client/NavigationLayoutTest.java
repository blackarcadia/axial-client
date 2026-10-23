package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavigationLayoutTest {
    @Test void mapAndSidebarStayInsideTheWindowAndNeverOverlap() {
        for(int width:new int[]{320,480,640,960,1920})for(int height:new int[]{180,240,360,540,1080}) {
            var l=new NavigationLayout(width,height);
            assertTrue(l.panel.x()>=0&&l.panel.y()>=0);
            assertTrue(l.panel.right()<=width&&l.panel.bottom()<=height);
            assertEquals(l.map.width(),l.map.height());
            assertTrue(l.map.x()>l.sidebar.right());
            assertTrue(l.map.right()<=l.panel.right()-10);
            assertTrue(l.map.bottom()<=l.panel.bottom()-28);
            assertTrue(l.list.bottom()<=l.panel.bottom()-28);
            assertTrue(l.map.y()>=l.search.y());
            assertEquals(0,l.maxScroll(0));assertTrue(l.maxScroll(200)>0);
        }
    }
}
