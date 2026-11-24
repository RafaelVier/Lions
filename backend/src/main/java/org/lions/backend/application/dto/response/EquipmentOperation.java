package org.lions.backend.application.dto.response;

/** Operações oferecidas na leitura do QR Code conforme o status do item (RN15) — a leitura nunca executa nada. */
public enum EquipmentOperation {
    EMPRESTAR("Registrar empréstimo"),
    REGISTRAR_ASSINATURA("Registrar assinatura do termo"),
    REGISTRAR_DEVOLUCAO("Registrar devolução"),
    RENOVAR("Renovar empréstimo"),
    ABRIR_MANUTENCAO("Abrir manutenção"),
    LANCAR_CUSTO("Lançar custo de manutenção"),
    ENCERRAR_MANUTENCAO("Encerrar manutenção"),
    BAIXAR("Dar baixa"),
    EDITAR("Editar cadastro"),
    IMPRIMIR_QRCODE("Imprimir etiqueta (QR Code)"),
    VER_HISTORICO("Ver histórico");

    private final String description;

    EquipmentOperation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
