package com.axial.cosmetics.mixin;

import com.axial.cosmetics.client.AccountSessionAccess;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsPeriodicCheckers;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.session.telemetry.TelemetryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientAccountMixin implements AccountSessionAccess {
    @Shadow @Final @Mutable private Session session;
    @Shadow @Final @Mutable private UserApiService userApiService;
    @Shadow @Final @Mutable private CompletableFuture<UserApiService.UserProperties> userPropertiesFuture;
    @Shadow @Final @Mutable private CompletableFuture<ProfileResult> gameProfileFuture;
    @Shadow @Final @Mutable private ProfileKeys profileKeys;
    @Shadow @Final @Mutable private SocialInteractionsManager socialInteractionsManager;
    @Shadow @Final @Mutable private TelemetryManager telemetryManager;
    @Shadow @Final @Mutable private RealmsPeriodicCheckers realmsPeriodicCheckers;
    @Shadow private AbuseReportContext abuseReportContext;

    @Override
    public void axial$setAccount(Session next, UserApiService service,
                                 UserApiService.UserProperties properties, ProfileResult profile) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (!client.isOnThread() || client.world != null || client.getNetworkHandler() != null) {
            throw new IllegalStateException("Return to the main menu before switching accounts.");
        }
        telemetryManager.close();
        session = next;
        userApiService = service;
        userPropertiesFuture = CompletableFuture.completedFuture(properties);
        gameProfileFuture = CompletableFuture.completedFuture(profile);
        profileKeys = next.getAccessToken().isBlank() ? ProfileKeys.MISSING
                : ProfileKeys.create(service, next, client.runDirectory.toPath());
        socialInteractionsManager = new SocialInteractionsManager(client, service);
        telemetryManager = new TelemetryManager(client, service, next);
        abuseReportContext = AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), service);
        RealmsClientAccountAccessor.axial$setInstance(null);
        realmsPeriodicCheckers = new RealmsPeriodicCheckers(RealmsClient.createRealmsClient(client));
    }
}
