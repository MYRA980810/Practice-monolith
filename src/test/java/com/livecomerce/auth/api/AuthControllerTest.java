package com.livecomerce.auth.api;

import com.livecomerce.auth.application.EmailAlreadyTakenException;
import com.livecomerce.auth.application.InvalidCredentialsException;
import com.livecomerce.auth.application.port.in.AuthResult;
import com.livecomerce.auth.application.port.in.AuthenticateUserUseCase;
import com.livecomerce.auth.application.port.in.RegisterUserUseCase;
import com.livecomerce.auth.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthenticateUserUseCase authenticateUseCase;
    @MockitoBean RegisterUserUseCase registerUseCase;

    private static final UUID USER_ID = UUID.randomUUID();

    private static final AuthResult AUTH_RESULT = AuthResult.of(
            "jwt-token", USER_ID, "seller@test.com", Role.SELLER
    );

    // --- LOGIN ---

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        when(authenticateUseCase.authenticate(any())).thenReturn(AUTH_RESULT);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"seller@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("seller@test.com"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void login_withInvalidCredentials_returns401ProblemDetail() throws Exception {
        when(authenticateUseCase.authenticate(any())).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"seller@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/invalid-credentials"));
    }

    @Test
    void login_withMissingFields_returns400WithErrors() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_withMalformedJson_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/malformed-request"));
    }

    // --- REGISTER ---

    @Test
    void register_withValidData_returns201WithToken() throws Exception {
        when(registerUseCase.register(any())).thenReturn(AUTH_RESULT);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"+1234",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void register_withExistingEmail_returns409ProblemDetail() throws Exception {
        when(registerUseCase.register(any())).thenThrow(new EmailAlreadyTakenException("seller@test.com"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"+1234",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/email-already-taken"));
    }

    @Test
    void register_withInvalidRole_returns400ProblemDetail() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"seller@test.com",
                                  "password":"secret123",
                                  "firstName":"John",
                                  "lastName":"Doe",
                                  "phone":"+1234",
                                  "role":"ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/malformed-request"));
    }

    @Test
    void register_withValidationErrors_returns400WithErrorsArray() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"not-an-email",
                                  "password":"123",
                                  "firstName":"",
                                  "lastName":"Doe",
                                  "role":"SELLER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
