from dataclasses import dataclass


@dataclass(frozen=True)
class DocumentSection:
    text: str
    pageNumber: int | None = None
