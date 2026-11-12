package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WardrobeModelViewTest {
    @Test void modelViewportsLeaveCaptionsClearAcrossGuiSizes() {
        for(int width:new int[]{260,320,427,640,960,1280})for(int height:new int[]{165,210,270,360,540,720}) {
            var layout=new WardrobeLayout(width,height);
            for(int i=0;i<6;i++) {
                var card=layout.card(i);
                if(card.height()<55)continue;
                var stage=WardrobeLayout.modelStage(card);
                assertTrue(stage.x()>=card.x()&&stage.right()<=card.right());
                assertTrue(stage.y()>=card.y()&&stage.bottom()<card.bottom()-29);
                for(var item:java.util.List.of(WardrobeItem.of(WardrobeCape.PURRR),WardrobeItem.of(WardrobeAura.HEXWIND),
                        WardrobeItem.of(WardrobeAccessory.WHITE_KICKS),WardrobeItem.of(WardrobeAccessory.SWIM_BANDS))) {
                    var view=WardrobeModelView.fit(item,stage,true);
                    assertTrue(Float.isFinite(view.scale())&&view.scale()>0);
                }
            }
        }
    }

    @Test void footwearGetsACloseupAndCapesFaceTheirOutwardDesign() {
        var bounds=new Rect(0,0,140,130);
        var kicks=WardrobeItem.of(WardrobeAccessory.WHITE_KICKS);
        var full=WardrobeModelView.fit(kicks,bounds,false);
        var closeup=WardrobeModelView.fit(kicks,bounds,true);
        assertTrue(closeup.scale()>full.scale());
        assertTrue(closeup.centerY()<.5f,"shoe card camera centers on the feet");
        assertEquals(155,WardrobeModelView.yaw(WardrobeItem.of(WardrobeCape.PURRR)));
        assertEquals(25,WardrobeModelView.yaw(kicks));
        assertEquals(335,WardrobeModelView.cardYaw(WardrobeItem.of(WardrobeCape.PURRR)));
        assertEquals(205,WardrobeModelView.cardYaw(kicks));
        assertEquals(205,WardrobeModelView.cardYaw(WardrobeItem.of(WardrobeAccessory.SWIM_BANDS)));
        var aura=WardrobeModelView.fit(WardrobeItem.of(WardrobeAura.HEXWIND),bounds,true);
        assertTrue(aura.scale()<full.scale(),"aura needs more space around the mannequin");
        assertEquals(full,WardrobeModelView.fit(null,bounds,false),"empty loadout still shows the player");
    }
}
