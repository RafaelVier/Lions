import numpy as np

from ragservice.domain.port.embeddingPort import EmbeddingError, EmbeddingPort
from ragservice.infrastructure.embedding.e5TokenCounter import E5TokenCounter

E5_MAX_SEQUENCE_LENGTH = 512
PASSAGE_PREFIX = "passage: "
QUERY_PREFIX = "query: "
SPECIAL_TOKEN_COUNT = 2


class E5EmbeddingAdapter(EmbeddingPort):

    def __init__(self, model, batchSize: int):
        if batchSize <= 0:
            raise EmbeddingError("O tamanho do lote deve ser positivo.")

        modelDimension = model.get_embedding_dimension()
        if not modelDimension:
            raise EmbeddingError("O modelo de embeddings não informou sua dimensão.")

        self.model = model
        self.batchSize = batchSize
        self.modelDimension = modelDimension

    @property
    def dimension(self) -> int:
        return self.modelDimension

    def maxPassageTokens(self) -> int:
        prefixTokens = E5TokenCounter(self.model.tokenizer).countTokens([PASSAGE_PREFIX])[0]
        return E5_MAX_SEQUENCE_LENGTH - prefixTokens - SPECIAL_TOKEN_COUNT

    def ensurePassageLimit(self, maxTokens: int) -> None:
        if maxTokens > self.maxPassageTokens():
            raise EmbeddingError("O tamanho máximo do trecho excede o limite do modelo de embeddings.")

    def embedPassages(self, texts: list[str]) -> np.ndarray:
        if not texts:
            raise EmbeddingError("Informe ao menos um trecho para gerar embeddings.")
        return self.encode(texts, PASSAGE_PREFIX)

    def embedQuery(self, text: str) -> np.ndarray:
        return self.encode([text], QUERY_PREFIX)[0]

    def encode(self, texts: list[str], prefix: str) -> np.ndarray:
        if any(not text.strip() for text in texts):
            raise EmbeddingError("Não é possível gerar embeddings de textos vazios.")

        # O prefixo explícito também substitui qualquer prompt padrão do modelo
        embeddings = self.model.encode(
            texts,
            prompt=prefix,
            batch_size=self.batchSize,
            show_progress_bar=False,
            convert_to_numpy=True,
            normalize_embeddings=True,
        )
        embeddings = np.asarray(embeddings, dtype=np.float32)

        if embeddings.shape != (len(texts), self.modelDimension):
            raise EmbeddingError("O modelo de embeddings retornou um formato inesperado.")
        return embeddings
