package org.lions.backend.presentation.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** Monta respostas de download (PDF do termo, PNG do QR Code) com {@code Content-Disposition}. */
final class FileResponses {

    private FileResponses() {
    }

    static ResponseEntity<byte[]> attachment(byte[] content, String contentType, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentLength(content.length)
                .body(content);
    }

    static ResponseEntity<byte[]> inline(byte[] content, String contentType, String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentLength(content.length)
                .body(content);
    }
}
