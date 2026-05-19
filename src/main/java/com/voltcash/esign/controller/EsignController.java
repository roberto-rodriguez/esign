package com.voltcash.esign.controller;

import com.voltcash.esign.model.AgreementAcceptanceRequest;
import com.voltcash.esign.model.EsignInitRequest;
import com.voltcash.esign.service.EsignService;
import jakarta.validation.Valid;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/esign")
public class EsignController {

    private final EsignService esignService;

    public EsignController(EsignService esignService) {
        this.esignService = esignService;
    }

    @PostMapping("/init")
    public Map<String, String> init(@RequestBody @Valid EsignInitRequest request) {
        String sessionId = esignService.initializeSession(request);
        return Map.of("sessionId", sessionId);
    }

    @GetMapping("/config")
    public Map<String, Object> config() {
        return esignService.getUiConfig();
    }

    @PostMapping("/{sessionId}/viewed/{documentType}")
    public ResponseEntity<Void> markViewed(@PathVariable("sessionId") String sessionId, @PathVariable("documentType") String documentType) {
        esignService.markViewed(sessionId, documentType);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody @Valid AgreementAcceptanceRequest request) {
        return esignService.submit(request.sessionId(), request.esignAccepted(), request.otherAgreementsAccepted());
    }

    @GetMapping("/documents/{filename:.+}")
    public ResponseEntity<Resource> getDocument(@PathVariable("filename") String filename) {
        Resource resource = new ClassPathResource("pdfs/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
