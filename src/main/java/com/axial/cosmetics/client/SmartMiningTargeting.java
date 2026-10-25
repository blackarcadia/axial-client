package com.axial.cosmetics.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.axial.axialutils.client.AxialConfigManager;
import org.axial.axialutils.client.AxialVisualEffects;
import java.util.function.Predicate;

/** Uses the rendering eligibility rules so only faded players stop obstructing aim. */
public final class SmartMiningTargeting {
    private static final Predicate<Entity> NOT_FADED_PLAYER=entity->!(entity instanceof PlayerEntity player&&AxialVisualEffects.shouldFadeLivingEntity(player));
    private SmartMiningTargeting(){}
    public static Predicate<Entity> filter(Predicate<Entity> vanilla){
        return AxialConfigManager.get().smartMiningEnabled?vanilla.and(NOT_FADED_PLAYER):vanilla;
    }
}
