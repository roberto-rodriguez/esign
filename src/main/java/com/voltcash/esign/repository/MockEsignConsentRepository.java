package com.voltcash.esign.repository;

import com.voltcash.esign.model.EsignSession;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MockEsignConsentRepository implements EsignConsentRepository {

    private final Map<String, EsignSession> store = new ConcurrentHashMap<>();

    @Override
    public void createSession(EsignSession session) {
        store.put(session.sessionId(), session);
    }

    @Override
    public Optional<EsignSession> findBySessionId(String sessionId) {
        return Optional.ofNullable(store.get(sessionId));
    }

    @Override
    public void updateSession(EsignSession session) {
        store.put(session.sessionId(), session);
    }

    @Override
    public void saveSignatureResult(EsignSession session) {
        store.put(session.sessionId(), session);
    }
}
