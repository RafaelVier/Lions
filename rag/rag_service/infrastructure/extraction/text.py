from pathlib import PurePath

MAX_DOCUMENT_SIZE_BYTES = 5 * 1024 * 1024
SUPPORTED_EXTENSIONS = {".txt", ".md"}


class TextExtractionError(ValueError):
    """Indica que um arquivo não pode ser processado como texto."""


def extract_text(filename: str, content: bytes) -> str:
    if PurePath(filename).suffix.lower() not in SUPPORTED_EXTENSIONS:
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
