package com.example.Tournament.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. Static resources allow
        if (uri.endsWith(".css") || uri.endsWith(".js") ||
            uri.contains("/images/") || uri.contains("/static/") ||
            uri.endsWith(".png") || uri.endsWith(".jpg") || uri.endsWith(".ico") || uri.endsWith(".svg")) {
            return true;
        }

        // 2. Public pages — login required nahi
        if (uri.equals("/login") || uri.equals("/signup") || uri.equals("/") ||
            uri.startsWith("/auth/") || uri.equals("/error") ||
            uri.equals("/matches") || uri.startsWith("/match/") ||
            uri.startsWith("/commentry/") ||
            uri.equals("/teams") || uri.equals("/players") ||
            uri.equals("/tournaments") || uri.startsWith("/tournament/") ||
            uri.startsWith("/api/public/") ||
            // BUG FIX: /api/players/ is called by admin live-scoring page to load player
            // dropdowns. Without this, an expired session silently breaks the scoring UI.
            // /api/score/ endpoints are also called from the scoring page JS.
            uri.startsWith("/api/players/") ||
            uri.startsWith("/api/score/")) {
            return true;
        }

        // 3. Session check
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
