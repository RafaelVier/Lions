package org.lions.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Lote de operações offline enviado ao reconectar (RN60). */
public record SyncBatchRequest(
        @NotEmpty(message = "Informe ao menos uma operação.")
        @Size(max = 200, message = "Envie no máximo 200 operações por lote.")
        List<@Valid SyncOperationRequest> operations) {
}
