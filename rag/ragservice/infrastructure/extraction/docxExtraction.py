from io import BytesIO
from zipfile import BadZipFile, ZipFile

from docx import Document
from docx.opc.exceptions import PackageNotFoundError
from docx.oxml.ns import qn
from docx.text.paragraph import Paragraph

from ragservice.infrastructure.extraction.documentLimits import (
    MAX_DOCUMENT_SIZE_BYTES,
    MAX_UNCOMPRESSED_DOCUMENT_SIZE_BYTES,
)

ZIP_SIGNATURE = b"PK\x03\x04"
OLE_SIGNATURE = b"\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1"
FALLBACK_TAG = "{http://schemas.openxmlformats.org/markup-compatibility/2006}Fallback"


class DocxExtractionError(ValueError):
    pass


def extractDocxText(content: bytes) -> str:
    if len(content) > MAX_DOCUMENT_SIZE_BYTES:
        raise DocxExtractionError("O arquivo excede o limite de 5 MiB.")
    if content.startswith(OLE_SIGNATURE):
        raise DocxExtractionError("O arquivo DOCX está criptografado ou em formato antigo.")
    if not content.startswith(ZIP_SIGNATURE):
        raise DocxExtractionError("O arquivo DOCX é inválido.")

    try:
        validateUncompressedSize(content)
        document = Document(BytesIO(content))
        paragraphTexts = [
            Paragraph(paragraph, document).text
            for paragraph in document.element.body.iter(qn("w:p"))
            if not isInsideFallback(paragraph)
        ]
    except DocxExtractionError:
        raise
    except (BadZipFile, PackageNotFoundError, KeyError, ValueError) as exc:
        raise DocxExtractionError("O arquivo DOCX é inválido.") from exc

    text = "\n".join(paragraphTexts)
    if not text.strip():
        raise DocxExtractionError("O arquivo DOCX não contém texto.")

    return text


def validateUncompressedSize(content: bytes) -> None:
    with ZipFile(BytesIO(content)) as archive:
        uncompressedSize = sum(entry.file_size for entry in archive.infolist())

    if uncompressedSize > MAX_UNCOMPRESSED_DOCUMENT_SIZE_BYTES:
        raise DocxExtractionError("O conteúdo descompactado do arquivo DOCX excede o limite.")


# Conteúdo alternativo de compatibilidade repete o texto de caixas de texto
def isInsideFallback(paragraph) -> bool:
    return any(ancestor.tag == FALLBACK_TAG for ancestor in paragraph.iterancestors())
