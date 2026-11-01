package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ScoreboardHudConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public abstract class InGameHudScoreboardNumbersMixin {
    // Replace the formatted sidebar score before vanilla measures it. Per-entry server
    // number formats are hidden too, while score values and sorting remain untouched.
    @WrapOperation(method="method_55439", at=@At(value="INVOKE",
            target="Lnet/minecraft/scoreboard/ScoreboardEntry;formatted(Lnet/minecraft/scoreboard/number/NumberFormat;)Lnet/minecraft/text/MutableText;"))
    private MutableText axial$sidebarNumber(ScoreboardEntry entry, NumberFormat format, Operation<MutableText> original) {
        return ScoreboardHudConfig.showNumbers() ? original.call(entry, format) : Text.empty();
    }
}
