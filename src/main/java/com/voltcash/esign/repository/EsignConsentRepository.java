package com.voltcash.esign.repository;

import com.voltcash.esign.model.EsignSession;

import java.util.Optional;

public interface EsignConsentRepository {

    void createSession(EsignSession session);

    Optional<EsignSession> findBySessionId(String sessionId);

    void updateSession(EsignSession session);

    void saveSignatureResult(EsignSession session);
}
