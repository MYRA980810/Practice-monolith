package com.livecomerce.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    private MockHttpServletResponse response;
    private RestAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        entryPoint = new RestAuthenticationEntryPoint(new ObjectMapper());
    }

    private static final AuthenticationException AUTH_EXCEPTION =
            new AuthenticationException("Unauthorized") {};

    @Test
    void commence_withNoAttribute_returns401WithUnauthenticatedCode() throws Exception {
        when(request.getAttribute("auth_error")).thenReturn(null);

        entryPoint.commence(request, response, AUTH_EXCEPTION);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        @SuppressWarnings("unchecked")
        var body = new ObjectMapper().readValue(response.getContentAsByteArray(), Map.class);
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("code")).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void commence_withTokenExpiredAttribute_returns401WithTokenExpiredCode() throws Exception {
        when(request.getAttribute("auth_error")).thenReturn("TOKEN_EXPIRED");

        entryPoint.commence(request, response, AUTH_EXCEPTION);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        @SuppressWarnings("unchecked")
        var body = new ObjectMapper().readValue(response.getContentAsByteArray(), Map.class);
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("code")).isEqualTo("TOKEN_EXPIRED");
        assertThat(body.get("detail")).isNotNull();
    }

    @Test
    void commence_withInvalidTokenAttribute_returns401WithInvalidTokenCode() throws Exception {
        when(request.getAttribute("auth_error")).thenReturn("INVALID_TOKEN");

        entryPoint.commence(request, response, AUTH_EXCEPTION);

        assertThat(response.getStatus()).isEqualTo(401);

        @SuppressWarnings("unchecked")
        var body = new ObjectMapper().readValue(response.getContentAsByteArray(), Map.class);
        assertThat(body.get("code")).isEqualTo("INVALID_TOKEN");
    }
}
