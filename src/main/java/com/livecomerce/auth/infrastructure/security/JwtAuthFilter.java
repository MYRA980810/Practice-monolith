package com.livecomerce.auth.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.TransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <access token>}.
 * <p>
 * An expired token sets {@code auth_error=TOKEN_EXPIRED} and any other bad token (malformed, bad
 * signature, unknown or inactive user) sets {@code INVALID_TOKEN}; the chain continues and
 * {@link RestAuthenticationEntryPoint} answers 401 if the endpoint needs authentication.
 * <p>
 * If the user store is down while loading the user, the request is answered right here with 503
 * {@code AUTH_STORE_UNAVAILABLE} and {@code Retry-After}: an outage must never look like a bad
 * token, or clients would drop a valid session.
 */
@RequiredArgsConstructor
class JwtAuthFilter extends OncePerRequestFilter {

    static final String AUTH_STORE_UNAVAILABLE = "AUTH_STORE_UNAVAILABLE";
    static final Duration RETRY_AFTER = Duration.ofSeconds(5);

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            var token = authHeader.substring(7);
            var claims = jwtService.validateAndExtract(token);

            var type = claims.get("type", String.class);
            if ("otp-pending".equals(type) || "oauth-pending".equals(type)
                    || "reset-pending".equals(type) || "password-reset".equals(type)
                    || "change-password-pending".equals(type) || "change-password".equals(type)) {
                chain.doFilter(request, response);
                return;
            }

            var userId = claims.getSubject();

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(userId);
                if (!userDetails.isEnabled()) {
                    // Same code as a bad token on purpose: the client tries one refresh, which is
                    // also rejected for an inactive user, and then logs out.
                    log.warn("Rejected JWT of inactive user '{}'", userId);
                    request.setAttribute("auth_error", "INVALID_TOKEN");
                } else {
                    var auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("Authenticated user '{}' with authorities: {}", userId, userDetails.getAuthorities());
                }
            }
        } catch (DataAccessException | TransactionException e) {
            rejectAsUnavailable(response, e);
            return;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            request.setAttribute("auth_error", "TOKEN_EXPIRED");
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            request.setAttribute("auth_error", "INVALID_TOKEN");
        }

        chain.doFilter(request, response);
    }

    /**
     * Fails closed: nothing is authenticated and the chain is not continued. Answered here because
     * this filter runs before the {@code ExceptionTranslationFilter} and no
     * {@code @RestControllerAdvice} applies.
     */
    private void rejectAsUnavailable(HttpServletResponse response, Exception e) throws IOException {
        log.error("JWT authentication unavailable (user store failure); answering 503 {}",
                AUTH_STORE_UNAVAILABLE, e);
        SecurityContextHolder.clearContext();
        errorWriter.writeUnavailable(response, AUTH_STORE_UNAVAILABLE, RETRY_AFTER);
    }
}
