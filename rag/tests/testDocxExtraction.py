from io import BytesIO
from zipfile import ZIP_DEFLATED, ZipFile

import pytest
from docx import Document

from ragservice.infrastructure.extraction.docxExtraction import (
    DocxExtractionError,
    extractDocxText,
)
from ragservice.infrastructure.extraction.documentLimits import (
    MAX_DOCUMENT_SIZE_BYTES,
    MAX_UNCOMPRESSED_DOCUMENT_SIZE_BYTES,
)


def saveDocument(document) -> bytes:
    output = BytesIO()
    document.save(output)
    return output.getvalue()


def createDocx(paragraphs: list[str]) -> bytes:
    document = Document()
    for text in paragraphs:
        document.add_paragraph(text)
    return saveDocument(document)


def testExtractsAccentedParagraphs() -> None:
    content = createDocx(["Orientações para empréstimo", "Devolução em até 30 dias"])

    assert extractDocxText(content) == "Orientações para empréstimo\nDevolução em até 30 dias"


def testExtractsTablesInDocumentOrder() -> None:
    document = Document()
    document.add_paragraph("Antes da tabela")
    table = document.add_table(rows=1, cols=2)
    table.cell(0, 0).text = "Cadeira de rodas"
    table.cell(0, 1).text = "Disponível"
    document.add_paragraph("Depois da tabela")

    assert extractDocxText(saveDocument(document)) == (
        "Antes da tabela\nCadeira de rodas\nDisponível\nDepois da tabela"
    )


def testPreservesTabsAndLineBreaks() -> None:
    document = Document()
    paragraph = document.add_paragraph()
    run = paragraph.add_run("Item\tQuantidade")
    run.add_break()
    run.add_text("Muleta\t2")

    assert extractDocxText(saveDocument(document)) == "Item\tQuantidade\nMuleta\t2"


@pytest.mark.parametrize("paragraphs", [[], ["", "   "]])
def testRejectsDocxWithoutText(paragraphs: list[str]) -> None:
    with pytest.raises(DocxExtractionError, match="não contém texto"):
        extractDocxText(createDocx(paragraphs))


@pytest.mark.parametrize("content", [b"", b"texto", b"PK\x03\x04truncado"])
def testRejectsInvalidDocx(content: bytes) -> None:
    with pytest.raises(DocxExtractionError, match="inválido"):
        extractDocxText(content)


def testRejectsZipWithoutWordDocument() -> None:
    output = BytesIO()
    with ZipFile(output, "w") as archive:
        archive.writestr("leia-me.txt", "Arquivo comum")

    with pytest.raises(DocxExtractionError, match="inválido"):
        extractDocxText(output.getvalue())


def testRejectsEncryptedOrLegacyDocument() -> None:
    content = b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1" + b"\x00" * 512

    with pytest.raises(DocxExtractionError, match="criptografado"):
        extractDocxText(content)


def testRejectsDocxAboveSizeLimit() -> None:
    content = b"PK\x03\x04" + b"\x00" * MAX_DOCUMENT_SIZE_BYTES

    with pytest.raises(DocxExtractionError, match="5 MiB"):
        extractDocxText(content)


def testRejectsDocxAboveUncompressedLimit() -> None:
    source = BytesIO(createDocx(["Texto"]))
    output = BytesIO()
    with ZipFile(source) as original, ZipFile(output, "w", ZIP_DEFLATED) as archive:
        for entry in original.infolist():
            archive.writestr(entry, original.read(entry))
        archive.writestr("word/media/extra.bin", b"\x00" * (MAX_UNCOMPRESSED_DOCUMENT_SIZE_BYTES + 1))

    with pytest.raises(DocxExtractionError, match="descompactado"):
        extractDocxText(output.getvalue())
