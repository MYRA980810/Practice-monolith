package com.livecomerce.media.application.port.in;

public interface UploadImageUseCase {

    String upload(UploadImageCommand command);

    record UploadImageCommand(byte[] data, String mimeType, String context) {}
}
