from pathlib import Path

import pytest
from pydantic import ValidationError

from ragservice.infrastructure.config.settings import RagSettings


@pytest.fixture(autouse=True)
def clearRagEnvironment(monkeypatch: pytest.MonkeyPatch) -> None:
    for name in [
        "RAG_EMBEDDING_MODEL_NAME",
        "RAG_EMBEDDING_MODEL_REVISION",
        "RAG_EMBEDDING_CACHE_DIR",
        "RAG_EMBEDDING_BATCH_SIZE",
        "RAG_CHUNK_MAX_TOKENS",
        "RAG_CHUNK_OVERLAP_TOKENS",
        "RAG_DATABASE_URL",
    ]:
        monkeypatch.delenv(name, raising=False)


def testUsesDefaultValues() -> None:
    settings = RagSettings()

    assert settings.embeddingModelName == "intfloat/multilingual-e5-small"
    assert len(settings.embeddingModelRevision) == 40
    assert settings.embeddingCacheDir is None
    assert settings.embeddingBatchSize == 32
    assert settings.chunkMaxTokens == 350
    assert settings.chunkOverlapTokens == 60
    assert settings.databaseUrl is None


def testReadsEnvironmentVariables(monkeypatch: pytest.MonkeyPatch) -> None:
    cacheDir = Path("dados") / "modelos"
    monkeypatch.setenv("RAG_EMBEDDING_CACHE_DIR", str(cacheDir))
    monkeypatch.setenv("RAG_EMBEDDING_BATCH_SIZE", "8")
    monkeypatch.setenv("RAG_CHUNK_MAX_TOKENS", "200")
    monkeypatch.setenv("RAG_CHUNK_OVERLAP_TOKENS", "20")

    settings = RagSettings()

    assert settings.embeddingCacheDir == cacheDir
    assert settings.embeddingBatchSize == 8
    assert settings.chunkMaxTokens == 200
    assert settings.chunkOverlapTokens == 20


def testReadsDatabaseUrlAsSecret(monkeypatch: pytest.MonkeyPatch) -> None:
    databaseUrl = "postgresql://rag:senha-ficticia@localhost/rag"
    monkeypatch.setenv("RAG_DATABASE_URL", databaseUrl)

    settings = RagSettings()

    assert settings.databaseUrl is not None
    assert settings.databaseUrl.get_secret_value() == databaseUrl
    assert "senha-ficticia" not in repr(settings)


def testAcceptsFieldNames() -> None:
    settings = RagSettings(chunkMaxTokens=100, chunkOverlapTokens=10)

    assert settings.chunkMaxTokens == 100
    assert settings.chunkOverlapTokens == 10


@pytest.mark.parametrize(
    "values",
    [
        {"embeddingBatchSize": 0},
        {"chunkMaxTokens": 0},
        {"chunkOverlapTokens": -1},
        {"chunkMaxTokens": 50, "chunkOverlapTokens": 50},
        {"embeddingModelName": ""},
    ],
)
def testRejectsInvalidValues(values: dict) -> None:
    with pytest.raises(ValidationError):
        RagSettings(**values)
