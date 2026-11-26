from alembic import context

from ragservice.infrastructure.config.settings import RagSettings
from ragservice.infrastructure.persistence.database import createDatabaseEngine


def runOfflineMigrations() -> None:
    context.configure(dialect_name="postgresql", literal_binds=True)
    with context.begin_transaction():
        context.run_migrations()


def runOnlineMigrations() -> None:
    engine = createDatabaseEngine(RagSettings())
    try:
        with engine.connect() as connection:
            context.configure(connection=connection)
            with context.begin_transaction():
                context.run_migrations()
    finally:
        engine.dispose()


if context.is_offline_mode():
    runOfflineMigrations()
else:
    runOnlineMigrations()
