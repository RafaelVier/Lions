from functools import lru_cache
from pathlib import Path

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class RagSettings(BaseSettings):
    model_config = SettingsConfigDict(frozen=True, validate_by_name=True, validate_by_alias=True)

    embeddingModelName: str = Field(
        default="intfloat/multilingual-e5-small",
        min_length=1,
        alias="RAG_EMBEDDING_MODEL_NAME",
    )
    embeddingModelRevision: str = Field(
        default="614241f622f53c4eeff9890bdc4f31cfecc418b3",
        min_length=1,
        alias="RAG_EMBEDDING_MODEL_REVISION",
    )
    embeddingCacheDir: Path | None = Field(default=None, alias="RAG_EMBEDDING_CACHE_DIR")
    embeddingBatchSize: int = Field(default=32, gt=0, alias="RAG_EMBEDDING_BATCH_SIZE")
    chunkMaxTokens: int = Field(default=350, gt=0, alias="RAG_CHUNK_MAX_TOKENS")
    chunkOverlapTokens: int = Field(default=60, ge=0, alias="RAG_CHUNK_OVERLAP_TOKENS")

    @model_validator(mode="after")
    def validateChunking(self) -> "RagSettings":
        if self.chunkOverlapTokens >= self.chunkMaxTokens:
            raise ValueError("A sobreposição deve ser menor que o tamanho máximo do trecho.")
        return self


@lru_cache
def getSettings() -> RagSettings:
    return RagSettings()
