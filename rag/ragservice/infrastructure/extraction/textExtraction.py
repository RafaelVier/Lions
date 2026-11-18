from pathlib import PurePath

from ragservice.infrastructure.extraction.documentLimits import MAX_DOCUMENT_SIZE_BYTES

SUPPORTED_EXTENSIONS = {".txt", ".md"}


class TextExtractionError(ValueError):
    pass


def extractText(fileName: str, content: bytes) -> str:
    if PurePath(fileName).suffix.lower() not in SUPPORTED_EXTENSIONS:
        raise TextExtractionError("Formato não suportado: use TXT ou Markdown.")

    if len(content) > MAX_DOCUMENT_SIZE_BYTES:
        raise TextExtractionError("O arquivo excede o limite de 5 MiB.")

    try:
        text = content.decode("utf-8-sig")
    except UnicodeDecodeError as exc:
        raise TextExtractionError("O arquivo deve estar codificado em UTF-8.") from exc

    if not text.strip():
        raise TextExtractionError("O arquivo não pode estar vazio.")
    if "\x00" in text:
        raise TextExtractionError("O arquivo contém caracteres nulos.")

    return text
