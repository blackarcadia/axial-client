package com.axial.cosmetics.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(InGameHud.class)
public interface InGameHudScoreboardInvoker {
    @Invoker("renderScoreboardSidebar") void axial$renderSidebar(DrawContext context,ScoreboardObjective objective);
}
