package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.ReplayRecordingConfig;
import com.axial.cosmetics.client.ReplayRecordingRestart;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.replaymod.recording.handler.ConnectionEventHandler",remap=false)
public abstract class ReplayRecordingConnectionMixin {
    @Inject(method="onConnectedToServerEvent",at=@At("HEAD"),cancellable=true,remap=false)
    private void axial$blockDisabledConnection(ClientConnection connection,CallbackInfo ci){if(!ReplayRecordingConfig.enabled())ci.cancel();}

    @WrapOperation(method="onConnectedToServerEvent",at=@At(value="INVOKE",target="Lcom/replaymod/core/SettingsRegistry;get(Lcom/replaymod/core/SettingsRegistry$SettingKey;)Ljava/lang/Object;"),remap=false)
    private Object axial$manualStartSettings(@Coerce Object registry,@Coerce Object key,Operation<Object> original){return ReplayRecordingRestart.forceSetting(key)?Boolean.TRUE:original.call(registry,key);}
    @WrapOperation(method="onConnectedToServerEvent",at=@At(value="INVOKE",target="Lcom/replaymod/recording/ServerInfoExt;getAutoRecording()Ljava/lang/Boolean;"),remap=false)
    private Boolean axial$manualStartOnServer(@Coerce Object info,Operation<Boolean> original){return ReplayRecordingRestart.requested()?Boolean.TRUE:original.call(info);}
    @Inject(method="onConnectedToServerEvent",at=@At("RETURN"),remap=false)
    private void axial$consumeManualStart(ClientConnection connection,CallbackInfo ci){ReplayRecordingRestart.clear();}
}
