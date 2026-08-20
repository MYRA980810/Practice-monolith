package com.livecomerce.auth.infrastructure.security;

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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
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
        filter = new JwtAuthFilter(jwtService, userDetailsService);
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
}
