package org.lions.backend.application.port;

/** Geração de QR Code (RN10): determinística — o mesmo conteúdo e tamanho geram sempre a mesma imagem. */
public interface QrCodePort {

    /** PNG quadrado de {@code size} pixels codificando {@code content}. */
    byte[] generatePng(String content, int size);
}
