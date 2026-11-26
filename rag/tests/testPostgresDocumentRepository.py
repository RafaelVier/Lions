import os
from collections.abc import Iterator
from contextlib import ExitStack
from pathlib import Path
from uuid import uuid4

import pytest
from alembic import command
from alembic.config import Config
from sqlalchemy import text
from sqlalchemy.engine import Engine
from sqlalchemy.schema import CreateSchema, DropSchema

from ragservice.application.service.documentRegistrationService import DocumentRegistrationService
from ragservice.domain.port.documentRepositoryPort import DocumentPersistenceError
from ragservice.infrastructure.config.settings import RagSettings
from ragservice.infrastructure.persistence.database import createDatabaseEngine
from ragservice.infrastructure.persistence.postgresDocumentRepository import PostgresDocumentRepository


def dropTestSchema(engine: Engine, schemaName: str) -> None:
    with engine.begin() as connection:
        connection.execute(DropSchema(schemaName, cascade=True))


@pytest.fixture
def postgresEngine(monkeypatch: pytest.MonkeyPatch) -> Iterator[Engine]:
    databaseUrl = os.environ.get("RAG_TEST_DATABASE_URL")
    if not databaseUrl:
        pytest.skip("Defina RAG_TEST_DATABASE_URL para rodar com PostgreSQL.")

    schemaName = f"rag_test_{uuid4().hex}"
    with ExitStack() as cleanup:
        baseEngine = createDatabaseEngine(RagSettings(databaseUrl=databaseUrl))
        cleanup.callback(baseEngine.dispose)
        with baseEngine.begin() as connection:
            connection.execute(CreateSchema(schemaName))
        cleanup.callback(dropTestSchema, baseEngine, schemaName)

        testUrl = baseEngine.url.update_query_dict({"options": f"-csearch_path={schemaName}"})
        isolatedUrl = testUrl.render_as_string(hide_password=False)
        engine = createDatabaseEngine(RagSettings(databaseUrl=isolatedUrl))
        cleanup.callback(engine.dispose)

        monkeypatch.setenv("RAG_DATABASE_URL", isolatedUrl)
        config = Config(str(Path(__file__).resolve().parents[1] / "alembic.ini"))
        command.upgrade(config, "head")
        yield engine


def testPersistsOriginalAndPendingJobAfterReconnecting(postgresEngine: Engine) -> None:
    service = DocumentRegistrationService(PostgresDocumentRepository(postgresEngine))
    content = b"Orientacoes\x00para emprestimo"
    registration = service.registerDocument("Orientações", "orientacoes.txt", content)

    postgresEngine.dispose()
    with postgresEngine.connect() as connection:
        row = connection.execute(
            text(
                "SELECT d.title, v.version_number, v.file_name, v.content, j.status "
                "FROM documents AS d "
                "JOIN document_versions AS v ON v.document_id = d.id "
                "JOIN ingestion_jobs AS j ON j.version_id = v.id "
                "WHERE d.id = :documentId AND v.id = :versionId AND j.id = :jobId"
            ),
            {
                "documentId": registration.documentId,
                "versionId": registration.versionId,
                "jobId": registration.jobId,
            },
        ).one()

    assert registration.status == "pendente"
    assert row == ("Orientações", 1, "orientacoes.txt", content, "pendente")


def testRollsBackDocumentAndVersionWhenJobInsertFails(postgresEngine: Engine) -> None:
    with postgresEngine.begin() as connection:
        connection.execute(
            text("ALTER TABLE ingestion_jobs ADD CONSTRAINT test_reject_pending CHECK (status <> 'pendente')")
        )

    service = DocumentRegistrationService(PostgresDocumentRepository(postgresEngine))
    with pytest.raises(DocumentPersistenceError):
        service.registerDocument("Orientações", "orientacoes.txt", b"texto")

    with postgresEngine.connect() as connection:
        counts = connection.execute(
            text(
                "SELECT (SELECT count(*) FROM documents), "
                "(SELECT count(*) FROM document_versions), (SELECT count(*) FROM ingestion_jobs)"
            ),
        ).one()

    assert counts == (0, 0, 0)
