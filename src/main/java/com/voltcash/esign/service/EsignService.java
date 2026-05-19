package com.voltcash.esign.service;

import com.voltcash.esign.model.EsignInitRequest;
import com.voltcash.esign.model.EsignSession;
import com.voltcash.esign.repository.EsignConsentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EsignService {

    private final EsignConsentRepository repository;
    private final CallbackNotifierService callbackNotifierService;

    public EsignService(EsignConsentRepository repository, CallbackNotifierService callbackNotifierService) {
        this.repository = repository;
        this.callbackNotifierService = callbackNotifierService;
    }

    public String initializeSession(EsignInitRequest request) {
        String sessionId = UUID.randomUUID().toString();
        EsignSession session = new EsignSession(
                sessionId,
                request.fullName(),
                request.ipAddress(),
                request.phoneNumber(),
                request.callbackUrl(),
                Instant.now(),
                null,
                false,
                false,
                false,
                false
        );
        repository.createSession(session);
        return sessionId;
    }

    public void markViewed(String sessionId, String documentType) {
        EsignSession session = requireSession(sessionId);
        boolean esignViewed = session.esignViewed() || "esign".equals(documentType);
        boolean othersViewed = session.othersViewed() || "others".equals(documentType);
        repository.updateSession(new EsignSession(session.sessionId(), session.fullName(), session.ipAddress(),
                session.phoneNumber(), session.callbackUrl(), session.createdAt(), session.signedAt(),
                esignViewed, othersViewed, session.esignAccepted(), session.othersAccepted()));
    }

    public Map<String, Object> submit(String sessionId, boolean esignAccepted, boolean othersAccepted) {
        EsignSession session = requireSession(sessionId);

        if (!session.esignViewed() || !session.othersViewed()) {
            throw new IllegalStateException("All required documents must be viewed before submit.");
        }
        if (!esignAccepted || !othersAccepted) {
            throw new IllegalStateException("All required agreements must be accepted.");
        }

        EsignSession signed = new EsignSession(session.sessionId(), session.fullName(), session.ipAddress(),
                session.phoneNumber(), session.callbackUrl(), session.createdAt(), Instant.now(),
                session.esignViewed(), session.othersViewed(), true, true);

        repository.saveSignatureResult(signed);
        callbackNotifierService.sendCallback(signed.callbackUrl(), signed.sessionId(), "Y");

        return Map.of("status", "SIGNED", "sessionId", signed.sessionId(), "signedAt", signed.signedAt().toString());
    }

    public Map<String, Object> getUiConfig() {
        return Map.of(
                "documents", List.of(
                        Map.of("id", "esign", "label", "E-Sign Requirements", "url", "/api/esign/documents/esign-requirements.pdf"),
                        Map.of("id", "others", "label", "Other Agreements", "url", "/api/esign/documents/other-agreements.pdf")
                )
        );
    }

    private EsignSession requireSession(String sessionId) {
        return repository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sessionId"));
    }
}
