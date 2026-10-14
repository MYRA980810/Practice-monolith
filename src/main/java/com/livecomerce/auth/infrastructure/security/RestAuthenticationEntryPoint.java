package com.livecomerce.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.StreamTicketStoreUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, resolveCode(request));
    }

    /**
     * Writes the auth error body ({@code status}, {@code detail}, {@code code}) as
     * {@code application/problem+json}. Shared with filters that must answer before the
     * {@code ExceptionTranslationFilter} (e.g. {@link StreamTicketAuthFilter} on a store outage), so
     * every auth-layer error has the same shape.
     */
    void writeError(HttpServletResponse response, int status, String errorCode) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("detail", resolveDetail(errorCode));
        body.put("code", errorCode);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolveCode(HttpServletRequest request) {
        var attr = request.getAttribute("auth_error");
        if (attr instanceof String code) {
            return code;
        }
        return "UNAUTHENTICATED";
    }

    private String resolveDetail(String code) {
        return switch (code) {
            case "TOKEN_EXPIRED"  -> "The access token has expired. Please refresh or log in again.";
            case "INVALID_TOKEN"  -> "The provided token is invalid or malformed.";
            case "TICKET_INVALID" -> "The stream ticket is invalid, expired or already used. Request a new ticket.";
            case StreamTicketStoreUnavailableException.CODE ->
                    "Stream authentication is temporarily unavailable. Retry after the Retry-After delay.";
            default               -> "Authentication is required to access this resource.";
        };
    }
}
