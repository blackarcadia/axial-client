package com.axial.cosmetics.client;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

class WardrobeTest {
    @Test void accessRequiresOwnerSessionAndMatchingWorldPlayer() {
        UUID owner=CreatorCosmetics.OWNER, stranger=new UUID(0,1);
        assertTrue(WardrobeAccess.allows(owner,null));
        assertTrue(WardrobeAccess.allows(owner,owner));
        assertFalse(WardrobeAccess.allows(stranger,null));
        assertFalse(WardrobeAccess.allows(stranger,stranger));
        assertFalse(WardrobeAccess.allows(stranger,owner));
        assertFalse(WardrobeAccess.allows(owner,stranger));
        assertFalse(WardrobeAccess.allows(null,null));
        assertFalse(WardrobeAccess.allows(null,owner));
    }
    @Test void panelsCardsAndActionsFitAcrossGuiScales() {
        for(int w:new int[]{260,320,427,640,960,1280})for(int h:new int[]{165,210,270,360,540,720}) {
            var l=new WardrobeLayout(w,h);
            inside(l.panel,new Rect(0,0,w,h));
            for(Rect r:new Rect[]{l.close,l.categories,l.search,l.grid,l.preview,l.details,l.done})inside(r,l.panel);
            for(int i=0;i<6;i++)inside(l.category(i),l.categories);
            for(int i=0;i<2;i++)inside(l.card(i),l.grid);
            assertTrue(l.search.right()<l.preview.x());
            assertTrue(l.details.bottom()<=l.done.y());
            assertTrue(l.grid.right()<l.preview.x());
        }
    }
    private static void inside(Rect child,Rect parent) {
        assertTrue(child.width()>0&&child.height()>0,child.toString());
        assertTrue(child.x()>=parent.x()&&child.y()>=parent.y(),child.toString());
        assertTrue(child.right()<=parent.right()&&child.bottom()<=parent.bottom(),child+" outside "+parent);
    }
}
