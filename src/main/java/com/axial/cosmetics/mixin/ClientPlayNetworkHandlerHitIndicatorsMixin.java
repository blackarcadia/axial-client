package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.HitIndicators;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerHitIndicatorsMixin {
    // TAIL runs after the vanilla packet handler has enforced the client thread.
    @Inject(method = "onEntityDamage", at = @At("TAIL"))
    private void axial$hitIndicator(EntityDamageS2CPacket packet, CallbackInfo ci) {
        HitIndicators.onDamage(packet);
    }
    @WrapOperation(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/data/DataTracker;writeUpdatedEntries(Ljava/util/List;)V"))
    private void axial$healthDelta(DataTracker tracker, List<DataTracker.SerializedEntry<?>> values, Operation<Void> original,
                                    net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket packet) {
        var client = MinecraftClient.getInstance();
        LivingEntity target = client.world != null && client.world.getEntityById(packet.id()) instanceof LivingEntity living ? living : null;
        float previous = target == null ? 0 : target.getHealth();
        original.call(tracker, values);
        if (target != null && target.getHealth() < previous) HitIndicators.healthChanged(target, previous);
    }
}
