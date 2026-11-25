import re
from dataclasses import dataclass

from ragservice.domain.entity.documentChunk import DocumentChunk
from ragservice.domain.entity.documentSection import DocumentSection
from ragservice.domain.port.tokenCounterPort import TokenCounterPort

WORD_PATTERN = re.compile(r"\S+")
BLANK_LINE_PATTERN = re.compile(r"\n[^\S\n]*\n")
SENTENCE_ENDINGS = (".", "!", "?", ";", ":")
BOUNDARY_SEARCH_RATIO = 0.4


class ChunkingError(ValueError):
    pass


@dataclass(frozen=True)
class TextWord:
    start: int
    end: int
    tokenCount: int
    endsBoundary: bool


class ChunkingService:

    def __init__(self, tokenCounterPort: TokenCounterPort, maxTokens: int, overlapTokens: int):
        if maxTokens <= 0:
            raise ChunkingError("O tamanho máximo do trecho deve ser positivo.")
        if overlapTokens < 0 or overlapTokens >= maxTokens:
            raise ChunkingError("A sobreposição deve ser menor que o tamanho máximo do trecho.")

        self.tokenCounterPort = tokenCounterPort
        self.maxTokens = maxTokens
        self.overlapTokens = overlapTokens

    def splitSections(self, sections: list[DocumentSection]) -> list[DocumentChunk]:
        chunks: list[DocumentChunk] = []
        for section in sections:
            for start, end, tokenCount in self.splitText(section.text):
                chunks.append(DocumentChunk(
                    text=section.text[start:end],
                    chunkIndex=len(chunks),
                    pageNumber=section.pageNumber,
                    startOffset=start,
                    endOffset=end,
                    tokenCount=tokenCount,
                ))
        return chunks

    def splitText(self, text: str) -> list[tuple[int, int, int]]:
        words = self.findWords(text)
        ranges: list[tuple[int, int, int]] = []
        first = 0

        while first < len(words):
            last = self.findLastWord(words, first)
            tokenCount = sum(word.tokenCount for word in words[first:last + 1])
            ranges.append((words[first].start, words[last].end, tokenCount))
            if last == len(words) - 1:
                break
            first = self.findNextFirstWord(words, first, last)

        return ranges

    def findWords(self, text: str) -> list[TextWord]:
        matches = list(WORD_PATTERN.finditer(text))
        if not matches:
            return []

        tokenCounts = self.tokenCounterPort.countTokens([match.group() for match in matches])
        if len(tokenCounts) != len(matches):
            raise ChunkingError("O contador de tokens retornou uma quantidade inválida.")

        words = []
        for index, (match, tokenCount) in enumerate(zip(matches, tokenCounts)):
            nextStart = matches[index + 1].start() if index + 1 < len(matches) else len(text)
            endsBoundary = (
                match.group().endswith(SENTENCE_ENDINGS)
                or BLANK_LINE_PATTERN.search(text, match.end(), nextStart) is not None
            )
            words.append(TextWord(match.start(), match.end(), tokenCount, endsBoundary))
        return words

    def findLastWord(self, words: list[TextWord], first: int) -> int:
        total = 0
        last = first
        for index in range(first, len(words)):
            if index > first and total + words[index].tokenCount > self.maxTokens:
                break
            total += words[index].tokenCount
            last = index
        else:
            return last

        # Prefere encerrar o trecho em fim de frase ou parágrafo próximo ao limite
        minimumTokens = self.maxTokens * (1 - BOUNDARY_SEARCH_RATIO)
        running = total
        for index in range(last, first, -1):
            if running < minimumTokens:
                break
            if words[index].endsBoundary:
                return index
            running -= words[index].tokenCount
        return last

    def findNextFirstWord(self, words: list[TextWord], first: int, last: int) -> int:
        nextFirst = last + 1
        overlap = 0
        while nextFirst - 1 > first and overlap + words[nextFirst - 1].tokenCount <= self.overlapTokens:
            nextFirst -= 1
            overlap += words[nextFirst].tokenCount
        return nextFirst
