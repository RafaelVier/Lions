import pytest

from ragservice.application.service.chunkingService import ChunkingError, ChunkingService
from ragservice.application.service.documentSections import sectionsFromPages, sectionsFromText
from ragservice.domain.entity.documentSection import DocumentSection
from ragservice.domain.port.tokenCounterPort import TokenCounterPort


class WordTokenCounter(TokenCounterPort):

    def __init__(self, longWordTokens: int = 1):
        self.longWordTokens = longWordTokens
        self.calls = 0

    def countTokens(self, texts: list[str]) -> list[int]:
        self.calls += 1
        return [self.longWordTokens if len(text) > 30 else 1 for text in texts]


def createService(maxTokens: int, overlapTokens: int = 0, counter: TokenCounterPort | None = None) -> ChunkingService:
    return ChunkingService(counter or WordTokenCounter(), maxTokens, overlapTokens)


def numberedWords(count: int) -> str:
    return " ".join(f"palavra{index}" for index in range(count))


def testKeepsShortTextInSingleChunk() -> None:
    text = "Orientações para empréstimo de equipamentos."

    chunks = createService(maxTokens=10).splitSections(sectionsFromText(text))

    assert len(chunks) == 1
    assert chunks[0].text == text
    assert chunks[0].chunkIndex == 0
    assert chunks[0].pageNumber is None
    assert chunks[0].tokenCount == 5


def testRespectsMaximumTokens() -> None:
    chunks = createService(maxTokens=4).splitSections(sectionsFromText(numberedWords(10)))

    assert [chunk.tokenCount for chunk in chunks] == [4, 4, 2]
    assert all(len(chunk.text.split()) <= 4 for chunk in chunks)


def testRepeatsOverlapBetweenChunks() -> None:
    chunks = createService(maxTokens=4, overlapTokens=2).splitSections(sectionsFromText(numberedWords(8)))

    assert [chunk.text.split() for chunk in chunks] == [
        ["palavra0", "palavra1", "palavra2", "palavra3"],
        ["palavra2", "palavra3", "palavra4", "palavra5"],
        ["palavra4", "palavra5", "palavra6", "palavra7"],
    ]


def testPrefersSentenceEnding() -> None:
    text = "Um dois três quatro. Cinco seis sete oito nove dez"

    chunks = createService(maxTokens=6).splitSections(sectionsFromText(text))

    assert chunks[0].text == "Um dois três quatro."
    assert chunks[1].text == "Cinco seis sete oito nove dez"


def testPrefersParagraphBreak() -> None:
    text = "Um dois três quatro\n\nCinco seis sete oito nove dez"

    chunks = createService(maxTokens=6).splitSections(sectionsFromText(text))

    assert chunks[0].text == "Um dois três quatro"


def testIgnoresSentenceEndingFarFromLimit() -> None:
    text = "Um. dois três quatro cinco seis sete oito nove dez"

    chunks = createService(maxTokens=6).splitSections(sectionsFromText(text))

    assert chunks[0].text == "Um. dois três quatro cinco seis"


def testOffsetsReferenceOriginalText() -> None:
    text = "  Primeira linha.\nSegunda   linha com   espaços.\n\nTerceira linha final.  "

    chunks = createService(maxTokens=3, overlapTokens=1).splitSections(sectionsFromText(text))

    for chunk in chunks:
        assert text[chunk.startOffset:chunk.endOffset] == chunk.text
    assert chunks[0].startOffset == 2
    assert chunks[-1].endOffset == len(text.rstrip())


def testKeepsPagesSeparatedAndSkipsBlankPages() -> None:
    sections = sectionsFromPages(["Texto da primeira página", "", "   ", "Texto da quarta"])

    chunks = createService(maxTokens=10).splitSections(sections)

    assert [(chunk.pageNumber, chunk.text) for chunk in chunks] == [
        (1, "Texto da primeira página"),
        (4, "Texto da quarta"),
    ]
    assert [chunk.chunkIndex for chunk in chunks] == [0, 1]


def testKeepsLongWordInOwnChunk() -> None:
    longWord = "x" * 40
    counter = WordTokenCounter(longWordTokens=10)

    chunks = createService(maxTokens=4, overlapTokens=1, counter=counter).splitSections(
        sectionsFromText(f"antes {longWord} depois")
    )

    assert [chunk.text for chunk in chunks] == ["antes", longWord, "depois"]
    assert chunks[1].tokenCount == 10


def testCountsTokensOncePerSection() -> None:
    counter = WordTokenCounter()

    createService(maxTokens=3, overlapTokens=1, counter=counter).splitSections(
        [DocumentSection(numberedWords(20), 1), DocumentSection(numberedWords(20), 2)]
    )

    assert counter.calls == 2


def testReturnsNoChunksForEmptyText() -> None:
    service = createService(maxTokens=5)

    assert service.splitSections(sectionsFromText("  \n ")) == []
    assert service.splitSections([DocumentSection("   ")]) == []


@pytest.mark.parametrize("maxTokens, overlapTokens", [(0, 0), (5, -1), (5, 5), (5, 6)])
def testRejectsInvalidSettings(maxTokens: int, overlapTokens: int) -> None:
    with pytest.raises(ChunkingError):
        createService(maxTokens, overlapTokens)


def testRejectsInconsistentTokenCounter() -> None:
    class BrokenCounter(TokenCounterPort):
        def countTokens(self, texts: list[str]) -> list[int]:
            return []

    with pytest.raises(ChunkingError, match="contador"):
        createService(maxTokens=5, counter=BrokenCounter()).splitSections(sectionsFromText("um dois"))
