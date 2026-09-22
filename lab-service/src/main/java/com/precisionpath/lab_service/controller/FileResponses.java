package com.precisionpath.lab_service.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

final class FileResponses {

    private FileResponses() {
    }

    static <T> ResponseEntity<T> file(T body, String fileName, String contentType, boolean inline) {

        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }

    static ResponseEntity<Resource> resource(Resource resource, String fileName, String contentType) {
        return file(resource, fileName, contentType, true);
    }
}
