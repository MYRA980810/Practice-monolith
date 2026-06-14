package com.livecomerce.media.application;

import com.livecomerce.media.application.port.in.UploadImageUseCase;
import com.livecomerce.media.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UploadImageService implements UploadImageUseCase {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    private final ImageStoragePort storagePort;

    @Override
    public String upload(UploadImageCommand command) {
        validate(command);
        var folder = command.context() != null ? command.context() : "general";
        return storagePort.store(command.data(), command.mimeType(), folder);
    }

    @Override
    public List<String> uploadAll(List<UploadImageCommand> commands) {
        commands.forEach(this::validate);
        return commands.stream()
                .map(cmd -> {
                    var folder = cmd.context() != null ? cmd.context() : "general";
                    return storagePort.store(cmd.data(), cmd.mimeType(), folder);
                })
                .toList();
    }

    private void validate(UploadImageCommand command) {
        if (command.mimeType() == null || !ALLOWED_TYPES.contains(command.mimeType())) {
            throw new InvalidImageException("only JPEG, PNG and WebP are allowed");
        }
        if (command.data().length > MAX_BYTES) {
            throw new InvalidImageException("file exceeds the 5 MB limit");
        }
    }
}
