package com.onlinebanking.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

@Component
public class SessionContextStore {

    private final Map<String, SecurityContext> contexts = new ConcurrentHashMap<>();

    public void save(String sessionId, SecurityContext context) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        contexts.put(sessionId, context);
    }

    public SecurityContext load(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? null : contexts.get(sessionId);
    }

    public void clear(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            contexts.remove(sessionId);
        }
    }
}
