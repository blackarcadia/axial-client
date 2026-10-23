package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TitleMenuLayoutTest {
    @Test void nativeControlsAndBulletinsFitAllSupportedGuiScales() {
        for(int w:new int[]{320,360,480,560,640,720,960,1280})for(int h:new int[]{240,270,300,360,450,720}) {
            var l=new TitleMenuLayout(w,h);
            for(var r:new ModMenuLayout.Rect[]{l.navigation,l.gamemodes,l.account,l.quit,l.music,l.prisonsImage,l.skyImage,l.prisonsConnect,l.skyConnect,l.pages,l.nav(0),l.nav(1),l.nav(2),l.nav(3),l.dot(0),l.dot(2)}) {
                assertTrue(r.x()>=0&&r.y()>=0&&r.right()<=w&&r.bottom()<=h,w+"x"+h+": "+r);
            }
            assertTrue(l.navigation.right()<l.gamemodes.x());
            assertTrue(l.account.right()<l.quit.x());
            assertTrue(l.skyConnect.bottom()<=l.gamemodes.bottom());
            assertTrue(l.nav(3).bottom()<=l.navigation.bottom());
            assertTrue(l.navigation.bottom()<l.music.y());
        }
    }
    @Test void fullBulletinIsContainedWithoutCroppingOrStretching() {
        for(int w:new int[]{100,184,300})for(int h:new int[]{24,60,170})for(int imageWidth:new int[]{1670,1671,1672}) {
            var area=new ModMenuLayout.Rect(20,30,w,h);var image=TitleMenuLayout.contain(area,imageWidth,941);
            assertTrue(image.x()>=area.x()&&image.y()>=area.y());
            assertTrue(image.right()<=area.right()&&image.bottom()<=area.bottom());
            assertEquals(imageWidth/(double)941,image.width()/(double)image.height(),.04);
        }
    }
}
