import pytest

from ragservice.application.service.documentRegistrationService import (
    DocumentRegistrationError,
    DocumentRegistrationService,
)
from ragservice.domain.entity.documentRegistration import DocumentRegistration
from ragservice.domain.port.documentRepositoryPort import DocumentRepositoryPort


class DocumentRepositoryStub(DocumentRepositoryPort):
    def __init__(self) -> None:
        self.documents: list[tuple[str, str, bytes]] = []

    def registerDocument(self, title: str, fileName: str, content: bytes) -> DocumentRegistration:
        self.documents.append((title, fileName, content))
        return DocumentRegistration(documentId=1, versionId=2, jobId=3, status="pendente")


def testRegistersOriginalBytesAndReturnsPendingJob() -> None:
    repository = DocumentRepositoryStub()
    service = DocumentRegistrationService(repository)
    content = "Informações sobre empréstimos".encode("utf-8")

    result = service.registerDocument("Orientações", "orientacoes.txt", content)

    assert result == DocumentRegistration(documentId=1, versionId=2, jobId=3, status="pendente")
    assert repository.documents == [("Orientações", "orientacoes.txt", content)]


@pytest.mark.parametrize(
    ("title", "fileName", "content", "message"),
    [
        (" \t", "orientacoes.txt", b"a", "título"),
        ("Orientações", "\n", b"a", "nome do arquivo"),
        ("Orientações", "orientacoes.txt", b"", "vazio"),
        ("Orientações", "orientacoes.txt", b"a" * (5 * 1024 * 1024 + 1), "5 MiB"),
    ],
)
def testRejectsInvalidDocumentsBeforePersistence(
    title: str, fileName: str, content: bytes, message: str,
) -> None:
    repository = DocumentRepositoryStub()

    with pytest.raises(DocumentRegistrationError, match=message):
        DocumentRegistrationService(repository).registerDocument(title, fileName, content)

    assert repository.documents == []


@pytest.mark.parametrize("size", [1, 5 * 1024 * 1024])
def testAcceptsBothContentSizeBoundaries(size: int) -> None:
    repository = DocumentRepositoryStub()
    content = b"a" * size

    DocumentRegistrationService(repository).registerDocument("Orientações", "orientacoes.txt", content)

    assert repository.documents == [("Orientações", "orientacoes.txt", content)]
