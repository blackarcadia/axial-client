package com.axial.cosmetics.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.hit.EntityHitResult;
import com.axial.cosmetics.mixin.MinecraftClientAttackInvoker;

/**
 * Repeats an attack only after a verified AxialUtils server handshake and only
 * while the player continues to aim at the hostile mob they originally hit.
 */
public final class VerifiedMobAttack {
    private static Entity target;

    private VerifiedMobAttack() {
    }

    public static void recordManualAttack(MinecraftClient client, boolean attackSucceeded) {
        if (!attackSucceeded || !AxialServerVerification.isVerified() || client.crosshairTarget == null) {
            clear();
            return;
        }

        if (client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof Monster
                && entityHit.getEntity().isAlive()
                && !entityHit.getEntity().isRemoved()) {
            target = entityHit.getEntity();
        } else {
            clear();
        }
    }

    public static void tick(MinecraftClient client) {
        if (!AxialServerVerification.isVerified()
                || client.player == null
                || client.currentScreen != null
                || target == null
                || !target.isAlive()
                || target.isRemoved()
                || !isStillAimingAtTarget(client)) {
            clear();
            return;
        }

        if (client.player.getAttackCooldownProgress(0.0F) >= 1.0F) {
            ((MinecraftClientAttackInvoker) client).axial_cosmetics$invokeDoAttack();
        }
    }

    private static boolean isStillAimingAtTarget(MinecraftClient client) {
        return client.crosshairTarget instanceof EntityHitResult entityHit
                && entityHit.getEntity() == target;
    }

    public static void clear() {
        target = null;
    }
}
