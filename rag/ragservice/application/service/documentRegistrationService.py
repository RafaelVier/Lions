from ragservice.domain.documentLimits import MAX_DOCUMENT_SIZE_BYTES
from ragservice.domain.entity.documentRegistration import DocumentRegistration
from ragservice.domain.port.documentRepositoryPort import DocumentRepositoryPort


class DocumentRegistrationError(ValueError):
    pass


class DocumentRegistrationService:
    def __init__(self, repository: DocumentRepositoryPort) -> None:
        self.repository = repository

    def registerDocument(self, title: str, fileName: str, content: bytes) -> DocumentRegistration:
        if not title.strip():
            raise DocumentRegistrationError("Informe o título do documento.")
        if not fileName.strip():
            raise DocumentRegistrationError("Informe o nome do arquivo.")
        if not content:
            raise DocumentRegistrationError("O arquivo não pode estar vazio.")
        if len(content) > MAX_DOCUMENT_SIZE_BYTES:
            raise DocumentRegistrationError("O arquivo excede o limite de 5 MiB.")

        return self.repository.registerDocument(title, fileName, content)
