package com.axial.cosmetics.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.axial.cosmetics.client.ModMenuLayout.Rect;

class CrosshairEditorLayoutTest {
    @Test void controlsRemainAccessibleOnSmallAndLargeWindows(){
        for(int width:new int[]{220,260,320,480,640,960})for(int height:new int[]{160,210,280,360,540}){
            var l=new CrosshairEditorLayout(width,height);
            for(Rect r:new Rect[]{l.controls,l.presets,l.customize,l.back,l.enabled,l.close,l.reset,l.undo,l.done,l.scrollbar}){
                assertTrue(r.x()>=l.panel.x()&&r.right()<=l.panel.right(),r.toString());
                assertTrue(r.y()>=l.panel.y()&&r.bottom()<=l.panel.bottom(),r.toString());
            }
            assertTrue(l.reset.right()<l.undo.x());assertTrue(l.undo.right()<l.done.x());
            assertTrue(l.controls.height()>=34);assertTrue(l.controls.bottom()<l.done.y());
            assertTrue(l.presets.bottom()<l.controls.y());assertTrue(l.presets.right()<l.customize.x());
            int card=Math.min(56,l.controls.height()),stride=card+6,rows=(20+l.columns()-1)/l.columns();
            int contentHeight=rows*stride-6;
            for(int i=0;i<rows;i++){
                int scroll=Math.clamp(i*stride,0,l.maxScroll(contentHeight));
                assertTrue(l.fullyVisible(new Rect(l.controls.x(),l.controls.y()+i*stride-scroll,80,card)));
            }
        }
    }
    @Test void desktopKeepsPreviewBesideTheEditor(){
        var l=new CrosshairEditorLayout(960,540);assertNotNull(l.preview);
        assertTrue(l.controls.right()<l.preview.x());assertEquals(420,l.panel.height());
        var narrow=new CrosshairEditorLayout(480,360);assertNotNull(narrow.preview);assertTrue(narrow.preview.bottom()<narrow.presets.y());
    }
}
