package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModMenuLayoutTest {
    @Test void cardsControlsAndFiltersFitAtDifferentWindowSizes() {
        for(int w:new int[]{220,280,320,427,484,640,854,1280}) {
            for(int h:new int[]{160,180,240,270,360,480,720}) {
                var l=new ModMenuLayout(w,h);
                inside(l.panel,new ModMenuLayout.Rect(0,0,w,h));
                assertTrue(l.panel.height()<=304);
                inside(l.search,l.panel);inside(l.enabledFilter,l.panel);
                inside(l.close,l.panel);inside(l.navigation,l.panel);inside(l.grid,l.panel);
                assertTrue(l.search.right()<l.enabledFilter.x());
                assertTrue(l.grid.y()>=l.search.bottom());
                if(!l.tight) for(int i=0;i<5;i++)inside(l.category(i),l.panel);
                for(int count:new int[]{0,1,3,9,17,30}) {
                    int max=l.maxScroll(count);
                    for(int i=0;i<count;i++) {
                        var card=l.card(i,0);
                        inside(l.toggle(card),card);inside(l.settings(card),card);
                        assertTrue(l.toggle(card).right()<l.settings(card).x());
                        assertTrue(l.toggle(card).y()>=card.y()+22,"title and toggle must not overlap");
                        assertTrue(card.right()<=l.grid.right());
                    }
                    if(count>0) assertTrue(l.card(count-1,max).bottom()<=l.grid.bottom());
                }
            }
        }
    }
    @Test void desktopFitsThreeColumnsButSmallWindowsReflow() {
        assertEquals(3,new ModMenuLayout(854,480).columns);
        assertTrue(new ModMenuLayout(854,480).sidebar);
        assertEquals(1,new ModMenuLayout(320,240).columns);
        assertFalse(new ModMenuLayout(320,240).sidebar);
    }
    @Test void desktopShowsTwoCompleteRowsInTheShorterPanel() {
        var layout=new ModMenuLayout(854,480);
        assertTrue(layout.card(5,0).bottom()<=layout.grid.bottom());
        assertTrue(layout.card(6,0).y()>=layout.grid.bottom());
        assertTrue(layout.maxScroll(17)>0);
    }
    private static void inside(ModMenuLayout.Rect child,ModMenuLayout.Rect parent) {
        assertTrue(child.width()>0&&child.height()>0,"non-positive bounds: "+child);
        assertTrue(child.x()>=parent.x()&&child.y()>=parent.y(),"outside: "+child+" parent "+parent);
        assertTrue(child.right()<=parent.right()&&child.bottom()<=parent.bottom(),"outside: "+child+" parent "+parent);
    }
}
