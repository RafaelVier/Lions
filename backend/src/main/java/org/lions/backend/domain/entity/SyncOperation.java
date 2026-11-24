package org.lions.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lions.backend.domain.enums.SyncOperationType;
import org.lions.backend.domain.enums.SyncResultStatus;

import java.time.LocalDateTime;

/**
 * Operação feita sem conexão e recebida na sincronização (RN60). {@code clientOperationId} (gerado no dispositivo)
 * garante idempotência: reenviar a mesma operação não a aplica de novo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncOperation {

    private Long id;
    private String clientOperationId;
    private SyncOperationType type;
    private Long beneficiaryId;
    private Long equipmentId;
    private Long loanId;
    /** Data/hora em que a operação foi feita no dispositivo. */
    private LocalDateTime occurredAt;
    /** Data/hora em que o servidor recebeu a operação. */
    private LocalDateTime receivedAt;
    private String username;
    private SyncResultStatus result;
    private String message;
    /** Empréstimo criado ou alterado pela operação aplicada. */
    private Long resultingLoanId;
    private Long conflictId;
}
