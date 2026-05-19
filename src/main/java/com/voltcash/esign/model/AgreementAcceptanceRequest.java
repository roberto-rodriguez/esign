package com.voltcash.esign.model;

import jakarta.validation.constraints.NotBlank;

public record AgreementAcceptanceRequest(
        @NotBlank String sessionId,
        boolean esignAccepted,
        boolean otherAgreementsAccepted
) {
}
