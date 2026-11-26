from dataclasses import dataclass


@dataclass(frozen=True)
class DocumentRegistration:
    documentId: int
    versionId: int
    jobId: int
    status: str
