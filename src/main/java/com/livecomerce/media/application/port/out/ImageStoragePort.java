package com.livecomerce.media.application.port.out;

public interface ImageStoragePort {

    String store(byte[] data, String mimeType, String folder);
}
