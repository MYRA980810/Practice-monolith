package com.livecomerce.media.application;

import com.livecomerce.media.application.port.in.UploadImageUseCase;
import com.livecomerce.media.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadImageService implements UploadImageUseCase {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    private final ImageStoragePort storagePort;

    @Override
    public String upload(UploadImageCommand command) {
        if (command.mimeType() == null || !ALLOWED_TYPES.contains(command.mimeType())) {
            throw new InvalidImageException("only JPEG, PNG and WebP are allowed");
        }
        if (command.data().length > MAX_BYTES) {
            throw new InvalidImageException("file exceeds the 5 MB limit");
        }
        var folder = command.context() != null ? command.context() : "general";
        return storagePort.store(command.data(), command.mimeType(), folder);
    }
}
