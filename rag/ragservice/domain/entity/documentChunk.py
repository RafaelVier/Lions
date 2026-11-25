from dataclasses import dataclass


@dataclass(frozen=True)
class DocumentChunk:
    text: str
    chunkIndex: int
    pageNumber: int | None
    startOffset: int
    endOffset: int
    tokenCount: int
