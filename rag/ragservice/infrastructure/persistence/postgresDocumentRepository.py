from sqlalchemy import text
from sqlalchemy.engine import Engine
from sqlalchemy.exc import SQLAlchemyError

from ragservice.domain.entity.documentRegistration import DocumentRegistration
from ragservice.domain.port.documentRepositoryPort import (
    DocumentPersistenceError,
    DocumentRepositoryPort,
)


class PostgresDocumentRepository(DocumentRepositoryPort):
    def __init__(self, engine: Engine) -> None:
        self.engine = engine

    def registerDocument(self, title: str, fileName: str, content: bytes) -> DocumentRegistration:
        try:
            with self.engine.begin() as connection:
                documentId = connection.execute(
                    text("INSERT INTO documents (title) VALUES (:title) RETURNING id"),
                    {"title": title},
                ).scalar_one()
                versionId = connection.execute(
                    text(
                        "INSERT INTO document_versions (document_id, version_number, file_name, content) "
                        "VALUES (:documentId, 1, :fileName, :content) RETURNING id"
                    ),
                    {"documentId": documentId, "fileName": fileName, "content": content},
                ).scalar_one()
                jobId, status = connection.execute(
                    text("INSERT INTO ingestion_jobs (version_id) VALUES (:versionId) RETURNING id, status"),
                    {"versionId": versionId},
                ).one()
        except SQLAlchemyError:
            raise DocumentPersistenceError("Não foi possível registrar o documento no banco do RAG.") from None

        return DocumentRegistration(documentId, versionId, jobId, status)
