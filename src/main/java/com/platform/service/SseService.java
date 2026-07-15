package com.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.model.Obligation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(60_000L);
        String emitterId = UUID.randomUUID().toString();
        emitters.put(emitterId, emitter);

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));
        emitter.onError(e -> emitters.remove(emitterId));

        log.info("Created SSE emitter: {}", emitterId);
        return emitter;
    }

    public void sendObligationUpdate(Obligation obligation) {
        try {
            String eventData = objectMapper.writeValueAsString(
                    Map.of("event", "update", "obligation", obligation)
            );
            sendEvent(eventData);
        } catch (IOException e) {
            log.error("Error sending SSE event", e);
        }
    }

    public void sendObligationDeletion(UUID id) {
        try {
            String eventData = objectMapper.writeValueAsString(
                    Map.of("type", "obligation_deleted", "id", id.toString())
            );
            sendEvent(eventData);
        } catch (IOException e) {
            log.error("Error sending SSE event", e);
        }
    }

    private void sendEvent(String eventData) {
        emitters.entrySet().removeIf(entry -> {
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .data(eventData, MediaType.APPLICATION_JSON));
                return false;
            } catch (IOException e) {
                emitter.complete();
                return true;
            }
        });
    }
}
