package com.mekongmobile.smartcrm.survey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SurveyRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/surveys/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start >= 60000);
        Window window = windows.compute(request.getRemoteAddr(), (ip, current) ->
                current == null || now - current.start >= 60000 ? new Window(now, 1) : new Window(current.start, current.count + 1));
        if (window.count > 10) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", Long.toString(Math.max(1, (60000 - now + window.start + 999) / 1000)));
            response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Quá 10 yêu cầu/phút/IP\",\"fields\":{}}}");
            return;
        }
        chain.doFilter(request, response);
    }
}
