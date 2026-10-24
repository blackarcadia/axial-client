package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.PerformanceConfig;
import net.minecraft.client.render.WeatherRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherRendering.class)
public abstract class PerformanceWeatherMixin {
    @Inject(method="buildPrecipitationPieces",at=@At("HEAD"),cancellable=true)
    private void axial$skipWeatherGeometry(CallbackInfo ci){if(!PerformanceConfig.weather())ci.cancel();}
    @Inject(method="renderPrecipitation",at=@At("HEAD"),cancellable=true)
    private void axial$hideWeather(CallbackInfo ci){if(!PerformanceConfig.weather())ci.cancel();}
    @Inject(method="addParticlesAndSound",at=@At("HEAD"),cancellable=true)
    private void axial$hideRainSplashes(CallbackInfo ci){if(!PerformanceConfig.weather())ci.cancel();}
}
