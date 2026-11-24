package org.lions.backend.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lions.backend.domain.exception.ConflictException;
import org.lions.backend.domain.exception.ResourceNotFoundException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageAdapterTest {

    private LocalFileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalFileStorageAdapter("build/test-storage/" + UUID.randomUUID());
    }

    @Test
    void deveGravarELerQuandoCaminhoValido() {
        byte[] content = "termo".getBytes(StandardCharsets.UTF_8);

        String path = adapter.store("termos/2025", "emprestimo-1.pdf", content);

        assertThat(path).isEqualTo("termos/2025/emprestimo-1.pdf");
        assertThat(adapter.exists(path)).isTrue();
        assertThat(adapter.read(path)).isEqualTo(content);
        assertThat(Files.exists(adapter.getBaseDir().resolve(Path.of("termos", "2025", "emprestimo-1.pdf")))).isTrue();
    }

    @Test
    void deveGravarNaRaizQuandoSemPasta() {
        assertThat(adapter.store(null, "a.txt", new byte[]{1})).isEqualTo("a.txt");
    }

    @Test
    void deveRetornarConflitoQuandoArquivoJaExiste() {
        adapter.store("termos", "x.pdf", new byte[]{1});
        assertThatThrownBy(() -> adapter.store("termos", "x.pdf", new byte[]{2}))
                .isInstanceOf(ConflictException.class);
        assertThat(adapter.read("termos/x.pdf")).containsExactly(1);
    }

    @Test
    void deveRejeitarQuandoPathTraversalOuCaminhoAbsoluto() {
        assertThatThrownBy(() -> adapter.store("../fora", "x.pdf", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.store("termos", "../x.pdf", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.store("/etc", "passwd", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.read("termos/../../segredo.txt"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.read("C:\\Windows\\win.ini"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRetornarNaoEncontradoQuandoArquivoInexistente() {
        assertThat(adapter.exists("nada.pdf")).isFalse();
        assertThatThrownBy(() -> adapter.read("nada.pdf")).isInstanceOf(ResourceNotFoundException.class);
    }
}
