from io import BytesIO

from pypdf import PdfReader
from pypdf.errors import DependencyError, PyPdfError

from ragservice.infrastructure.extraction.documentLimits import MAX_DOCUMENT_SIZE_BYTES


class PdfExtractionError(ValueError):
    pass


def extractPdfPages(content: bytes) -> list[str]:
    if len(content) > MAX_DOCUMENT_SIZE_BYTES:
        raise PdfExtractionError("O arquivo excede o limite de 5 MiB.")
    if not content.startswith(b"%PDF-"):
        raise PdfExtractionError("O arquivo PDF é inválido.")

    try:
        with PdfReader(BytesIO(content), strict=True) as reader:
            if reader.is_encrypted:
                raise PdfExtractionError("O arquivo PDF está criptografado.")
            pageTexts = [page.extract_text() or "" for page in reader.pages]
    except PdfExtractionError:
        raise
    except DependencyError as exc:
        raise PdfExtractionError("O arquivo PDF está criptografado.") from exc
    except (PyPdfError, ValueError, KeyError) as exc:
        raise PdfExtractionError("O arquivo PDF é inválido.") from exc

    if not any(text.strip() for text in pageTexts):
        raise PdfExtractionError("O arquivo PDF não contém texto extraível.")

    return pageTexts
