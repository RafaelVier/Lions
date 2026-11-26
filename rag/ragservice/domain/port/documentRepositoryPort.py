from abc import ABC, abstractmethod

from ragservice.domain.entity.documentRegistration import DocumentRegistration


class DocumentPersistenceError(RuntimeError):
    pass


class DocumentRepositoryPort(ABC):
    @abstractmethod
    def registerDocument(self, title: str, fileName: str, content: bytes) -> DocumentRegistration:
        pass
