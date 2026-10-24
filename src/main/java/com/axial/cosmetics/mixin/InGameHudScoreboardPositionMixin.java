package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ScoreboardHud;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public abstract class InGameHudScoreboardPositionMixin {
    @WrapMethod(method="renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V")
    private void axial$positionSidebar(DrawContext context,ScoreboardObjective objective,Operation<Void> original){
        context.getMatrices().pushMatrix();
        try{original.call(context,objective);}finally{context.getMatrices().popMatrix();}
    }
    // Vanilla's first fill is the title background; local 13 is the sidebar bottom.
    // Translate here, after vanilla has measured all styled names and formatted scores.
    @WrapOperation(method="renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",ordinal=0))
    private void axial$moveMeasuredSidebar(DrawContext context,int left,int top,int right,int titleBottom,int color,Operation<Void> original,@Local(index=13) int bottom){
        ScoreboardHud.place(context,left,top,right,bottom);
        original.call(context,left,top,right,titleBottom,color);
    }
}
