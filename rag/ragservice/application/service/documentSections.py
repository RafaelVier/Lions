from ragservice.domain.entity.documentSection import DocumentSection


def sectionsFromText(text: str) -> list[DocumentSection]:
    if not text.strip():
        return []
    return [DocumentSection(text)]


def sectionsFromPages(pageTexts: list[str]) -> list[DocumentSection]:
    return [
        DocumentSection(text, pageNumber)
        for pageNumber, text in enumerate(pageTexts, start=1)
        if text.strip()
    ]
