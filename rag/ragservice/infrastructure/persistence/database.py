from sqlalchemy import create_engine
from sqlalchemy.engine import Engine, make_url
from sqlalchemy.exc import ArgumentError

from ragservice.infrastructure.config.settings import RagSettings


class DatabaseConfigurationError(ValueError):
    pass


def createDatabaseEngine(settings: RagSettings) -> Engine:
    if settings.databaseUrl is None or not settings.databaseUrl.get_secret_value().strip():
        raise DatabaseConfigurationError("Configure RAG_DATABASE_URL para acessar o banco do RAG.")

    try:
        url = make_url(settings.databaseUrl.get_secret_value())
    except (ArgumentError, ValueError):
        raise DatabaseConfigurationError("RAG_DATABASE_URL deve conter uma URL PostgreSQL válida.") from None

    if url.drivername not in {"postgresql", "postgresql+psycopg"} or not url.database:
        raise DatabaseConfigurationError("RAG_DATABASE_URL deve identificar um banco PostgreSQL.")

    return create_engine(url.set(drivername="postgresql+psycopg"), hide_parameters=True)
