package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Camera framing is independent of the mannequin and the saved loadout. */
record WardrobeModelView(float scale,float centerY) {
    static float yaw(WardrobeItem item) {return item.cape()!=null?155:25;}
    static float cardYaw(WardrobeItem item) {
        return yaw(item)+(item.cape()!=null||item.accessory()!=null?180:0);
    }

    static WardrobeModelView fit(WardrobeItem item,Rect bounds,boolean card) {
        float height=2.05f,width=1.05f,center=.9f;
        if(item!=null&&item.aura()!=null) {height=2.9f;width=2.6f;center=.95f;}
        else if(card&&item!=null&&(item.accessory()==WardrobeAccessory.WHITE_KICKS||item.accessory()==WardrobeAccessory.WHITE_SNEAKERS)) {
            height=.95f;width=1.05f;center=.38f;
        } else if(card&&item!=null&&item.accessory()==WardrobeAccessory.SWIM_BANDS) {
            height=1.5f;width=1.4f;center=1.13f;
        }
        return new WardrobeModelView(Math.max(1,Math.min((bounds.height()-6)/height,(bounds.width()-8)/width)),center);
    }
}
