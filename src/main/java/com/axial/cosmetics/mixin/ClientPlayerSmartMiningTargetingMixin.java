package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.SmartMiningTargeting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.function.Predicate;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerSmartMiningTargetingMixin {
    // Keep vanilla block occlusion, interaction ranges and other entity predicates intact.
    @ModifyArg(method="method_76763",at=@At(value="INVOKE",target="Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"),index=4)
    private static Predicate<Entity> axial$skipFadedPlayers(Predicate<Entity> vanilla){return SmartMiningTargeting.filter(vanilla);}

    @ModifyArg(method="method_76762",at=@At(value="INVOKE",target="Lnet/minecraft/component/type/AttackRangeComponent;getHitResult(Lnet/minecraft/entity/Entity;FLjava/util/function/Predicate;)Lnet/minecraft/util/hit/HitResult;"),index=2)
    private Predicate<Entity> axial$skipFadedPlayersInItemTargeting(Predicate<Entity> vanilla){return SmartMiningTargeting.filter(vanilla);}
}
