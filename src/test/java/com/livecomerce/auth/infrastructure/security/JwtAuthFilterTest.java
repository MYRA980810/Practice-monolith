package com.livecomerce.auth.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService);
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
}
