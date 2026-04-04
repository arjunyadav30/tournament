package com.example.Tournament.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class LiveScoreBroadcaster {

    private final Map<Integer, List<SseEmitter>> emittersByMatch = new ConcurrentHashMap<>();

    public SseEmitter subscribe(int matchId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByMatch.computeIfAbsent(matchId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(matchId, emitter));
        emitter.onTimeout(() -> removeEmitter(matchId, emitter));
        emitter.onError(ex -> removeEmitter(matchId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"ok\":true}"));
        } catch (IOException e) {
            removeEmitter(matchId, emitter);
        }

        return emitter;
    }

    public void broadcastBallUpdate(int matchId, int totalBalls) {
        broadcast(matchId, "ball", "{\"matchId\":" + matchId + ",\"totalBalls\":" + totalBalls + "}");
    }

    public void broadcastMatchUpdate(int matchId, String status) {
        String safeStatus = status == null ? "" : status.replace("\"", "\\\"");
        broadcast(matchId, "match", "{\"matchId\":" + matchId + ",\"status\":\"" + safeStatus + "\"}");
    }

    private void broadcast(int matchId, String eventName, String payloadJson) {
        List<SseEmitter> emitters = emittersByMatch.get(matchId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payloadJson));
            } catch (IOException e) {
                removeEmitter(matchId, emitter);
            }
        }
    }

    private void removeEmitter(int matchId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByMatch.get(matchId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByMatch.remove(matchId);
        }
    }
}
