package com.voltcash.esign.model;

import jakarta.validation.constraints.NotBlank;

public record EsignInitRequest(
        @NotBlank String fullName,
        String ipAddress,
        @NotBlank String phoneNumber,
        @NotBlank String callbackUrl,
        String defaultLanguage
) {
}
