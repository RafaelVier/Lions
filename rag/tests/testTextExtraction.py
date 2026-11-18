import pytest

from ragservice.infrastructure.extraction.textExtraction import (
    MAX_DOCUMENT_SIZE_BYTES,
    TextExtractionError,
    extractText,
)


@pytest.mark.parametrize("fileName", ["documento.txt", "regras.MD", "manual.TxT"])
def testExtractsUtf8TextWithoutChangingContent(fileName: str) -> None:
    text = "  Orientações para empréstimo\n\n"

    assert extractText(fileName, text.encode("utf-8")) == text


def testExtractsUtf8WithBom() -> None:
    assert extractText("documento.txt", b"\xef\xbb\xbfOl\xc3\xa1") == "Olá"


@pytest.mark.parametrize("fileName", ["documento.pdf", "documento", "documento.txt.pdf"])
def testRejectsUnsupportedFormat(fileName: str) -> None:
    with pytest.raises(TextExtractionError, match="Formato não suportado"):
        extractText(fileName, b"conteudo")


@pytest.mark.parametrize("content", [b"", b" \n\t", b"\xef\xbb\xbf"])
def testRejectsEmptyContent(content: bytes) -> None:
    with pytest.raises(TextExtractionError, match="vazio"):
        extractText("documento.txt", content)


def testRejectsInvalidUtf8() -> None:
    with pytest.raises(TextExtractionError, match="UTF-8"):
        extractText("documento.md", b"\xff")


def testRejectsNullCharacters() -> None:
    with pytest.raises(TextExtractionError, match="nulos"):
        extractText("documento.txt", b"texto\x00binario")


def testAcceptsDocumentAtSizeLimit() -> None:
    content = b"a" * MAX_DOCUMENT_SIZE_BYTES

    assert extractText("documento.txt", content) == "a" * MAX_DOCUMENT_SIZE_BYTES


def testRejectsDocumentAboveSizeLimit() -> None:
    content = b"a" * (MAX_DOCUMENT_SIZE_BYTES + 1)

    with pytest.raises(TextExtractionError, match="5 MiB"):
        extractText("documento.txt", content)
