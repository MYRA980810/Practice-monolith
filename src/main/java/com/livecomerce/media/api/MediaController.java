package com.livecomerce.media.api;

import com.livecomerce.media.application.port.in.UploadImageUseCase;
import com.livecomerce.media.application.port.in.UploadImageUseCase.UploadImageCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
class MediaController {

    private final UploadImageUseCase uploadImageUseCase;

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<UploadImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "context", defaultValue = "general") String context) throws IOException {

        var url = uploadImageUseCase.upload(
                new UploadImageCommand(file.getBytes(), file.getContentType(), context));

        return ResponseEntity.ok(new UploadImageResponse(url));
    }

    @PostMapping(value = "/images/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<UploadImagesResponse> uploadImages(
            @RequestParam List<MultipartFile> files,
            @RequestParam(defaultValue = "general") String context) throws IOException {

        var commands = new ArrayList<UploadImageCommand>();
        for (var file : files) {
            commands.add(new UploadImageCommand(file.getBytes(), file.getContentType(), context));
        }
        var urls = uploadImageUseCase.uploadAll(commands);
        return ResponseEntity.ok(new UploadImagesResponse(urls));
    }
}
