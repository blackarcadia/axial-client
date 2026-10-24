package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuickMenuLayoutTest {
    @Test void allSevenActionsAndLogoFitWithoutOverlaps() {
        for(int width:new int[]{220,280,320,427,640,854,1280})for(int height:new int[]{160,180,240,270,360,480,720}) {
            var layout=new QuickMenuLayout(width,height);
            assertTrue(layout.logo.y()>=0);
            assertTrue(layout.logo.bottom()<layout.bar.y());
            assertTrue(layout.bar.x()>=0&&layout.bar.right()<=width);
            assertTrue(layout.hintY+9<=height);
            assertTrue(layout.bar.height()<=62);
            for(int i=0;i<7;i++) {
                var button=layout.button(i);
                assertTrue(button.x()>=layout.bar.x()&&button.right()<=layout.bar.right());
                assertTrue(button.y()>=layout.bar.y()&&button.bottom()<=layout.bar.bottom());
                if(i<6)assertTrue(button.right()<=layout.button(i+1).x());
            }
        }
    }
}
