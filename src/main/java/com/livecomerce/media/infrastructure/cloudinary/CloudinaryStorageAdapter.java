package com.livecomerce.media.infrastructure.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.livecomerce.media.application.port.out.ImageStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
@RequiredArgsConstructor
class CloudinaryStorageAdapter implements ImageStoragePort {

    private final Cloudinary cloudinary;

    @Override
    public String store(byte[] data, String mimeType, String folder) {
        try {
            var result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "folder", "livecomerce/" + folder,
                    "resource_type", "image"
            ));
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Cloudinary upload failed", e);
        }
    }
}
