package com.axial.cosmetics.client;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.SkinTextures;

/** A detached mannequin. The queued GUI render carries its own cosmetic selection. */
final class WardrobePreviewState extends PlayerEntityRenderState {
    final WardrobeAura aura;
    final java.util.List<WardrobeAccessory> accessories;

    WardrobePreviewState(WardrobeItem item,SkinTextures skin,float age,float yaw) {
        this(item.cape(),item.aura(),item.accessory()==null?java.util.List.of():java.util.List.of(item.accessory()),skin,age,yaw);
    }

    static WardrobePreviewState equipped(SkinTextures skin,float age,float yaw) {
        return new WardrobePreviewState(WardrobePreferences.current(),WardrobePreferences.currentAura(),
                WardrobePreferences.currentAccessories(),skin,age,yaw);
    }

    private WardrobePreviewState(WardrobeCape cape,WardrobeAura aura,java.util.List<WardrobeAccessory> accessories,
                                 SkinTextures skin,float age,float yaw) {
        this.aura=aura;
        this.accessories=java.util.List.copyOf(accessories);
        entityType=EntityType.PLAYER;
        width=.6f;height=1.8f;standingEyeHeight=1.62f;
        light=0xF000F0;
        this.age=age;
        bodyYaw=yaw;
        skinTextures=new SkinTextures(skin.body(),cape==null?null:cape.asset(age),
                null,skin.model(),skin.secure());
        capeVisible=cape!=null;
        // A little lift keeps the cape face clear of the mannequin's legs.
        field_53536=8;
    }
}
