package com.livecomerce.media.application;

import com.livecomerce.media.application.port.in.UploadImageUseCase.UploadImageCommand;
import com.livecomerce.media.application.port.out.ImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadImageServiceTest {

    @Mock ImageStoragePort storagePort;
    @InjectMocks UploadImageService service;

    private static final byte[] VALID_DATA = new byte[100];
    private static final String JPEG = "image/jpeg";
    private static final String PNG  = "image/png";
    private static final String WEBP = "image/webp";

    @Test
    void upload_withJpeg_storesAndReturnsUrl() {
        when(storagePort.store(any(), eq(JPEG), eq("product"))).thenReturn("https://cdn.example.com/img.jpg");

        var url = service.upload(new UploadImageCommand(VALID_DATA, JPEG, "product"));

        assertThat(url).isEqualTo("https://cdn.example.com/img.jpg");
        verify(storagePort).store(VALID_DATA, JPEG, "product");
    }

    @Test
    void upload_withPng_accepted() {
        when(storagePort.store(any(), eq(PNG), any())).thenReturn("https://cdn.example.com/img.png");

        var url = service.upload(new UploadImageCommand(VALID_DATA, PNG, "general"));

        assertThat(url).isNotBlank();
    }

    @Test
    void upload_withWebp_accepted() {
        when(storagePort.store(any(), eq(WEBP), any())).thenReturn("https://cdn.example.com/img.webp");

        var url = service.upload(new UploadImageCommand(VALID_DATA, WEBP, "avatar"));

        assertThat(url).isNotBlank();
    }

    @Test
    void upload_withPdf_throwsInvalidImageException() {
        assertThatThrownBy(() ->
                service.upload(new UploadImageCommand(VALID_DATA, "application/pdf", "product")))
                .isInstanceOf(InvalidImageException.class);

        verify(storagePort, never()).store(any(), any(), any());
    }

    @Test
    void upload_withGif_throwsInvalidImageException() {
        assertThatThrownBy(() ->
                service.upload(new UploadImageCommand(VALID_DATA, "image/gif", "product")))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void upload_withNullMimeType_throwsInvalidImageException() {
        assertThatThrownBy(() ->
                service.upload(new UploadImageCommand(VALID_DATA, null, "product")))
                .isInstanceOf(InvalidImageException.class);
    }

    @Test
    void upload_withOversizedFile_throwsInvalidImageException() {
        var bigData = new byte[6 * 1024 * 1024]; // 6 MB > 5 MB limit

        assertThatThrownBy(() ->
                service.upload(new UploadImageCommand(bigData, JPEG, "product")))
                .isInstanceOf(InvalidImageException.class);

        verify(storagePort, never()).store(any(), any(), any());
    }

    @Test
    void upload_withExactlyFiveMb_isAccepted() {
        var fiveMb = new byte[5 * 1024 * 1024];
        when(storagePort.store(any(), any(), any())).thenReturn("https://cdn.example.com/img.jpg");

        var url = service.upload(new UploadImageCommand(fiveMb, JPEG, "product"));

        assertThat(url).isNotBlank();
    }

    // --- uploadAll ---

    @Test
    void uploadAll_withThreeValidCommands_returnsThreeUrlsInOrder() {
        when(storagePort.store(any(), eq(JPEG), any()))
                .thenReturn("https://cdn.example.com/1.jpg")
                .thenReturn("https://cdn.example.com/2.jpg")
                .thenReturn("https://cdn.example.com/3.jpg");

        var commands = List.of(
                new UploadImageCommand(VALID_DATA, JPEG, "product"),
                new UploadImageCommand(VALID_DATA, JPEG, "product"),
                new UploadImageCommand(VALID_DATA, JPEG, "product")
        );

        var urls = service.uploadAll(commands);

        assertThat(urls).containsExactly(
                "https://cdn.example.com/1.jpg",
                "https://cdn.example.com/2.jpg",
                "https://cdn.example.com/3.jpg"
        );
        verify(storagePort, times(3)).store(any(), any(), any());
    }

    @Test
    void uploadAll_withInvalidTypeInAnyFile_throwsAndNeverCallsStore() {
        var commands = List.of(
                new UploadImageCommand(VALID_DATA, JPEG, "product"),
                new UploadImageCommand(VALID_DATA, "application/pdf", "product"),
                new UploadImageCommand(VALID_DATA, JPEG, "product")
        );

        assertThatThrownBy(() -> service.uploadAll(commands))
                .isInstanceOf(InvalidImageException.class);

        verify(storagePort, never()).store(any(), any(), any());
    }

    @Test
    void uploadAll_withOversizedFileInBatch_throwsAndNeverCallsStore() {
        var bigData = new byte[6 * 1024 * 1024];
        var commands = List.of(
                new UploadImageCommand(VALID_DATA, JPEG, "product"),
                new UploadImageCommand(bigData, JPEG, "product")
        );

        assertThatThrownBy(() -> service.uploadAll(commands))
                .isInstanceOf(InvalidImageException.class);

        verify(storagePort, never()).store(any(), any(), any());
    }
}
