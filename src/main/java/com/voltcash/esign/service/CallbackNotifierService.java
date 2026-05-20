package com.voltcash.esign.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class CallbackNotifierService {
    private static final Logger log = LoggerFactory.getLogger(CallbackNotifierService.class);
    private final RestClient restClient = RestClient.create();

    public void sendCallback(String callbackUrl, String sessionId, String result) {
        try {
            restClient.post()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sessionId", sessionId, "result", result))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Callback failed for session {}: {}", sessionId, ex.getMessage());
        }
    }
}
