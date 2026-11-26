import pytest

from ragservice.infrastructure.config.settings import RagSettings
from ragservice.infrastructure.persistence.database import (
    DatabaseConfigurationError,
    createDatabaseEngine,
)


def testCreatesPostgresEngineWithoutConnecting() -> None:
    settings = RagSettings(databaseUrl="postgresql://rag:senha-ficticia@localhost/rag")

    engine = createDatabaseEngine(settings)
    try:
        assert engine.url.drivername == "postgresql+psycopg"
        assert engine.url.database == "rag"
        assert engine.hide_parameters
    finally:
        engine.dispose()


@pytest.mark.parametrize(
    "databaseUrl",
    [None, "", "url-invalida", "postgresql://localhost:porta/rag", "sqlite:///rag", "postgresql://localhost"],
)
def testRejectsInvalidDatabaseConfiguration(databaseUrl: str | None) -> None:
    with pytest.raises(DatabaseConfigurationError, match="RAG_DATABASE_URL"):
        createDatabaseEngine(RagSettings(databaseUrl=databaseUrl))
