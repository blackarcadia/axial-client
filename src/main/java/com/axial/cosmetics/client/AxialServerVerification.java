package com.axial.cosmetics.client;

import java.lang.reflect.Field;

/**
 * Reads the connection state maintained by AxialUtils' mod-verification
 * protocol. If AxialUtils or its verification state is unavailable, features
 * guarded by this class stay disabled.
 */
final class AxialServerVerification {
    private static final long NO_NONCE = Long.MIN_VALUE;
    private static Field activeNonce;
    private static Field reportedUpToDate;
    private static boolean unavailable;

    private AxialServerVerification() {
    }

    static boolean isVerified() {
        if (!resolveFields()) {
            return false;
        }

        try {
            return activeNonce.getLong(null) != NO_NONCE
                    && Boolean.TRUE.equals(reportedUpToDate.get(null));
        } catch (IllegalAccessException e) {
            unavailable = true;
            return false;
        }
    }

    private static boolean resolveFields() {
        if (unavailable) {
            return false;
        }
        if (activeNonce != null && reportedUpToDate != null) {
            return true;
        }

        try {
            Class<?> verification = Class.forName("org.axial.axialutils.client.AxialModVerification");
            activeNonce = verification.getDeclaredField("activeNonce");
            reportedUpToDate = verification.getDeclaredField("lastReportedUpToDate");
            activeNonce.setAccessible(true);
            reportedUpToDate.setAccessible(true);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
            unavailable = true;
            return false;
        }
    }
}
