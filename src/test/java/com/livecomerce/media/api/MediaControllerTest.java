package com.livecomerce.media.api;

import com.livecomerce.media.application.InvalidImageException;
import com.livecomerce.media.application.port.in.UploadImageUseCase;
import com.livecomerce.shared.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MediaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
class MediaControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean UploadImageUseCase uploadImageUseCase;

    @BeforeEach
    void setUpPrincipal() {
        var principal = new UserPrincipal(
                UUID.randomUUID(), "user@test.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")), true
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadImage_withValidJpeg_returns200WithUrl() throws Exception {
        when(uploadImageUseCase.upload(any())).thenReturn("https://res.cloudinary.com/test/image.jpg");

        var file = new MockMultipartFile("file", "image.jpg", "image/jpeg", new byte[100]);

        mvc.perform(multipart("/api/media/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/test/image.jpg"));
    }

    @Test
    void uploadImage_withContextParam_returns200() throws Exception {
        when(uploadImageUseCase.upload(any())).thenReturn("https://res.cloudinary.com/test/avatar.png");

        var file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[100]);

        mvc.perform(multipart("/api/media/images").file(file).param("context", "avatar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").isNotEmpty());
    }

    @Test
    void uploadImage_withInvalidType_returns400() throws Exception {
        when(uploadImageUseCase.upload(any())).thenThrow(new InvalidImageException("Unsupported image type"));

        var file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[100]);

        mvc.perform(multipart("/api/media/images").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImage_withMissingFile_returns400() throws Exception {
        mvc.perform(multipart("/api/media/images"))
                .andExpect(status().isBadRequest());
    }
}
