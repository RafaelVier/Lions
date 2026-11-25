import numpy as np
import pytest

from ragservice.application.service.chunkingService import ChunkingService
from ragservice.application.service.documentSections import sectionsFromText
from ragservice.domain.port.embeddingPort import EmbeddingError
from ragservice.infrastructure.embedding.e5EmbeddingAdapter import (
    PASSAGE_PREFIX,
    QUERY_PREFIX,
    E5EmbeddingAdapter,
)
from ragservice.infrastructure.embedding.e5TokenCounter import E5TokenCounter


class FakeTokenizer:

    def __call__(self, texts: list[str], add_special_tokens: bool) -> dict:
        assert add_special_tokens is False
        return {"input_ids": [list(range(len(text.split()))) for text in texts]}


class FakeModel:

    def __init__(self, dimension: int = 4):
        self.dimension = dimension
        self.tokenizer = FakeTokenizer()
        self.calls: list[dict] = []

    def get_embedding_dimension(self) -> int:
        return self.dimension

    def encode(self, texts: list[str], **options) -> np.ndarray:
        self.calls.append({"texts": texts, **options})
        return np.ones((len(texts), self.dimension), dtype=np.float64)


def testAppliesPassagePrefixAndNormalization() -> None:
    model = FakeModel()
    adapter = E5EmbeddingAdapter(model, batchSize=8)

    embeddings = adapter.embedPassages(["Primeiro trecho", "Segundo trecho"])

    assert embeddings.shape == (2, 4)
    assert embeddings.dtype == np.float32
    assert model.calls[0]["texts"] == ["Primeiro trecho", "Segundo trecho"]
    assert model.calls[0]["prompt"] == PASSAGE_PREFIX
    assert model.calls[0]["normalize_embeddings"] is True
    assert model.calls[0]["batch_size"] == 8


def testAppliesQueryPrefix() -> None:
    model = FakeModel()

    embedding = E5EmbeddingAdapter(model, batchSize=8).embedQuery("Como pedir uma cadeira?")

    assert embedding.shape == (4,)
    assert model.calls[0]["prompt"] == QUERY_PREFIX


@pytest.mark.parametrize("texts", [[], ["texto", "  "], [""]])
def testRejectsEmptyPassages(texts: list[str]) -> None:
    with pytest.raises(EmbeddingError):
        E5EmbeddingAdapter(FakeModel(), batchSize=8).embedPassages(texts)


def testRejectsEmptyQuery() -> None:
    with pytest.raises(EmbeddingError):
        E5EmbeddingAdapter(FakeModel(), batchSize=8).embedQuery(" ")


def testRejectsUnexpectedEmbeddingShape() -> None:
    class WrongModel(FakeModel):
        def encode(self, texts: list[str], **options) -> np.ndarray:
            return np.ones((len(texts), self.dimension + 1))

    with pytest.raises(EmbeddingError, match="formato"):
        E5EmbeddingAdapter(WrongModel(), batchSize=8).embedPassages(["texto"])


@pytest.mark.parametrize("batchSize, dimension", [(0, 4), (8, 0)])
def testRejectsInvalidConfiguration(batchSize: int, dimension: int) -> None:
    with pytest.raises(EmbeddingError):
        E5EmbeddingAdapter(FakeModel(dimension), batchSize=batchSize)


def testValidatesPassageLimit() -> None:
    adapter = E5EmbeddingAdapter(FakeModel(), batchSize=8)

    assert adapter.maxPassageTokens() == 512 - 1 - 2
    adapter.ensurePassageLimit(509)
    with pytest.raises(EmbeddingError, match="limite"):
        adapter.ensurePassageLimit(510)


def testCountsTokensWithoutSpecialTokens() -> None:
    counter = E5TokenCounter(FakeTokenizer())

    assert counter.countTokens(["um dois", "três"]) == [2, 1]
    assert counter.countTokens([]) == []


@pytest.fixture(scope="module")
def realModel():
    from ragservice.infrastructure.config.settings import RagSettings
    from ragservice.infrastructure.embedding.e5Model import loadE5Model

    return loadE5Model(RagSettings())


@pytest.mark.model
def testRealModelProducesNormalizedVectors(realModel) -> None:
    adapter = E5EmbeddingAdapter(realModel, batchSize=8)

    embeddings = adapter.embedPassages(["Cadeiras de rodas são emprestadas por 90 dias."])

    assert adapter.dimension == 384
    assert embeddings.shape == (1, 384)
    assert np.linalg.norm(embeddings[0]) == pytest.approx(1.0, abs=1e-5)


@pytest.mark.model
def testRealModelRanksRelevantPassageFirst(realModel) -> None:
    adapter = E5EmbeddingAdapter(realModel, batchSize=8)
    passages = adapter.embedPassages([
        "O empréstimo de cadeiras de rodas tem prazo de devolução de 90 dias.",
        "A reunião mensal do clube acontece na primeira segunda-feira do mês.",
    ])

    query = adapter.embedQuery("Quanto tempo posso ficar com a cadeira de rodas?")
    scores = passages @ query

    assert scores[0] > scores[1]


@pytest.mark.model
def testRealTokenCounterKeepsChunksWithinModelLimit(realModel) -> None:
    adapter = E5EmbeddingAdapter(realModel, batchSize=8)
    counter = E5TokenCounter(realModel.tokenizer)
    text = " ".join(["Orientações sobre empréstimo e devolução de equipamentos ortopédicos."] * 200)

    chunks = ChunkingService(counter, maxTokens=350, overlapTokens=60).splitSections(sectionsFromText(text))

    assert len(chunks) > 1
    for chunk in chunks:
        realTokens = counter.countTokens([chunk.text])[0]
        assert realTokens <= adapter.maxPassageTokens()
        assert realTokens == pytest.approx(chunk.tokenCount, rel=0.05)
