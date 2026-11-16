package com.axial.cosmetics.client;

/** Exposes only recorder lifetime to keep stale pause-screen controls inert. */
public interface ReplayRecorderStatus {
    boolean axial$isFinished();
}
