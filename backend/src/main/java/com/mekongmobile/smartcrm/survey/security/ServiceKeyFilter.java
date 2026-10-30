
package com.mekongmobile.smartcrm.survey.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

@Component
public class ServiceKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Service-Key";
    private static final String PATH = "/api/survey-invitations";

    private final byte[] expectedKey;

    public ServiceKeyFilter(
            @Value("${app.security.service-key}") String key
    ) {
        if (key == null || key.isBlank() || key.length() < 32) {
            throw new IllegalStateException(
                    "SERVICE_API_KEY must contain at least 32 characters"
            );
        }

        this.expectedKey = key.getBytes(StandardCharsets.UTF_8);
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getServletPath();

        boolean protectedPath =
                PATH.equals(path)
                        || path.startsWith(PATH + "/");

        boolean isPost =
                "POST".equalsIgnoreCase(request.getMethod());

        return !(isPost && protectedPath);
    }



    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        Enumeration<String> values = request.getHeaders(HEADER);

        String suppliedKey = null;

        if (values.hasMoreElements()) {
            suppliedKey = values.nextElement();

            // Reject duplicate key headers.
            if (values.hasMoreElements()) {
                suppliedKey = null;
            }
        }

        boolean valid = suppliedKey != null
                && MessageDigest.isEqual(
                expectedKey,
                suppliedKey.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");

            response.getWriter().write(
                    "{\"error\":{"
                            + "\"code\":\"UNAUTHENTICATED\","
                            + "\"message\":\"Missing or invalid X-Service-Key\","
                            + "\"fields\":{}"
                            + "}}"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
