package com.voltcash.esign.service;

import com.voltcash.esign.model.EsignInitRequest;
import com.voltcash.esign.model.EsignSession;
import com.voltcash.esign.repository.EsignConsentRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
public class EsignService {

    private static final String ESIGN_FILENAME = "VoltCashESIGNAgreement.pdf";
    private final EsignConsentRepository repository;
    private final CallbackNotifierService callbackNotifierService;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public EsignService(EsignConsentRepository repository, CallbackNotifierService callbackNotifierService) {
        this.repository = repository;
        this.callbackNotifierService = callbackNotifierService;
    }

    public String initializeSession(EsignInitRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String lang = normalizeLang(request.defaultLanguage());
        EsignSession session = new EsignSession(
                sessionId,
                request.fullName(),
                request.ipAddress(),
                request.phoneNumber(),
                request.callbackUrl(),
                lang,
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

    public Map<String, Object> getUiConfig(String sessionId) {
        EsignSession session = requireSession(sessionId);
        String lang = normalizeLang(session.language());
        List<String> allFiles = listLanguagePdfFiles(lang);

        List<String> otherFiles = allFiles.stream()
                .filter(file -> !ESIGN_FILENAME.equalsIgnoreCase(file))
                .sorted()
                .toList();

        return Map.of(
                "language", lang,
                "esignDocumentUrl", buildUrl(lang, ESIGN_FILENAME),
                "otherDocumentUrls", otherFiles.stream().map(file -> buildUrl(lang, file)).toList()
        );
    }

    public void markViewed(String sessionId, String documentType) {
        EsignSession session = requireSession(sessionId);
        boolean esignViewed = session.esignViewed() || "esign".equals(documentType);
        boolean othersViewed = session.othersViewed() || "others".equals(documentType);
        repository.updateSession(new EsignSession(session.sessionId(), session.fullName(), session.ipAddress(),
                session.phoneNumber(), session.callbackUrl(), session.language(), session.createdAt(), session.signedAt(),
                esignViewed, othersViewed, session.esignAccepted(), session.othersAccepted()));
    }

    public Map<String, Object> submit(String sessionId, boolean esignAccepted, boolean othersAccepted) {
        EsignSession session = requireSession(sessionId);

        if (!session.esignViewed() || !session.othersViewed()) throw new IllegalStateException("All required documents must be viewed before submit.");
        if (!esignAccepted || !othersAccepted) throw new IllegalStateException("All required agreements must be accepted.");

        EsignSession signed = new EsignSession(session.sessionId(), session.fullName(), session.ipAddress(),
                session.phoneNumber(), session.callbackUrl(), session.language(), session.createdAt(), Instant.now(),
                session.esignViewed(), session.othersViewed(), true, true);

        repository.saveSignatureResult(signed);
        callbackNotifierService.sendCallback(signed.callbackUrl(), signed.sessionId(), "Y");

        return Map.of("status", "SIGNED", "sessionId", signed.sessionId(), "signedAt", signed.signedAt().toString());
    }

    private List<String> listLanguagePdfFiles(String lang) {
        try {
            Resource[] resources = resolver.getResources("classpath*:pdfs/" + lang + "/*.pdf");
            List<String> names = new ArrayList<>();
            for (Resource r : resources) {
                if (r.getFilename() != null) names.add(r.getFilename());
            }
            return names;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load PDF files for language " + lang, e);
        }
    }

    private String buildUrl(String lang, String filename) {
        return "/api/esign/documents/" + lang + "/" + filename;
    }

    private String normalizeLang(String lang) {
        return "es".equalsIgnoreCase(lang) ? "es" : "en";
    }

    private EsignSession requireSession(String sessionId) {
        return repository.findBySessionId(sessionId).orElseThrow(() -> new IllegalArgumentException("Invalid sessionId"));
    }
}
