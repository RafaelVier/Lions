from sentence_transformers import SentenceTransformer

from ragservice.infrastructure.config.settings import RagSettings
from ragservice.infrastructure.embedding.e5EmbeddingAdapter import E5_MAX_SEQUENCE_LENGTH


def loadE5Model(settings: RagSettings, localFilesOnly: bool = True) -> SentenceTransformer:
    cacheFolder = str(settings.embeddingCacheDir) if settings.embeddingCacheDir else None
    model = SentenceTransformer(
        settings.embeddingModelName,
        device="cpu",
        cache_folder=cacheFolder,
        revision=settings.embeddingModelRevision,
        local_files_only=localFilesOnly,
    )
    model.max_seq_length = E5_MAX_SEQUENCE_LENGTH
    return model
