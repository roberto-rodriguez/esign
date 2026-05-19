package com.voltcash.esign.model;

import java.time.Instant;

public record EsignSession(
        String sessionId,
        String fullName,
        String ipAddress,
        String phoneNumber,
        String callbackUrl,
        Instant createdAt,
        Instant signedAt,
        boolean esignViewed,
        boolean othersViewed,
        boolean esignAccepted,
        boolean othersAccepted
) {
}
