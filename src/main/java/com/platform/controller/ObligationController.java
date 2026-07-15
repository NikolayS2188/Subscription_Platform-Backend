package com.platform.controller;

import com.platform.model.Category;
import com.platform.model.Obligation;
import com.platform.model.Status;
import com.platform.model.dto.*;
import com.platform.service.ObligationService;
import com.platform.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/obligations")
@RequiredArgsConstructor
public class ObligationController {

    private final ObligationService obligationService;
    private final SseService sseService;

    @PostMapping
    public ResponseEntity<ObligationResponse> createObligation(
            @Valid @RequestBody ObligationRequest request) {
        ObligationResponse response = obligationService.createObligation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Obligation>> getObligations(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Category category) {
        List<Obligation> obligations = obligationService.getObligations(status, category);
        return ResponseEntity.ok(obligations);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<UpcomingResponse> getUpcomingObligations(
            @RequestParam(defaultValue = "7") Integer days) {
        UpcomingResponse response = obligationService.getUpcomingObligations(days);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PaymentResponse> payObligation(
            @PathVariable UUID id,
            @RequestBody(required = false) PaymentRequest request) {
        if (request == null) {
            request = new PaymentRequest();
        }
        PaymentResponse response = obligationService.processPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteObligation(@PathVariable UUID id) {
        obligationService.deleteObligation(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Obligation> cancelObligation(@PathVariable UUID id) {
        Obligation cancelled = obligationService.cancelObligation(id);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamObligations() {
        return sseService.createEmitter();
    }
}
