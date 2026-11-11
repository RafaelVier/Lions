package org.lions.backend.application.port;

/** Armazenamento de arquivos (termos PDF, assinaturas digitalizadas, fotos). Caminhos são relativos ao storage. */
public interface FileStoragePort {

    /**
     * Grava o arquivo em {@code folder/fileName} e retorna o caminho relativo (separador {@code /}).
     * Não sobrescreve arquivo existente (termos são imutáveis — RN61).
     */
    String store(String folder, String fileName, byte[] content);

    /** Lê o arquivo pelo caminho relativo retornado em {@link #store}. */
    byte[] read(String path);

    boolean exists(String path);
}
