package com.livecomerce.media.application.port.in;

import java.util.List;

public interface UploadImageUseCase {

    String upload(UploadImageCommand command);

    List<String> uploadAll(List<UploadImageCommand> commands);

    record UploadImageCommand(byte[] data, String mimeType, String context) {}
}
