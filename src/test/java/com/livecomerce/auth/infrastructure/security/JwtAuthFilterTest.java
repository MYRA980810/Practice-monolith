package com.livecomerce.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.shared.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService,
                new RestAuthenticationEntryPoint(new ObjectMapper()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withExpiredToken_setsTokenExpiredAttributeAndContinues() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired.token.here");

        // Simulate ExpiredJwtException (need a non-null header, claims, and body — use minimal mock)
        var expired = mock(ExpiredJwtException.class);
        when(expired.getMessage()).thenReturn("JWT expired");
        when(jwtService.validateAndExtract(anyString())).thenThrow(expired);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("auth_error")).isEqualTo("TOKEN_EXPIRED");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidToken_setsInvalidTokenAttributeAndContinues() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid.token.here");

        when(jwtService.validateAndExtract(anyString())).thenThrow(new JwtException("bad signature"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("auth_error")).isEqualTo("INVALID_TOKEN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNoAuthHeader_doesNotSetAttributeAndContinues() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("auth_error")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"otp-pending", "oauth-pending", "reset-pending", "password-reset",
            "change-password-pending", "change-password"})
    void doFilterInternal_withNarrowPurposeTokenType_doesNotAuthenticate(String type) throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer narrow-purpose.token.here");

        when(jwtService.validateAndExtract(anyString())).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn(type);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withActiveUser_authenticatesAndContinues() throws Exception {
        var userId = UUID.randomUUID();
        var request = bearerRequestFor(userId.toString());
        var response = new MockHttpServletResponse();
        var principal = principal(userId, true);
        when(userDetailsService.loadUserByUsername(userId.toString())).thenReturn(principal);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(principal);
        assertThat(request.getAttribute("auth_error")).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInactiveUser_doesNotAuthenticate_setsInvalidTokenAndContinues() throws Exception {
        var userId = UUID.randomUUID();
        var request = bearerRequestFor(userId.toString());
        var response = new MockHttpServletResponse();
        when(userDetailsService.loadUserByUsername(userId.toString())).thenReturn(principal(userId, false));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("auth_error")).isEqualTo("INVALID_TOKEN");
        verify(filterChain).doFilter(request, response);
    }

    private static UserPrincipal principal(UUID userId, boolean active) {
        return new UserPrincipal(userId, "u@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")), active);
    }

    // --- user store unavailable: 503, never INVALID_TOKEN, never authenticated ---

    @Test
    void doFilterInternal_whenUserLookupHitsDataAccessFailure_answers503WithoutContinuingTheChain() throws Exception {
        var request = bearerRequestFor(UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        filter.doFilterInternal(request, response, filterChain);

        assertStoreUnavailable(request, response);
    }

    @Test
    void doFilterInternal_whenUserLookupHitsTransactionFailure_answers503WithoutContinuingTheChain() throws Exception {
        var request = bearerRequestFor(UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new CannotCreateTransactionException("no connection"));

        filter.doFilterInternal(request, response, filterChain);

        assertStoreUnavailable(request, response);
    }

    @Test
    void doFilterInternal_whenUserNoLongerExists_setsInvalidTokenAndContinues() throws Exception {
        var request = bearerRequestFor(UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        when(userDetailsService.loadUserByUsername(anyString()))
                .thenThrow(new UsernameNotFoundException("gone"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(request.getAttribute("auth_error")).isEqualTo("INVALID_TOKEN");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest bearerRequestFor(String userId) {
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.token.here");
        when(jwtService.validateAndExtract(anyString())).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId);
        return request;
    }

    private void assertStoreUnavailable(MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(response.getContentAsString())
                .contains("\"code\":\"AUTH_STORE_UNAVAILABLE\"")
                .contains("\"status\":503");
        assertThat(request.getAttribute("auth_error")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(any(), any());
    }
}
