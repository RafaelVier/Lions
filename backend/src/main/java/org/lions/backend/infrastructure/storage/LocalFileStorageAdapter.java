package org.lions.backend.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.lions.backend.application.port.FileStoragePort;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/** Armazenamento em disco local sob {@code lions.storage.base-dir}. Rejeita path traversal e não sobrescreve arquivos. */
@Slf4j
@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path baseDir;

    public LocalFileStorageAdapter(@Value("${lions.storage.base-dir:./data/storage}") String baseDir) {
        this.baseDir = Path.of(baseDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String folder, String fileName, byte[] content) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Nome do arquivo é obrigatório.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Conteúdo do arquivo é obrigatório.");
        }
        if (fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Nome de arquivo inválido: " + fileName);
        }
        String relative = (folder == null || folder.isBlank()) ? fileName : stripSlashes(folder) + "/" + fileName;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            throw new ConflictException("Arquivo já existe no armazenamento.", Map.of("path", relative));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar arquivo " + relative, e);
        }
        return toRelative(target);
    }

    @Override
    public byte[] read(String path) {
        Path target = resolve(path);
        if (!Files.isRegularFile(target)) {
            throw new ResourceNotFoundException("Arquivo", path);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler arquivo " + path, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.isRegularFile(resolve(path));
    }

    Path getBaseDir() {
        return baseDir;
    }

    private Path resolve(String relative) {
        if (relative == null || relative.isBlank()) {
            throw new IllegalArgumentException("Caminho do arquivo é obrigatório.");
        }
        String normalized = relative.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) {
            throw new IllegalArgumentException("Caminho absoluto não permitido: " + relative);
        }
        for (String segment : normalized.split("/")) {
            if (segment.equals("..")) {
                throw new IllegalArgumentException("Caminho inválido: " + relative);
            }
        }
        Path target;
        try {
            target = baseDir.resolve(normalized).normalize();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("Caminho inválido: " + relative, e);
        }
        if (!target.startsWith(baseDir) || target.equals(baseDir)) {
            throw new IllegalArgumentException("Caminho fora do diretório de armazenamento: " + relative);
        }
        return target;
    }

    private String toRelative(Path target) {
        return baseDir.relativize(target).toString().replace('\\', '/');
    }

    private static String stripSlashes(String folder) {
        String result = folder.replace('\\', '/');
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
