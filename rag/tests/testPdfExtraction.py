from io import BytesIO

import pytest
from pypdf import PdfWriter
from pypdf.generic import DecodedStreamObject, DictionaryObject, NameObject

from ragservice.infrastructure.extraction.documentLimits import MAX_DOCUMENT_SIZE_BYTES
from ragservice.infrastructure.extraction.pdfExtraction import (
    PdfExtractionError,
    extractPdfPages,
)


def createPdf(pageTexts: list[str], password: str | None = None) -> bytes:
    with PdfWriter() as writer:
        for text in pageTexts:
            page = writer.add_blank_page(width=300, height=200)
            if text:
                font = DictionaryObject({
                    NameObject("/Type"): NameObject("/Font"),
                    NameObject("/Subtype"): NameObject("/Type1"),
                    NameObject("/BaseFont"): NameObject("/Helvetica"),
                    NameObject("/Encoding"): NameObject("/WinAnsiEncoding"),
                })
                page[NameObject("/Resources")] = DictionaryObject({
                    NameObject("/Font"): DictionaryObject({NameObject("/F1"): font}),
                })
                stream = DecodedStreamObject()
                encodedText = text.encode("cp1252").hex().encode("ascii")
                stream.set_data(b"BT /F1 12 Tf 20 100 Td <" + encodedText + b"> Tj ET")
                page.replace_contents(stream)
        if password is not None:
            writer.encrypt(password)
        output = BytesIO()
        writer.write(output)
        return output.getvalue()


def testExtractsAccentedText() -> None:
    assert extractPdfPages(createPdf(["Orientações para empréstimo"])) == [
        "Orientações para empréstimo"
    ]


def testPreservesPageOrderAndBlankPages() -> None:
    pageTexts = ["", "Primeira página com texto", "", "Segunda página com texto", ""]

    assert extractPdfPages(createPdf(pageTexts)) == pageTexts


@pytest.mark.parametrize("content", [b"", b"texto", b"%PDF-1.7\ntruncado"])
def testRejectsInvalidPdf(content: bytes) -> None:
    with pytest.raises(PdfExtractionError, match="inválido"):
        extractPdfPages(content)


@pytest.mark.parametrize("password", ["", "senha-de-teste"])
def testRejectsEncryptedPdf(password: str) -> None:
    with pytest.raises(PdfExtractionError, match="criptografado"):
        extractPdfPages(createPdf(["Texto protegido"], password=password))


@pytest.mark.parametrize("pageTexts", [[], [""], ["", "  "]])
def testRejectsPdfWithoutExtractableText(pageTexts: list[str]) -> None:
    with pytest.raises(PdfExtractionError, match="texto extraível"):
        extractPdfPages(createPdf(pageTexts))


def testRejectsImageOnlyPdf() -> None:
    with PdfWriter() as writer:
        page = writer.add_blank_page(width=300, height=200)
        stream = DecodedStreamObject()
        stream.set_data(b"q 20 0 0 20 10 10 cm BI /W 1 /H 1 /CS /RGB /BPC 8 ID \xff\xff\xff EI Q")
        page.replace_contents(stream)
        output = BytesIO()
        writer.write(output)

    with pytest.raises(PdfExtractionError, match="texto extraível"):
        extractPdfPages(output.getvalue())


def testAcceptsPdfAtSizeLimit() -> None:
    content = createPdf(["Texto"]).ljust(MAX_DOCUMENT_SIZE_BYTES, b" ")

    assert extractPdfPages(content) == ["Texto"]


def testRejectsPdfAboveSizeLimit() -> None:
    content = createPdf(["Texto"]).ljust(MAX_DOCUMENT_SIZE_BYTES + 1, b" ")

    with pytest.raises(PdfExtractionError, match="5 MiB"):
        extractPdfPages(content)
