package com.axial.cosmetics.client;

import com.axial.cosmetics.client.ModMenuLayout.Rect;

/** Camera framing is independent of the mannequin and the saved loadout. */
record WardrobeModelView(float scale,float centerY) {
    static float yaw(WardrobeItem item) {return item.emote()!=null?205:item.cape()!=null?155:25;}
    static float cardYaw(WardrobeItem item) {
        return yaw(item)+(item.cape()!=null||item.accessory()!=null?180:0);
    }

    static WardrobeModelView fit(WardrobeItem item,Rect bounds,boolean card) {
        float height=2.05f,width=1.05f,center=.9f;
        if(item!=null&&item.emote()!=null){height=3.1f;width=2.5f;center=.9f;}
        else if(item!=null&&item.aura()!=null) {height=2.9f;width=item.aura()==WardrobeAura.PIXEL_STARS?4.6f:3.7f;center=.95f;}
        else if(item!=null&&item.accessory()==WardrobeAccessory.GHOSTLY_GHOST) {height=card?1.4f:2.3f;width=2.6f;center=card?1.7f:1f;}
        else if(item!=null&&item.accessory()==WardrobeAccessory.POOKLES) {height=card?1.3f:2.2f;width=2.1f;center=card?1.65f:1f;}
        else if(item!=null&&item.accessory()!=null&&item.accessory().slots.contains(com.axial.cosmetics.data.CosmeticSlot.PET)) {height=card?1.15f:2.1f;width=1.8f;center=card?1.6f:.95f;}
        else if(card&&item!=null&&item.accessory()!=null&&item.accessory().slots.contains(com.axial.cosmetics.data.CosmeticSlot.HEAD)) {
            height=1.05f;width=1.55f;center=1.9f;
        } else if(card&&item!=null&&item.accessory()!=null&&item.accessory().slots.contains(com.axial.cosmetics.data.CosmeticSlot.BOOTS)) {
            height=.95f;width=1.05f;center=.38f;
        } else if(card&&item!=null&&(item.accessory()==WardrobeAccessory.SWIM_BANDS||item.accessory()==WardrobeAccessory.RIBCAGE)) {
            height=1.5f;width=1.4f;center=1.13f;
        }
        return new WardrobeModelView(Math.max(1,Math.min((bounds.height()-6)/height,(bounds.width()-8)/width)),center);
    }
}
