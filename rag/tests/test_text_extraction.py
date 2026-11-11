import pytest

from rag_service.infrastructure.extraction.text import (
    MAX_DOCUMENT_SIZE_BYTES,
    TextExtractionError,
    extract_text,
)


@pytest.mark.parametrize("filename", ["documento.txt", "regras.MD", "manual.TxT"])
def test_extracts_utf8_text_without_changing_content(filename: str) -> None:
    text = "  Orientações para empréstimo\n\n"

    assert extract_text(filename, text.encode("utf-8")) == text


def test_extracts_utf8_with_bom() -> None:
    assert extract_text("documento.txt", b"\xef\xbb\xbfOl\xc3\xa1") == "Olá"


@pytest.mark.parametrize("filename", ["documento.pdf", "documento", "documento.txt.pdf"])
def test_rejects_unsupported_format(filename: str) -> None:
    with pytest.raises(TextExtractionError, match="Formato não suportado"):
        extract_text(filename, b"conteudo")


@pytest.mark.parametrize("content", [b"", b" \n\t", b"\xef\xbb\xbf"])
def test_rejects_empty_content(content: bytes) -> None:
    with pytest.raises(TextExtractionError, match="vazio"):
        extract_text("documento.txt", content)


def test_rejects_invalid_utf8() -> None:
    with pytest.raises(TextExtractionError, match="UTF-8"):
        extract_text("documento.md", b"\xff")


def test_rejects_null_characters() -> None:
    with pytest.raises(TextExtractionError, match="nulos"):
        extract_text("documento.txt", b"texto\x00binario")


def test_accepts_document_at_size_limit() -> None:
    content = b"a" * MAX_DOCUMENT_SIZE_BYTES

    assert extract_text("documento.txt", content) == "a" * MAX_DOCUMENT_SIZE_BYTES


def test_rejects_document_above_size_limit() -> None:
    content = b"a" * (MAX_DOCUMENT_SIZE_BYTES + 1)

    with pytest.raises(TextExtractionError, match="5 MiB"):
        extract_text("documento.txt", content)
